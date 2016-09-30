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

package com.android.settings.ethernet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import android.net.ethernet.EthernetManager;
import android.util.Log;

public class EthernetEnabler implements CompoundButton.OnCheckedChangeListener  {
    private final Context mContext;
    private Switch mSwitch;

    private boolean waitForConnectResult = false;
    private boolean waitForDisconnectResult = false;

    private final EthernetManager mEthernetManager;
    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (EthernetManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                final int event = intent.getIntExtra(EthernetManager.EXTRA_ETHERNET_STATE,
                        EthernetManager.EVENT_ETHERNET_CONNECT_SUCCESSED);
                switch(event) {
                    case EthernetManager.EVENT_ETHERNET_CONNECT_SUCCESSED:
                    case EthernetManager.EVENT_ETHERNET_CONNECT_FAILED:
                    case EthernetManager.EVENT_PPPOE_CONNECT_SUCCESSED:
                    case EthernetManager.EVENT_PPPOE_CONNECT_FAILED:
                        if(waitForConnectResult) {
                            mSwitch.setEnabled(true);
                            waitForConnectResult = false;
                        }
                        break;
                    case EthernetManager.EVENT_ETHERNET_DISCONNECT_FAILED:
                    case EthernetManager.EVENT_ETHERNET_DISCONNECT_SUCCESSED:
                    case EthernetManager.EVENT_PPPOE_DISCONNECT_SUCCESSED:
                        if(waitForDisconnectResult) {
                            mSwitch.setEnabled(true);
                            waitForDisconnectResult = false;
                        }
                        break;
                    default:
                        break;
                }
            } else if(EthernetManager.ETHERNET_STATE_CHANGED_ACTION.equals(action)) {
                final int event = intent.getIntExtra(EthernetManager.EXTRA_ETHERNET_STATE,
                        EthernetManager.EVENT_INTERFACE_ADDED);
                switch(event) {
                    case EthernetManager.EVENT_INTERFACE_REMOVED:
                    case EthernetManager.EVENT_PHY_LINK_OUT:
                        mSwitch.setEnabled(true);
                        waitForDisconnectResult = false;
                        waitForConnectResult = false;
                        break;
                    default:
                        break;
                }
            }
        }
    };

    public EthernetEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;

        mEthernetManager = (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(EthernetManager.NETWORK_STATE_CHANGED_ACTION);
    }

    public void resume() {
        // Wi-Fi state is sticky, so just let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitch.setOnCheckedChangeListener(this);
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);

        final int EthernetState = mEthernetManager.getEthernetState();
        boolean isEnabled = EthernetState == mEthernetManager.ETHERNET_STATE_ENABLED;
        mSwitch.setChecked(isEnabled);
        mSwitch.setEnabled(!(waitForConnectResult || waitForDisconnectResult));
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
            if(mEthernetManager.getLinkState()) {
                waitForConnectResult = true;
                mSwitch.setEnabled(false);
            } else {
                waitForConnectResult = false;
                mSwitch.setEnabled(true);
            }
            setEnabled(isChecked);
        } else {
            if(mEthernetManager.getLinkState()) {
                waitForDisconnectResult = true;
                mSwitch.setEnabled(false);
            } else {
                waitForDisconnectResult = false;
                mSwitch.setEnabled(true);
            }
            setEnabled(isChecked);
        }
    }

    private void setEnabled(final boolean enable) {
        final int EthernetState = mEthernetManager.getEthernetState();
        boolean isEnabled = EthernetState == mEthernetManager.ETHERNET_STATE_ENABLED;
        if(enable == isEnabled) {
            if(enable) {
                waitForConnectResult = false;
            } else {
                waitForDisconnectResult = false;
            }
            mSwitch.setEnabled(true);
            return;
        }
        Thread setEnabledThread = new Thread(new Runnable() {
            public void run() {
                mEthernetManager.setEthernetEnabled(enable);
            }
        });
        setEnabledThread.start();
    }
}
