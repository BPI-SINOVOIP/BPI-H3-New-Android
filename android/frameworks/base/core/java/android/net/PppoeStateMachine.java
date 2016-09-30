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

package android.net;

import com.android.internal.util.Protocol;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpResults;
import android.net.NetworkUtils;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.os.Handler;

/**
 * @hide
 */
public class PppoeStateMachine extends StateMachine {

    private static String TAG = "PppoeStateMachine-";
    private static final boolean DBG = true;


    /* A StateMachine that controls the DhcpStateMachine */
    private Handler mHandler;

    private Context mContext;
    private BroadcastReceiver mBroadcastReceiver;

    //Remember DHCP configuration from first request
    private DhcpResults mDhcpResults;

    private static String mInterfaceName = null;
    private static boolean running = false;
    private static int lastState = 0;

    private static final int BASE = Protocol.BASE_PPPOE;

    public static final int CMD_START_PPPOE                  = BASE + 1;
    public static final int CMD_STOP_PPPOE                   = BASE + 2;
    public static final int CMD_START_PPPOE_SUCCESS          = BASE + 3;
    public static final int CMD_START_PPPOE_FAILURE          = BASE + 4;
    public static final int CMD_STATE_CHANGED                = BASE + 5;
    public static final int CMD_ON_QUIT                      = BASE + 6;

    /* Message.arg1 arguments to CMD_POST_PPPOE notification */
    public static final int PPPOE_SUCCESS =  11;
    public static final int PPPOE_FAILURE =  12;
    public static final int PPPOE_STOPPED =  13;
    public static final int PPPOE_HANGUP  =  14;

    private State mDefaultState = new DefaultState();
    private State mStoppedState = new StoppedState();
    private State mStartingState = new StartingState();
    private State mStartedState = new StartedState();

    private PppoeStateMachine(Context context, Handler handler, String intf) {
        super(TAG);

        mContext = context;
        mHandler = handler;
        mInterfaceName = intf;
        mDhcpResults = new DhcpResults();
        NetworkUtils.stopPppoe(mInterfaceName);

        addState(mDefaultState);
            addState(mStoppedState, mDefaultState);
            addState(mStartingState, mDefaultState);
                addState(mStartedState, mStartingState);

        setInitialState(mStoppedState);
    }

    public static PppoeStateMachine makePppoeStateMachine(Context context, Handler handler,
            String intf) {
        TAG += intf;
        PppoeStateMachine dsm = new PppoeStateMachine(context, handler, intf);
        dsm.start();
        running = true;
        new MonitorThread(dsm).start();
        return dsm;
    }

    /**
     * Quit the DhcpStateMachine.
     *
     * @hide
     */
    public void doQuit() {
        quit();
        running = false;
    }

    protected void onQuitting() {
        mHandler.obtainMessage(CMD_ON_QUIT).sendToTarget();
    }

