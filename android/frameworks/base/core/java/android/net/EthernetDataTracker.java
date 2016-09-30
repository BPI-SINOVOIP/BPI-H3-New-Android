/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net;

import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.provider.Settings;
import android.net.NetworkInfo.DetailedState;
import android.net.ethernet.EthernetDevInfo;
import android.net.ethernet.EthernetManager;
import android.net.LinkAddress;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.net.BaseNetworkObserver;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Class;
import java.lang.reflect.Constructor;
import java.lang.Exception;
import android.net.PppoeStateMachine;

/**
 * This class tracks the data connection associated with Ethernet
 * This is a singleton class and an instance will be created by
 * ConnectivityService.
 * @hide
 */
public class EthernetDataTracker extends BaseNetworkStateTracker {
    private static final String NETWORKTYPE = "ETHERNET";
    private static final String TAG         = "EthernetDataTracker";

    private AtomicBoolean mTeardownRequested    = new AtomicBoolean(false);
    private AtomicBoolean mPrivateDnsRouteSet   = new AtomicBoolean(false);
    private AtomicInteger mDefaultGatewayAddr   = new AtomicInteger(0);
    private AtomicBoolean mDefaultRouteSet      = new AtomicBoolean(false);

    private static boolean mLinkUp          = false;
    private static boolean mLinkIn          = false;
    private static boolean mReadyToConnect  = false;
    private static String sIfaceMatch       = "";
    private static String mIface            = "";
    private final String SYS_NET            = "/sys/class/net/";
    private String mHwaddr                  = "";
    private int prefixLength                = 0;

    private InterfaceObserver mInterfaceObserver;
    private INetworkManagementService mNMService;
    private EthernetManager mEthManager;
    /* For sending events to connectivity service handler */
    private Handler mCsHandler;
    private static EthernetDataTracker mInstance    = null;
    private static DhcpResults mDhcpResults;
    private Handler mPppoeHandler;
    private PppoeStateMachine mPppoeSM;

    private static final String ETHERNET_CONNECT_MODE_DHCP
                    = EthernetManager.ETHERNET_CONNECT_MODE_DHCP;
    private static final String ETHERNET_CONNECT_MODE_MANUAL
                    = EthernetManager.ETHERNET_CONNECT_MODE_MANUAL;
    private static final String ETHERNET_CONNECT_MODE_PPPOE
                    = EthernetManager.ETHERNET_CONNECT_MODE_PPPOE;

    private class InterfaceObserver extends BaseNetworkObserver {
        private EthernetDataTracker mTracker;

        InterfaceObserver(EthernetDataTracker tracker) {
            super();
            mTracker = tracker;
        }

        @Override
        public void interfaceStatusChanged(String iface, boolean up) {
            Log.d(TAG, "Interface status changed: " + iface + (up ? "up" : "down"));
        }

        @Override
        public void interfaceLinkStateChanged(String iface, boolean in) {
            if (mIface.equals(iface)) {
                Log.d(TAG, "Interface " + iface + " link " + (in ? "in" : "out"));
                mLinkIn = in;
                mTracker.mNetworkInfo.setIsAvailable(in);

                // 1. Send Linkin/out broadcast
                // 2. in: reconnect     out: disconnect
                if (in) {
                    sendEthStateBroadcast(EthernetManager.EVENT_PHY_LINK_IN);
                    mTracker.reconnect();
                } else {
                    sendEthStateBroadcast(EthernetManager.EVENT_PHY_LINK_OUT);
                    mTracker.disconnect();
                }
            }
        }

        @Override
        public void interfaceAdded(String iface) {
            mTracker.interfaceAdded(iface);
        }

        @Override
        public void interfaceRemoved(String iface) {
            mTracker.interfaceRemoved(iface);
        }
    }

    private void interfaceAdded(String iface) {
        Log.d(TAG,"interfaceAdded " + iface);
        if(!mEthManager.addInterfaceToService(iface)) {
            Log.w(TAG, "add iface[" + iface + "] to ethernet list failed.");
            return;
        }

        if (!iface.matches(sIfaceMatch)) {
            Log.w(TAG, "iface[" + iface + "] not match!" );
            return;
        }

        Log.d(TAG, "Adding " + iface);

        // Broadcast ethernet interface added success
        sendEthStateBroadcast(EthernetManager.EVENT_INTERFACE_ADDED);

        // Just select the first added interface as active interface
        synchronized(this) {
            if(!mIface.isEmpty())
                return;
            Log.d(TAG, "update mIface[" + iface + "]");
            mIface = iface;
        }

        // Update current active interface success
        // 1. Set active interface up
        // 2. Call reconnect to try connect(link up will call allways?)
        // 3. Send broadcast to ConnectivityService
        try {
            mNMService.setInterfaceUp(iface);
        } catch (Exception e) {
            Log.e(TAG, "Error upping interface " + iface + ": " + e);
        }
        reconnect();
        mNetworkInfo.setIsAvailable(true);
        Message msg = mCsHandler.obtainMessage(EVENT_CONFIGURATION_CHANGED, mNetworkInfo);
        msg.sendToTarget();
    }

