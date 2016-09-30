/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.net.pppoe;

import android.net.DhcpInfo;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.Reader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.net.ethernet.IEthernetManager;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetDevInfo;
import android.net.ethernet.BroadcastTrans;

public class PppoeManager {
    private static final String TAG = "PppoeManager";

    private EthernetManager mEthManager;
    private BroadcastTrans mBroadcastTrans;

    public static final String MSTAR_PPPOE_STATE_CONNECT = "connect";
    public static final String MSTAR_PPPOE_STATE_DISCONNECT = "disconnect";
    public static final String MSTAR_PPPOE_STATE_CONNECTING = "connecting";
    public static final String MSTAR_PPPOE_STATE_DISCONNECTING = "disconnecting";
    public static final String MSTAR_PPPOE_STATE_AUTHFAILED = "authfailed";
    public static final String MSTAR_PPPOE_STATE_LINKTIMEOUT = "linktimeout";
    public static final String MSTAR_PPPOE_STATE_FAILED = "failed";

    public static final int MSG_PPPOE_CONNECT = 0;
    public static final int MSG_PPPOE_DISCONNECT = 1;
    public static final int MSG_PPPOE_CONNECTING = 2;
    public static final int MSG_PPPOE_DISCONNECTING = 3;
    public static final int MSG_PPPOE_AUTH_FAILED = 4;
    public static final int MSG_PPPOE_TIME_OUT = 5;
    public static final int MSG_PPPOE_FAILED = 6;

    public static final String PPPOE_STATE_ACTION = "PPPOE_STATE_CHANGED";
    public static final String PPPOE_STATE_STATUE = "PppoeStatus";

    public static final String PPPOE_STATE_CHANGED_ACTION = "PPPOE_STATE_CHANGED";
    public static final String EXTRA_PPPOE_STATE = "pppoe_state";
    public static final int PPPOE_STATE_DISABLED = 1;
    public static final int PPPOE_STATE_ENABLED = 0;

    private static final boolean DEBUG = true;

    /**
     *function:PppoeManager constructor
     */
    public PppoeManager(Context context) {
        if(DEBUG){
            Log.d(TAG,"PppoeManager");
        }
        mEthManager = EthernetManager.getInstance();
        mBroadcastTrans = new BroadcastTrans(context, mEthManager);
    }

    /**
     *function:connectPppoe
     */
    public void connectPppoe(final String account, final String password, final String ifnet) {
        if(DEBUG){
            Log.d(TAG,"connectPppoe");
        }
        EthernetDevInfo devInfo = new EthernetDevInfo();
        devInfo.setMode(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE);
        devInfo.setUsername(account);
        devInfo.setPasswd(password);
        mEthManager.setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE, devInfo);
    }

    /**
     *function:disconnectPppoe
     */
    public void disconnectPppoe() {
        if(DEBUG){
            Log.d(TAG,"disconnectPppoe");
        }
        /*
         * we teardown ethernet before reconnect
         * so we don't need disconnect
         */
    }
    /**
     *function:set pppoe status
     */
    private void setPppoeStatus(int status, boolean sendBroadcast) {
        if(DEBUG){
            Log.d(TAG,"setPppoeStatus");
        }
        /*
         * do nothing
         */
    }

    /**
     *function:get pppoe status
     */
    public int getPppoeStatus() {
        int tmp = -1;
        if(DEBUG){
            Log.d(TAG,"getPppoeStatus");
        }
        tmp = mEthManager.getPppoeStatus();
        if(DEBUG){
            Log.d(TAG,"getPppoeStatus" + tmp);
        }
        switch(tmp) {
            case EthernetManager.PPPOE_STATE_STOPED:
                return PPPOE_STATE_DISCONNECT;
            case EthernetManager.PPPOE_STATE_STARTING:
                return PPPOE_STATE_CONNECTING;
            case EthernetManager.PPPOE_STATE_STARTED:
                return PPPOE_STATE_CONNECT;
            case EthernetManager.PPPOE_STATE_EXIT:
                return PPPOE_STATE_EXIT;
            case EthernetManager.PPPOE_STATE_UNKWON:
                return PPPOE_STATE_UNKNOWN;
            default:
                return PPPOE_STATE_UNKNOWN;
        }
    }
    public static final int PPPOE_STATE_UNKNOWN = 0;
    public static final int PPPOE_STATE_CONNECT = 1;
    public static final int PPPOE_STATE_DISCONNECT = 2;
    public static final int PPPOE_STATE_CONNECTING = 3;
    public static final int PPPOE_STATE_RETRY = 4;
    public static final int PPPOE_STATE_EXIT = 5;
    public static final int EVENT_CONNECT_SUCCESSED = 0;
    public static final int EVENT_CONNECT_FAILED = 1;
    /**
     * The pppoe interface is configured by dhcp
     */
    public static final String PPPOE_CONNECT_MODE_DHCP= "dhcp";
    /**
     * The pppoe interface is configured manually
     */
    public static final String PPPOE_CONNECT_MODE_MANUAL = "manual";
    /**
     * @param name
     * @param pswd
     */
    public synchronized void connect(String name,String pswd,String ifaceName){
        if(DEBUG){
            Log.d(TAG,"connect");
        }
        connectPppoe(name,pswd,ifaceName);
    }

    public synchronized void disconnect(String ifaceName){
        if(DEBUG){
            Log.d(TAG,"disconnect");
        }
        disconnectPppoe();
    }
    /**
     * @param mode
     * @param info
     */
    public synchronized void setPppoeMode(String mode,DhcpInfo info) {
        if(DEBUG){
            Log.d(TAG,"setPppoeMode");
        }
        /*
         * do nothing
         */
    }
    /**
     * @return mode PPOE_CONNECT_MODE_DHCP or PPPOE_CONNECT_MODE_MANUAL
     */
    public synchronized String getPppoeMode() {
        if(DEBUG){
            Log.d(TAG,"getPppoeMode");
        }
        return PPPOE_CONNECT_MODE_DHCP;
    }
    /**
     * PPPOE_STATE_UNKNOWN
     * PPPOE_STATE_CONNECT
     * PPPOE_STATE_DISCONNECT
     * PPPOE_STATE_CONNECTING
     */
    public int getPppoeState() {
        int tmp = -1;
        tmp = getPppoeStatus();
        if(DEBUG){
            Log.d(TAG,"getPppoeState : " + tmp);
        }

        return tmp;
    }

    public boolean isPppoeDeviceUp(){
        if(DEBUG){
            Log.d(TAG,"isPppoeDeviceUp");
        }
        return true;
    }

    public DhcpInfo getDhcpInfo() {
        if(DEBUG){
            Log.d(TAG,"getDhcpInfo");
        }
        return mEthManager.getDhcpInfo();
    }

}