    class DefaultState extends State {
        @Override
        public void exit() {
            mContext.unregisterReceiver(mBroadcastReceiver);
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                default:
                    Log.e(TAG, "Error! unhandled message  " + message);
                    break;
            }
            return HANDLED;
        }
    }


    class StoppedState extends State {
        @Override
        public void enter() {
            if (DBG) Log.d(TAG, getName() + "\n");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retValue = HANDLED;
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_START_PPPOE:
                    String username = (String)message.obj;
                    startPppoe(mInterfaceName, username);
                    transitionTo(mStartingState);
                    break;
                case CMD_STOP_PPPOE:
                case CMD_START_PPPOE_SUCCESS:
                case CMD_START_PPPOE_FAILURE:
                    //ignore
                    break;
                case CMD_STATE_CHANGED:
                    // TODO:
                    break;
                default:
                    retValue = NOT_HANDLED;
                    break;
            }
            return retValue;
        }
    }

    class StartingState extends State {
        @Override
        public void enter() {
            if (DBG) Log.d(TAG, getName() + "\n");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retValue = HANDLED;
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_STOP_PPPOE:
                    if (!NetworkUtils.stopPppoe(mInterfaceName)) {
                        Log.e(TAG, "stopPppoe error: " + NetworkUtils.getPppoeError());
                    } else {
                        mHandler.obtainMessage(PPPOE_STOPPED).sendToTarget();
                        transitionTo(mStoppedState);
                    }
                    break;
                case CMD_START_PPPOE:
                case CMD_STATE_CHANGED:
                    //ignore
                    break;
                case CMD_START_PPPOE_SUCCESS:
                    mHandler.obtainMessage(PPPOE_SUCCESS, (DhcpResults)message.obj).sendToTarget();
                    transitionTo(mStartedState);
                    break;
                case CMD_START_PPPOE_FAILURE:
                    mHandler.obtainMessage(PPPOE_FAILURE).sendToTarget();
                    transitionTo(mStoppedState);
                    break;
                default:
                    retValue = NOT_HANDLED;
            }
            return retValue;
        }
    }

    class StartedState extends State {
        @Override
        public void enter() {
            if (DBG) Log.d(TAG, getName() + "\n");
            updateState();
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retValue = HANDLED;
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_STATE_CHANGED:
                    // TODO: pppoe state change to stopped
                    String state = (String)message.obj;
                    if (state.equals("PPPOE_STOPPED") || state.equals("PPPOE_STOPPING")) {
                        mHandler.obtainMessage(PPPOE_HANGUP).sendToTarget();
                        transitionTo(mStoppedState);
                    } else {
                        //ignore
                    }
                    break;
                case CMD_START_PPPOE:
                case CMD_START_PPPOE_SUCCESS:
                    //ignore
                    break;
                default:
                    retValue = NOT_HANDLED;
            }
            return retValue;
        }
    }

    private void startPppoe(final String mInterfaceName, final String username) {
        if (DBG) Log.d(TAG, "startPppoe request on " + mInterfaceName);
        Thread pppoeThread = new Thread(new Runnable() {
            public void run() {
                mDhcpResults.clear();
                if (!NetworkUtils.startPppoe(mInterfaceName, username, mDhcpResults)) {
                    Log.e(TAG, "startPppoe error: " + NetworkUtils.getPppoeError());
                    sendMessage(CMD_START_PPPOE_FAILURE);
                } else {
                    Log.d(TAG,"startPppoe Success\ndhcpResults = " + mDhcpResults.toString());
                    sendMessage(CMD_START_PPPOE_SUCCESS, new DhcpResults(mDhcpResults));
                }
            }
        });
        pppoeThread.start();
    }

    private void updateState() {
        lastState = NetworkUtils.pppoeStatus(mInterfaceName);
    }

    private static class MonitorThread extends Thread {
        private int newState = 0;
        private StateMachine mStateMachine = null;

        public MonitorThread(StateMachine stateMachine) {
            super("PppoeMonitor");
            mStateMachine = stateMachine;
        }

        public void run() {
            while(running) {
                newState = NetworkUtils.pppoeStatus(mInterfaceName);
                if(lastState != newState) {
                    lastState = newState;
                    /*
                     * keep same with pppoe_utils.c define
                     * #define PPPOE_STOPPED 0
                     * #define PPPOE_STARTING 1
                     * #define PPPOE_STARTED 2
                     * #define PPPOE_STOPPING 3
                     */
                    if (lastState == 0) {
                        mStateMachine.sendMessage(CMD_STATE_CHANGED, "PPPOE_STOPPED");
                    } else if (lastState == 1) {
                        mStateMachine.sendMessage(CMD_STATE_CHANGED, "PPPOE_STARTING");
                    } else if (lastState == 2) {
                        mStateMachine.sendMessage(CMD_STATE_CHANGED, "PPPOE_STARTED");
                    } else if (lastState == 3) {
                        mStateMachine.sendMessage(CMD_STATE_CHANGED, "PPPOE_STOPPING");
                    }
                }
                try {
                    Thread.sleep(100); //sleep 100ms
                } catch (Exception e) {
                    Log.e(TAG, "Thread error!");
                }
            }
        }
    }
}