    private void interfaceRemoved(String iface) {
        Log.d(TAG, "interface removed[" + iface + "]");
        // Remove the inteface information from EthernetService.mDeviceMap
        mEthManager.removeInterfaceFromService(iface);
        if (!iface.equals(mIface)) {
            Log.w(TAG, "removed inteface[" + iface + "] not match!");
            return;
        }

        // Current active interface removed(rmmod)
        // 1. Broadcast interface removed
        // 2. Call disconnect to clear info
        // 3. Reset mIface
        sendEthStateBroadcast(EthernetManager.EVENT_INTERFACE_REMOVED);
        Log.d(TAG, "Removing " + iface);
        disconnect();
        mIface = "";
    }

    private EthernetDataTracker() {
        mNetworkInfo = new NetworkInfo(ConnectivityManager.TYPE_ETHERNET, 0, NETWORKTYPE, "");
        mLinkProperties = new LinkProperties();
        mLinkCapabilities = new LinkCapabilities();
        mDhcpResults = new DhcpResults();
    }

    public static synchronized EthernetDataTracker getInstance() {
        if (mInstance == null) mInstance = new EthernetDataTracker();
        return mInstance;
    }

    private void sendNetStateBroadcast(int event) {
        Intent intent = new Intent(EthernetManager.NETWORK_STATE_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                | Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(EthernetManager.EXTRA_NETWORK_INFO, mNetworkInfo);
        intent.putExtra(EthernetManager.EXTRA_LINK_PROPERTIES,
                new LinkProperties (mLinkProperties));
        intent.putExtra(EthernetManager.EXTRA_ETHERNET_STATE, event);
        mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendEthStateBroadcast(int event) {
        Intent intent = new Intent(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                | Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(EthernetManager.EXTRA_ETHERNET_STATE, event);
        mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void connect() {
        if(!mReadyToConnect) {
            return;
        }
        Log.d(TAG, "mLinkIn is " + mLinkIn + ", Enable is "
                + mEthManager.getEthernetState() + ", mIface " + mIface);

        if (mEthManager.getEthernetState() != EthernetManager.ETHERNET_STATE_ENABLED) {
            Log.d(TAG, "Current Ethernet is disabled!");
            return;
        }

        if(!mLinkIn) {
            Log.d(TAG, "Current inteface link out!");
            return;
        } else {
            Log.d(TAG, "Current interface link in!");
        }

        /* Stop dhcp */
        if(SystemProperties.get("dhcp." + mIface + ".result").equals("ok")) {
            NetworkUtils.stopDhcp(mIface);
        }

        /* Stop pppoe */
        if(getPppoeStatus() != EthernetManager.PPPOE_STATE_STOPPED) {
            stopPppoe();
        }

        /* DHCP Mode */
        if(mEthManager.getEthernetMode().equals(ETHERNET_CONNECT_MODE_DHCP)) {
            /* make sure iface to 0.0.0.0 */
            try{
                mNMService.clearInterfaceAddresses(mIface);
                NetworkUtils.resetConnections(mIface, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "ERROR: " + e);
            }
            Log.d(TAG, "DHCP Mode: connecting and running dhcp.");
            runDhcp();
        } else if (mEthManager.getEthernetMode().equals(ETHERNET_CONNECT_MODE_MANUAL)){
            /* Static Mode */
            /* read configuration from usr setting */
            EthernetDevInfo ifaceInfo = mEthManager.getStaticConfig();
            if(ifaceInfo == null) {
                Log.e(TAG, "get configuration failed.");
                mDhcpResults.clear();
                sendNetStateBroadcast(EthernetManager.EVENT_ETHERNET_CONNECT_FAILED);
                return;
            }
            mDhcpResults = getIpConfigure(ifaceInfo);
            mLinkProperties = mDhcpResults.linkProperties;
            mLinkProperties.setInterfaceName(mIface);

            InterfaceConfiguration ifcg = new InterfaceConfiguration();
            InetAddress addr = NetworkUtils.numericToInetAddress(ifaceInfo.getIpAddress());
            LinkAddress linkAddress = new LinkAddress(NetworkUtils.numericToInetAddress(ifaceInfo.getIpAddress()),prefixLength);
            ifcg.setLinkAddress(linkAddress);
            ifcg.setInterfaceUp();

            try{
                mNMService.setInterfaceConfig(mIface, ifcg);
            } catch (Exception e) {
                Log.e(TAG, "ERROR: " + e);
                sendNetStateBroadcast(EthernetManager.EVENT_ETHERNET_CONNECT_FAILED);
                return;
            }
            Log.d(TAG, "Manual Mode: connecting and confgure static ip address.");
            // Static configure success
            // 1. Update mNetworkInfo/mLinkProperties
            // 2. Broadcast network changed to ConnectivitService
            // 3. Broadcast network changed to Ethernet Setting
            mNetworkInfo.setIsAvailable(true);
            mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
            Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
            msg.sendToTarget();
            sendNetStateBroadcast(EthernetManager.EVENT_ETHERNET_CONNECT_SUCCESSED);
        } else if (mEthManager.getEthernetMode().equals(ETHERNET_CONNECT_MODE_PPPOE)) {
            // Trigger PPPoE connect process
            Log.d(TAG, "PPPoE Mode: connecting and start pppoe");
            /* make sure iface to 0.0.0.0 */
            try{
                mNMService.clearInterfaceAddresses(mIface);
                NetworkUtils.resetConnections(mIface, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "ERROR: " + e);
            }
            startPppoe();
        }
    }


    public void disconnect() {
        if(!mReadyToConnect) {
            return;
        }
        Log.d(TAG, "disconnect");
        String mode = mEthManager.getEthernetMode();

        if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)) {
            /* Stop dhcp */
            if(SystemProperties.get("dhcp." + mIface + ".result").equals("ok")) {
                NetworkUtils.stopDhcp(mIface);
                sendNetStateBroadcast(EthernetManager.EVENT_ETHERNET_DISCONNECT_SUCCESSED);
            }
        } else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)) {
            /* Stop pppoe */
            if(getPppoeStatus() != EthernetManager.PPPOE_STATE_STOPPED) {
                stopPppoe();
                sendNetStateBroadcast(EthernetManager.EVENT_PPPOE_DISCONNECT_SUCCESSED);
            }
        } else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
            /* send broadcast always */
            sendNetStateBroadcast(EthernetManager.EVENT_ETHERNET_DISCONNECT_SUCCESSED);
        }

