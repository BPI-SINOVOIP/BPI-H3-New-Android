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

package com.android.server;

import android.net.ethernet.IEthernetManager;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetDevInfo;
import android.net.EthernetDataTracker;
import android.net.NetworkUtils;
import android.net.InterfaceConfiguration;
import android.net.LinkProperties;
import android.net.LinkAddress;
import android.net.DhcpInfo;
import android.net.RouteInfo;
import android.net.DhcpResults;
import android.os.INetworkManagementService;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.net.Inet4Address;


/**
 * EthernetService handles remote Ethernet operation requests by implementing
 * the IEthernetManager interface. It also creates a EtherentMonitor to listen
 * for Etherent-related events.
 */
public class EthernetService extends IEthernetManager.Stub {

    private static final String TAG         = "EthernetService";
    private static final boolean DBG        = true;
    private final String SYS_NET            = "/sys/class/net/";
    private final String IPOE_CONFIG_FILE   = "/data/system/ipoe.config";
    private final String PPPOE_PAP_CONFIG_FILE      = "/data/system/pap-secrets";
    private final String PPPOE_CHAP_CONFIG_FILE     = "/data/system/chap-secrets";
    private final String PPPOE_CONFIG_FORMAT        = "\"%s\" * \"%s\"";
    private final int MAX_INFO_LENGTH               = 128;

    private Context mContext;
    private EthernetDataTracker mTracker;

    /* gathering all the ethx configuration */
    private HashMap<String, EthernetDevInfo> mDeviceMap;
    private final INetworkManagementService mNMService;

    public EthernetService(Context context) {
        mContext = context;
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNMService = INetworkManagementService.Stub.asInterface(b);
        mDeviceMap = new HashMap<String, EthernetDevInfo>();

        Log.d(TAG, "EthernetService Starting.\n");
        mTracker = EthernetDataTracker.getInstance();
        scanDevice();
        Log.d(TAG, "EthernetService Started.\n");
    }

    public boolean addInterfaceToService(String iface) {
       	if(!isEth(iface))
            return false;
        if(!(new File(SYS_NET + iface + "/ifindex").exists())) {
            if(DBG) Log.d(TAG, "addInterfaceToService: ifindex no such file!");
            return false;
        }
        synchronized(mDeviceMap) {
           try{
               if(!mDeviceMap.containsKey(iface)) {
                   EthernetDevInfo value = new EthernetDevInfo();
                   InterfaceConfiguration config = mNMService.getInterfaceConfig(iface);
                   value.setIfName(iface);
                   value.setHwaddr(config.getHardwareAddress());
                   mDeviceMap.put(iface, value);
                   if(DBG) Log.d(TAG, "addInterfaceToService: " + iface);
                   sendChangedBroadcast(value, EthernetManager.EVENT_NEWDEV);
               }
           } catch (RemoteException e) {
               Log.e(TAG, "Can't get the Interface Configure" + e);
     	   }
        }
        return true;
    }

    public boolean removeInterfaceFromService(String iface) {
        if(!isEth(iface))
            return false;
        synchronized(mDeviceMap) {
            if(mDeviceMap.containsKey(iface)) {
                sendChangedBroadcast(mDeviceMap.get(iface), EthernetManager.EVENT_DEVREM);
                mDeviceMap.remove(iface);
                if(DBG) Log.d(TAG, "removeInterfaceFormService: " + iface);
            }
        }
        return true;
    }

    public List<EthernetDevInfo> getDeviceList() {
        List<EthernetDevInfo> devList = new ArrayList<EthernetDevInfo>();

        synchronized(mDeviceMap){
            if(mDeviceMap.size() == 0)
                return null;
            for(EthernetDevInfo devinfo : mDeviceMap.values()){
                devList.add(devinfo);
            }
        }
        return devList;
    }

    public EthernetDevInfo getDevInfo() {
        Log.d(TAG, "getDevInfo");

        String mode = getEthernetMode();
        if (mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
            return getStaticConfig();
        } else {
            // Convert DhcpResults to EthernetDevInfo
            DhcpResults results = mTracker.getDhcpResults();
            LinkProperties lp = results.linkProperties;

            EthernetDevInfo info = mDeviceMap.get(mTracker.getActiveIface());
            if (info == null) {
                Log.e(TAG, "No active network inteface");
                return new EthernetDevInfo();
            }
            for (LinkAddress i : lp.getLinkAddresses()) {
                // Set IP
                info.setIpAddress(i.getAddress().getHostAddress());
                // Set netmask(0xffffff00 ---> 255.255.255.0)
                // Attent network byte order
                int r_mask = 0xffffffff;
                r_mask = r_mask << (32 - i.getNetworkPrefixLength());
                r_mask = Integer.reverseBytes(r_mask);
                info.setNetMask(NetworkUtils.intToInetAddress(r_mask).getHostAddress());
                break;
            }
            for (RouteInfo i : lp.getRoutes()) {
                if (i.isDefaultRoute()) {
                    info.setGateWay(i.getGateway().getHostAddress());
                }
            }
            int j = 0;
            for (InetAddress i : lp.getDnses()) {
                if (j == 0) {
                    info.setDns1(i.getHostAddress());
                } else if (j == 1) {
                    info.setDns2(i.getHostAddress());
                } else {
                    break;
                }
                j++;
            }
            return info;
        }
    }
    /**
     * Return the DHCP-assigned addresses from the last successful DHCP request,
     * if any.
     * @return the DHCP information
     * @deprecated
     */
    public DhcpInfo getDhcpInfo() {
        DhcpResults dhcpResults = mTracker.getDhcpResults();
        if (dhcpResults.linkProperties == null) return null;

        DhcpInfo info = new DhcpInfo();
        for (LinkAddress la : dhcpResults.linkProperties.getLinkAddresses()) {
            InetAddress addr = la.getAddress();
            if (addr instanceof Inet4Address) {
                info.ipAddress = NetworkUtils.inetAddressToInt((Inet4Address)addr);
                break;
            }
        }
        for (RouteInfo r : dhcpResults.linkProperties.getRoutes()) {
            if (r.isDefaultRoute()) {
                InetAddress gateway = r.getGateway();
                if (gateway instanceof Inet4Address) {
                    info.gateway = NetworkUtils.inetAddressToInt((Inet4Address)gateway);
                }
            } else if (r.hasGateway() == false) {
                LinkAddress dest = r.getDestination();
                if (dest.getAddress() instanceof Inet4Address) {
                    info.netmask = NetworkUtils.prefixLengthToNetmaskInt(
                            dest.getNetworkPrefixLength());
                }
            }
        }
        int dnsFound = 0;
        for (InetAddress dns : dhcpResults.linkProperties.getDnses()) {
            if (dns instanceof Inet4Address) {
                if (dnsFound == 0) {
                    info.dns1 = NetworkUtils.inetAddressToInt((Inet4Address)dns);
                } else {
                    info.dns2 = NetworkUtils.inetAddressToInt((Inet4Address)dns);
                }
                if (++dnsFound > 1) break;
            }
        }
        InetAddress serverAddress = dhcpResults.serverAddress;
        if (serverAddress instanceof Inet4Address) {
            info.serverAddress = NetworkUtils.inetAddressToInt((Inet4Address)serverAddress);
        }
        info.leaseDuration = dhcpResults.leaseDuration;

        return info;
    }