        // Update mLinkProperties/mNetworkInfo/mDhcpResults
        mLinkProperties.clear();
        mNetworkInfo.setIsAvailable(false);
        mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, mHwaddr);
        mDhcpResults.clear();

        // Send broadcast to ConnectivityService
        Message msg = mCsHandler.obtainMessage(EVENT_CONFIGURATION_CHANGED, mNetworkInfo);
        msg.sendToTarget();
        msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
        msg.sendToTarget();

        // Clear interface network configuration(ip/netmask)
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
        try {
            service.clearInterfaceAddresses(mIface);
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear addresses or disable ipv6" + e);
        }
    }

    private void runDhcp() {
        Thread dhcpThread = new Thread(new Runnable() {
            public void run() {
                Log.d(TAG, "Start DHCP thread!");
                mDhcpResults.clear();
                if (!NetworkUtils.runDhcp(mIface, mDhcpResults)) {
                    Log.e(TAG, "DHCP request error:" + NetworkUtils.getDhcpError());
                    sendNetStateBroadcast(EthernetManager.EVENT_ETHERNET_CONNECT_FAILED);
                    return;
                }

                Log.d(TAG,"DHCP Success\ndhcpResults = " + mDhcpResults.toString());
                // DHCP success
                // 1. Update mDhcpResults/mLinkProperties/mNetworkInfo
                // 2. Broadcast network state changed to ConnectivityService
                // 3. Broadcast network state changed to Ethernet Setting
                mLinkProperties = mDhcpResults.linkProperties;
                mNetworkInfo.setIsAvailable(true);
                mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, mHwaddr);
                Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
                msg.sendToTarget();
                sendNetStateBroadcast(EthernetManager.EVENT_ETHERNET_CONNECT_SUCCESSED);
            }
        });
        dhcpThread.start();
    }

    private void startPppoe() {
        mPppoeSM.sendMessage(PppoeStateMachine.CMD_START_PPPOE,
                mEthManager.getLoginInfo(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE).getUsername());
    }

    private void stopPppoe() {
        mPppoeSM.sendMessage(PppoeStateMachine.CMD_STOP_PPPOE);
        for(int i = 0; i < 100; i++) {
            //Log.d(TAG, "disconnect pppoe wating, sleep 100ms");
            if (getPppoeStatus() == EthernetManager.PPPOE_STATE_STOPPED) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                Log.e(TAG, "Thread error!");
            }
        }
        Log.e(TAG, "disconnect pppoe timeout!!!");
    }

    /**
     * Disable connectivity to a network
     * TODO: do away with return value after making MobileDataStateTracker async
     */
    public boolean teardown() {
        mTeardownRequested.set(true);
        disconnect();
        return true;
    }

    /**
     * Re-enable connectivity to a network after a {@link #teardown()}.
     */
    public boolean reconnect() {
        if (mLinkIn) {
            mTeardownRequested.set(false);
            connect();
        }
        return mLinkIn;
    }

    public static String getMaskFromIp(String ip) {
        if(ip == null) {
            return "255.255.255.255";
        }
        String ary0 = ip.substring(0, ip.indexOf("."));
        if(ary0 == null) {
            return "255.255.255.255";
        }
        Integer itg = Integer.valueOf(ary0);
        if(itg == null) {
            return "255.255.255.255";
        }
        int i = itg.intValue();
        if(i < 128 && i > 0) {
            return "255.0.0.0";
        } else if(i < 192) {
            return "255.255.0.0";
        } else if(i < 224) {
            return "255.255.255.0";
        } else {
            return "255.255.255.255";
        }
    }

    public DhcpResults getIpConfigure(EthernetDevInfo info){
        InetAddress netmask = null;
        InetAddress gw = null;
        RouteInfo routeAddress = null;
        DhcpResults dhcpResults = new DhcpResults();

        if(info == null)
            return dhcpResults;
        try {
            if (info.getNetMask() == null || info.getNetMask().matches("") ){
                netmask = NetworkUtils.numericToInetAddress(getMaskFromIp(info.getIpAddress()));
            } else {
                netmask = NetworkUtils.numericToInetAddress(info.getNetMask());
            }
        } catch (IllegalArgumentException e) {
            netmask = NetworkUtils.numericToInetAddress("255.255.255.255");
        }
        prefixLength = NetworkUtils.netmaskIntToPrefixLength(NetworkUtils.inetAddressToInt((Inet4Address)netmask));
        dhcpResults.addLinkAddress(info.getIpAddress(), prefixLength);
        dhcpResults.addGateway(info.getGateWay());
        dhcpResults.addDns(info.getDns1());
        return dhcpResults;
    }

    public DhcpResults getDhcpResults() {
        return mDhcpResults;
    }

    /**
     * Begin monitoring connectivity
     */
    public void startMonitoring(Context context, Handler target) {
        mContext = context;
        mCsHandler = target;

        // register for notifications from NetworkManagement Service
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNMService = INetworkManagementService.Stub.asInterface(b);
        mInterfaceObserver = new InterfaceObserver(this);
        mEthManager = (EthernetManager)mContext.getSystemService(Context.ETHERNET_SERVICE);
        Class mBroadcastTrans = null;
        try{
            mBroadcastTrans = Class.forName("android.net.ethernet.BroadcastTrans");
            Constructor con = mBroadcastTrans.getConstructor(Context.class, EthernetManager.class);
            Object BroadcastTrans = con.newInstance(mContext, mEthManager);
        } catch(Exception ex) {
            Log.d(TAG, "register android.net.ethernet.BroadcastTrans fail."
                    + "  ms(" + ex.getMessage() + ")");
        }

        // enable and try to connect to an ethernet interface that already exists
        sIfaceMatch = context.getResources().getString(
            com.android.internal.R.string.config_ethernet_iface_regex);

        List<EthernetDevInfo> ethInfos = mEthManager.getDeviceList();
        EthernetDevInfo saveInfo = mEthManager.getStaticConfig();
        if(saveInfo != null && ethInfos != null) {
            for (EthernetDevInfo info : ethInfos) {
                if (info.getIfName().matches(saveInfo.getIfName())){
                    saveInfo.setIfName(info.getIfName());
                    saveInfo.setHwaddr(info.getHwaddr());
                    Log.d(TAG, "startMonitoring: update stored EthernetDevInfo.");
                    mEthManager.setStaticConfig(saveInfo);
                }
            }
        }
        try {
            final String[] ifaces = mNMService.listInterfaces();
            for (String iface : ifaces) {
                if (iface.matches(sIfaceMatch)) {
                    mIface = iface;
                    try {
                        mNMService.setInterfaceUp(iface);
                        Log.d(TAG, "Set interface(" + iface + ") up!");
                    } catch (Exception e) {
                        Log.e(TAG, "Error upping interface " + iface + ": " + e);
                    }

                    InterfaceConfiguration config = mNMService.getInterfaceConfig(iface);
                    mLinkUp = config.hasFlag("up");
                    mLinkIn = checkLink(mIface);
                    if (config != null && mHwaddr == null) {
                        mHwaddr = config.getHardwareAddress();
                        if (mHwaddr != null) {
                            mNetworkInfo.setExtraInfo(mHwaddr);
                        }
                    }
                    // Ensure the first match ethernet interface is the active
                    break;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Could not get list of interfaces " + e);
        }

        if(mPppoeHandler == null) {
            HandlerThread handlerThread = new HandlerThread("EthernetPppoeThread");
            handlerThread.start();
            mPppoeHandler = new Handler(handlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case PppoeStateMachine.PPPOE_SUCCESS: {
                            mDhcpResults.clear();
                            mDhcpResults = (DhcpResults) msg.obj;
                            // DHCP success
                            // 1. Update mDhcpResults/mLinkProperties/mNetworkInfo
                            // 2. Broadcast network state changed to ConnectivityService
                            // 3. Broadcast network state changed to Ethernet Setting
                            mLinkProperties = mDhcpResults.linkProperties;
                            mNetworkInfo.setIsAvailable(true);
                            mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                            Message mCsMessage = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
                            mCsMessage.sendToTarget();
                            sendNetStateBroadcast(EthernetManager.EVENT_PPPOE_CONNECT_SUCCESSED);
                            break;
                        }
                        case PppoeStateMachine.PPPOE_FAILURE: {
                            sendNetStateBroadcast(EthernetManager.EVENT_PPPOE_CONNECT_FAILED);
                            break;
                        }
                        case PppoeStateMachine.PPPOE_HANGUP: {
                            disconnect();
                            break;
                        }
                    }
                }
            };
        }
        if(mPppoeSM == null) {
            mPppoeSM = PppoeStateMachine.makePppoeStateMachine(mContext, mPppoeHandler, mIface);
        }
        mReadyToConnect = true;

        try {
            mNMService.registerObserver(mInterfaceObserver);
            Log.d(TAG, "Register observer success!");
        } catch (RemoteException e) {
            Log.e(TAG, "Could not register InterfaceObserver " + e);
        }
    }

    public String getActiveIface() {
        return mIface;
    }

    public boolean getLinkState() {
        return mLinkIn;
    }

    /**
     * @param ifname the string that identifies the network interface
     * check if the interface linkin or not.
     * cat sys/class/net/ethx/carrier 1 means link in,
     * carrier is 0 or doesn't exist means link out.
     * @return {@code true} if linkin
     */
    public boolean checkLink(String ifname) {
        boolean ret = false;
        File filefd = null;
        FileInputStream fstream = null;
        String s = null;
        try {
            if(!(new File(SYS_NET + ifname).exists()))
                return false;
            fstream = new FileInputStream(SYS_NET + ifname + "/carrier");
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            s = br.readLine();
        } catch (IOException ex) {
            Log.e(TAG, "checkLink error: " + ex);
        } finally {
            if (fstream != null) {
                try {
                    fstream.close();
                } catch (IOException ex) {
                    Log.e(TAG, "checkLink fstream.close " + ex);
                }
            }
        }
        if(s != null && s.equals("1")) {
            ret = true;
        }
        Log.d(TAG, "checkLink: current link state " + (ret ? "link in" : "link out"));
        return ret;
    }

    /**
     * PPPOE_STOPPED 0
     * PPPOE_STARTING 1
     * PPPOE_STARTED 2
     * PPPOE_STOPPING 3
     */
    public int getPppoeStatus() {
        int mState = NetworkUtils.pppoeStatus(mIface);

        if(0 == mState) {
            return EthernetManager.PPPOE_STATE_STOPPED;
        } else if(1 == mState) {
            return EthernetManager.PPPOE_STATE_STARTING;
        } else if(2 == mState) {
            return EthernetManager.PPPOE_STATE_STARTED;
        } else if(3 == mState) {
            return EthernetManager.PPPOE_STATE_STOPPING;
        } else {
            return EthernetManager.PPPOE_STATE_UNKWON;
        }
    }

    public Object Clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public void setTeardownRequested(boolean isRequested) {
        mTeardownRequested.set(isRequested);
    }

    public boolean isTeardownRequested() {
        return mTeardownRequested.get();
    }

    @Override
    public void captivePortalCheckComplete() {
        // not implemented
    }

    @Override
    public void captivePortalCheckCompleted(boolean isCaptivePortal) {
        // not implemented
    }

    /**
     * Turn the wireless radio off for a network.
     * @param turnOn {@code true} to turn the radio on, {@code false}
     */
    public boolean setRadio(boolean turnOn) {
        return true;
    }

    /**
     * @return true - If are we currently tethered with another device.
     */
    public synchronized boolean isAvailable() {
        return mNetworkInfo.isAvailable();
    }

    /**
     * Tells the underlying networking system that the caller wants to
     * begin using the named feature. The interpretation of {@code feature}
     * is completely up to each networking implementation.
     * @param feature the name of the feature to be used
     * @param callingPid the process ID of the process that is issuing this request
     * @param callingUid the user ID of the process that is issuing this request
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is specific to each networking
     * implementation+feature combination, except that the value {@code -1}
     * always indicates failure.
     * TODO: needs to go away
     */
    public int startUsingNetworkFeature(String feature, int callingPid, int callingUid) {
        return -1;
    }

    /**
     * Tells the underlying networking system that the caller is finished
     * using the named feature. The interpretation of {@code feature}
     * is completely up to each networking implementation.
     * @param feature the name of the feature that is no longer needed.
     * @param callingPid the process ID of the process that is issuing this request
     * @param callingUid the user ID of the process that is issuing this request
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is specific to each networking
     * implementation+feature combination, except that the value {@code -1}
     * always indicates failure.
     * TODO: needs to go away
     */
    public int stopUsingNetworkFeature(String feature, int callingPid, int callingUid) {
        return -1;
    }

    @Override
    public void setUserDataEnable(boolean enabled) {
        Log.w(TAG, "ignoring setUserDataEnable(" + enabled + ")");
    }

    @Override
    public void setPolicyDataEnable(boolean enabled) {
        Log.w(TAG, "ignoring setPolicyDataEnable(" + enabled + ")");
    }

    /**
     * Check if private DNS route is set for the network
     */
    public boolean isPrivateDnsRouteSet() {
        return mPrivateDnsRouteSet.get();
    }

    /**
     * Set a flag indicating private DNS route is set
     */
    public void privateDnsRouteSet(boolean enabled) {
        mPrivateDnsRouteSet.set(enabled);
    }

    /**
     * Fetch NetworkInfo for the network
     */
    public synchronized NetworkInfo getNetworkInfo() {
        return mNetworkInfo;
    }

    /**
     * Fetch LinkProperties for the network
     */
    public synchronized LinkProperties getLinkProperties() {
        return new LinkProperties(mLinkProperties);
    }

   /**
     * A capability is an Integer/String pair, the capabilities
     * are defined in the class LinkSocket#Key.
     *
     * @return a copy of this connections capabilities, may be empty but never null.
     */
    public LinkCapabilities getLinkCapabilities() {
        return new LinkCapabilities(mLinkCapabilities);
    }

    /**
     * Fetch default gateway address for the network
     */
    public int getDefaultGatewayAddr() {
        return mDefaultGatewayAddr.get();
    }

    /**
     * Check if default route is set
     */
    public boolean isDefaultRouteSet() {
        return mDefaultRouteSet.get();
    }

    /**
     * Set a flag indicating default route is set for the network
     */
    public void defaultRouteSet(boolean enabled) {
        mDefaultRouteSet.set(enabled);
    }

    /**
     * Return the system properties name associated with the tcp buffer sizes
     * for this network.
     */
    public String getTcpBufferSizesPropName() {
        return "net.tcp.buffersize.wifi";
    }

    public void setDependencyMet(boolean met) {
        // not supported on this network
    }

    @Override
    public void addStackedLink(LinkProperties link) {
        mLinkProperties.addStackedLink(link);
    }

    @Override
    public void removeStackedLink(LinkProperties link) {
        mLinkProperties.removeStackedLink(link);
    }

    @Override
    public void supplyMessenger(Messenger messenger) {
        // not supported on this network
    }
}