    public String getEthernetMode() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            String mode = EthernetManager.ETHERNET_CONNECT_MODE_DHCP;
            if(1 == Settings.Global.getInt(cr,Settings.Global.ETHERNET_USE_PPPOE)) {
                mode = EthernetManager.ETHERNET_CONNECT_MODE_PPPOE;
            } else {
                mode = Settings.Global.getString(cr, Settings.Global.ETHERNET_MODE);
            }
            Log.d(TAG, "Current ethernet mode: "  + mode);
            return mode;
        } catch (Exception ex) {
            Log.e(TAG, "getEthernetMode error!");
            return EthernetManager.ETHERNET_CONNECT_MODE_DHCP;
        }
    }

    public void setEthernetMode(final String type, final EthernetDevInfo info) {
        Log.d(TAG, "setEthernetMode: Mode=" + type + " pid=" + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingUid());
        long ident = Binder.clearCallingIdentity();
        try {
            final ContentResolver cr = mContext.getContentResolver();
            //start a new thread to disconnect and connect
            Thread setModeThread = new Thread(new Runnable() {
                public void run() {
                    if (type.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)) {
                        // 1. teardown current connected network
                        // 2. Save current  mode to database
                        // 3. start to connect new mode
                        mTracker.teardown();
                        Settings.Global.putInt(cr, Settings.Global.ETHERNET_USE_PPPOE, 0);
                        Settings.Global.putString(cr, Settings.Global.ETHERNET_MODE, type);
                        mTracker.reconnect();
                    } else if (type.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
                        // 1. teardown current connected network
                        // 2. Save current  mode and static info to database
                        // 3. start to connect new mode
                        mTracker.teardown();
                        Settings.Global.putInt(cr, Settings.Global.ETHERNET_USE_PPPOE, 0);
                        if(info == null) {
                            Settings.Global.putString(cr, Settings.Global.ETHERNET_MODE, type);
                        } else {
                            setStaticConfig(info);
                        }
                        mTracker.reconnect();
                    } else if (type.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)){
                        // 1. teardown current connected network
                        // 2. Save current  mode and login info to config file
                        // 3. start to connect new mode
                        mTracker.teardown();
                        Settings.Global.putInt(cr, Settings.Global.ETHERNET_USE_PPPOE, 1);
                        if(info != null) {
                            saveLoginInfo(type, info);
                        }
                        mTracker.reconnect();
                    }
                }
            });
            setModeThread.start();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setEthernetEnabled(boolean enable) {
        Log.d(TAG, "setEthernetEnabled: Enabe=" + enable + " pid=" + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingUid());

        long ident = Binder.clearCallingIdentity();
        try {
            // Save ethernet enable state to database
            final ContentResolver cr = mContext.getContentResolver();
            if (getEthernetState() == EthernetManager.ETHERNET_STATE_ENABLED) {
                if (!enable) {
                    mTracker.teardown();
                    Settings.Global.putInt(cr, Settings.Global.ETHERNET_ON, enable ? 1 : 0);
                    Intent intent = new Intent(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
                    intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                            | Intent.FLAG_RECEIVER_REPLACE_PENDING);
                    intent.putExtra(EthernetManager.EXTRA_ETHERNET_STATE, EthernetManager.ETHERNET_STATE_DISABLED);
                    mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                }
            } else {
                if (enable) {
                    Settings.Global.putInt(cr, Settings.Global.ETHERNET_ON, enable ? 1 : 0);
                    mTracker.reconnect();
                    Intent intent = new Intent(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
                    intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                            | Intent.FLAG_RECEIVER_REPLACE_PENDING);
                    intent.putExtra(EthernetManager.EXTRA_ETHERNET_STATE, EthernetManager.ETHERNET_STATE_ENABLED);
                    mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getEthernetState() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            int state = Settings.Global.getInt(cr, Settings.Global.ETHERNET_ON);
            Log.d(TAG, "Current ethernet state: "  + (state==EthernetManager.ETHERNET_STATE_ENABLED ? "enable" : "disable"));
            return state;
        } catch (Exception ex) {
            Log.e(TAG, "getEthernetState error!");
            return EthernetManager.ETHERNET_STATE_ENABLED;
        }
    }

    public int getPppoeStatus() {
        return mTracker.getPppoeStatus();
    }

    public boolean getLinkState() {
        boolean state = mTracker.getLinkState();
        Log.d(TAG, "Current ethernet link state: " + (state ? "link up" : "link down"));
        return state;
    }

    public boolean getWifiDisguiseState() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            int state = Settings.Secure.getInt(cr, Settings.Secure.WIFI_CHEAT_ON);
            Log.d(TAG, "Current wifi cheat state: "  + (state==1 ? "enable" : "disable"));
            if (state == 1)
                return true;
            else
                return false;
        } catch (Exception ex) {
            Log.e(TAG, "getWifiDisguiseState error!");
            return false;
        }
    }

    public void setWifiDisguise(boolean enable) {
        Log.d(TAG, "setWifiDisguise: Enabe=" + enable + " pid=" + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingUid());

        final ContentResolver cr = mContext.getContentResolver();
        long ident = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putInt(cr, Settings.Secure.WIFI_CHEAT_ON, enable ? 1 : 0);
            Log.d(TAG, "Current wifi cheat state: "  + (enable ? "enable" : "disable"));
        } catch (Exception ex) {
            Log.e(TAG, "setWifiDisguise error!");
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public EthernetDevInfo getStaticConfig() {
        Log.d(TAG, "getStaticConfig");

        final ContentResolver cr = mContext.getContentResolver();
        EthernetDevInfo info = new EthernetDevInfo();
        try {
            info.setIfName(Settings.Global.getString(cr, Settings.Global.ETHERNET_IFACENAME));
            info.setHwaddr(Settings.Global.getString(cr, Settings.Global.ETHERNET_HWADDR));
            info.setIpAddress(Settings.Global.getString(cr, Settings.Global.ETHERNET_IPADDR));
            info.setGateWay(Settings.Global.getString(cr, Settings.Global.ETHERNET_GATEWAY));
            info.setNetMask(Settings.Global.getString(cr, Settings.Global.ETHERNET_NETMASK));
            info.setDns1(Settings.Global.getString(cr, Settings.Global.ETHERNET_DNS1));
            info.setDns2(Settings.Global.getString(cr, Settings.Global.ETHERNET_DNS2));
            info.setMode(Settings.Global.getString(cr, Settings.Global.ETHERNET_MODE));
            if(DBG) Log.d(TAG, "getSavedConfig: iface: " + info.getIfName() +" ip: " + info.getIpAddress());
            return info;
        } catch (Exception e) {
            Log.e(TAG, "getStaticConfig error!");
            return info;
        }
    }

    public synchronized void setStaticConfig(EthernetDevInfo info) {
        Log.d(TAG, "setStaticConfig");

        long ident = Binder.clearCallingIdentity();
        try {
            final ContentResolver cr = mContext.getContentResolver();
            Settings.Global.putString(cr, Settings.Global.ETHERNET_IFACENAME, info.getIfName());
            Settings.Global.putString(cr, Settings.Global.ETHERNET_HWADDR, info.getHwaddr());
            Settings.Global.putString(cr, Settings.Global.ETHERNET_IPADDR, info.getIpAddress());
            Settings.Global.putString(cr, Settings.Global.ETHERNET_GATEWAY, info.getGateWay());
            Settings.Global.putString(cr, Settings.Global.ETHERNET_NETMASK, info.getNetMask());
            Settings.Global.putString(cr, Settings.Global.ETHERNET_DNS1, info.getDns1());
            Settings.Global.putString(cr, Settings.Global.ETHERNET_DNS2, info.getDns2());
            Settings.Global.putString(cr, Settings.Global.ETHERNET_MODE, info.getMode());
        } finally {
            Binder.restoreCallingIdentity(ident);
        }

        if(DBG) Log.d(TAG, "setStaticInfo: iface: " + info.getIfName() +" ip: " + info.getIpAddress());

        if ((info != null) && mDeviceMap.containsKey(info.getIfName())){
            mDeviceMap.put(info.getIfName(), info);
         }
    }

    public EthernetDevInfo getLoginInfo(String mode) {
        Log.d(TAG, "getLoginInfo, mode=" + mode);

        EthernetDevInfo info = new EthernetDevInfo();
        if (mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)) {
            // read IPoE login info from saved config file
            return  new EthernetDevInfo();
        } else if (mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)) {
            // read PPPoE login info from saved config file
            File file = new File(PPPOE_PAP_CONFIG_FILE);
            char[] buf = new char[MAX_INFO_LENGTH];
            String loginInfo = new String();
            FileReader in;
            try {
                in = new FileReader(file);
                BufferedReader bufferedreader = new BufferedReader(in);
                loginInfo = bufferedreader.readLine();
                Log.d(TAG, "read from " + PPPOE_PAP_CONFIG_FILE
                        + " login info = " + loginInfo);
                bufferedreader.close();
                in.close();
            } catch (IOException e) {
                Log.e(TAG, "Read " + PPPOE_PAP_CONFIG_FILE + " failed! " + e);
            }
            if (loginInfo != null) {
                String[] infoArr = loginInfo.split("\\*");
                for (int m = 0; m < infoArr.length; m++)
                    Log.d(TAG, m +  ":" + infoArr[m]);
                if (infoArr.length == 2) {
                    info.setUsername(infoArr[0].replace('\"', ' ').trim());
                    info.setPasswd(infoArr[1].replace('\"', ' ').trim());
                }
            }
        }
        return info;
    }

    private void scanDevice() {
        String[] Devices = null;
        try {
            Devices = mNMService.listInterfaces();
            for(String iface : Devices) {
                if(isEth(iface)) {
                    EthernetDevInfo value = new EthernetDevInfo();
                    InterfaceConfiguration config = mNMService.getInterfaceConfig(iface);
                    value.setIfName(iface);
                    value.setHwaddr(config.getHardwareAddress());
                    if(DBG) Log.d(TAG, "scanDevice: remember " + iface + " configuration.");
                    synchronized(mDeviceMap) {
                        mDeviceMap.put(iface, value);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not get Config of interfaces " + e);
        }
    }

    /*
     * ignore sit/lo/ppp/ippp/tun/gre/ip6/ etc...
     * plus the wireless entry, add more if necessary.
     */
    private boolean isEth(String ifname) {
        if(ifname.startsWith("sit") || ifname.startsWith("lo") || ifname.startsWith("ppp")
                || ifname.startsWith("ippp") || ifname.startsWith("tun") || ifname.startsWith("gre")
                || ifname.startsWith("ip6") || ifname.startsWith("rndis") || ifname.startsWith("bt-pan")
                || ifname.startsWith("wlan") || ifname.startsWith("p2p"))
        return false;
        if(new File(SYS_NET + ifname + "/phy80211").exists())
            return false;
        if(new File(SYS_NET + ifname + "/wireless").exists())
            return false;
        if(new File(SYS_NET + ifname + "/wimax").exists())
            return false;
        if(new File(SYS_NET + ifname + "/bridge").exists())
            return false;
        if(DBG) Log.d(TAG, "isEth: " + ifname + " is ethernet device.");
            return true;
    }

    private void saveLoginInfo(String mode , EthernetDevInfo info) {
        if (mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)) {
            // 1. Save IPoE config
        } else if (mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)) {
            // 2. Save PPPoE config
            File pap_file = new File(PPPOE_PAP_CONFIG_FILE);
            File chap_file = new File(PPPOE_CHAP_CONFIG_FILE);
            String loginInfo = String.format(PPPOE_CONFIG_FORMAT, info.getUsername(), info.getPasswd());
            try {
                // 2.1. Save pap-secrets
                BufferedOutputStream out = new BufferedOutputStream(
                        new FileOutputStream(pap_file));
                out.write(loginInfo.getBytes(), 0, loginInfo.length());
                Log.d(TAG, "write to " + PPPOE_PAP_CONFIG_FILE
                        + " login info = " + loginInfo);
                out.flush();
                out.close();
                // 2.2. Save chap-secrets
                out = new BufferedOutputStream(
                        new FileOutputStream(chap_file));
                out.write(loginInfo.getBytes(), 0, loginInfo.length());
                Log.d(TAG, "write to " + PPPOE_CHAP_CONFIG_FILE
                        + " login info = " + loginInfo);
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Write PPPoE config failed!" + e);
            }
        }

    }

    public boolean checkLink(String ifname) {
        return mTracker.checkLink(ifname);
    }

    public void disconnect() {
        mTracker.teardown();
    }

    private void sendChangedBroadcast(EthernetDevInfo info, int event) {
        Intent intent = new Intent(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                | Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(EthernetManager.EXTRA_ETHERNET_STATE, event);
        mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }
}
