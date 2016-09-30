package android.net.ethernet;

import android.os.UserHandle;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.ethernet.EthernetManager;
import android.net.pppoe.PppoeManager;
import android.util.Log;

public class BroadcastTrans {
    private static final String TAG = "BroadcastTrans";
    private EthernetManager mEthManager;
    private final IntentFilter mFilter;
    private final BroadcastReceiver mEthStateReceiver;
    private Context mContext = null;

    public BroadcastTrans(Context context, EthernetManager eth) {
        mContext = context;
        mEthManager = eth;
        mFilter = new IntentFilter();
        mFilter.addAction(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
        mFilter.addAction(EthernetManager.NETWORK_STATE_CHANGED_ACTION);
        Log.d(TAG,"BroadcastTrans init");
        mEthStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };
        mContext.registerReceiver(mEthStateReceiver, mFilter);
    }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        String mode = mEthManager.getEthernetMode();
        if (EthernetManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            final NetworkInfo networkInfo = (NetworkInfo)
                    intent.getParcelableExtra(EthernetManager.EXTRA_NETWORK_INFO);
            final LinkProperties linkProperties = (LinkProperties)
                    intent.getParcelableExtra(EthernetManager.EXTRA_LINK_PROPERTIES);
            final int event = intent.getIntExtra(EthernetManager.EXTRA_ETHERNET_STATE,
                    EthernetManager.EVENT_ETHERNET_CONNECT_SUCCESSED);
            Log.d(TAG,"handleEvent eth event = " + event);
            switch(event) {
                case EthernetManager.EVENT_ETHERNET_CONNECT_SUCCESSED:
                    if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)) {
                        sendStateBroadcast(networkInfo, linkProperties,
                                EthernetManager.EVENT_DHCP_CONNECT_SUCCESSED);
                    } else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
                        sendStateBroadcast(networkInfo, linkProperties,
                                EthernetManager.EVENT_STATIC_CONNECT_SUCCESSED);
                    }
                    break;
                case EthernetManager.EVENT_ETHERNET_CONNECT_FAILED:
                    if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)) {
                        sendStateBroadcast(networkInfo, linkProperties,
                                EthernetManager.EVENT_DHCP_CONNECT_FAILED);
                    } else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
                        sendStateBroadcast(networkInfo, linkProperties,
                                EthernetManager.EVENT_STATIC_CONNECT_FAILED);
                    }
                    break;
                case EthernetManager.EVENT_ETHERNET_DISCONNECT_FAILED:
                    if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)) {
                        sendStateBroadcast(networkInfo, linkProperties,
                                EthernetManager.EVENT_DHCP_DISCONNECT_FAILED);
                    } else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
                        sendStateBroadcast(networkInfo, linkProperties,
                                EthernetManager.EVENT_STATIC_DISCONNECT_FAILED);
                    }
                    break;
                case EthernetManager.EVENT_ETHERNET_DISCONNECT_SUCCESSED:
                    if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)) {
                        sendStateBroadcast(networkInfo, linkProperties,
                                EthernetManager.EVENT_DHCP_DISCONNECT_SUCCESSED);
                    } else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
                        sendStateBroadcast(networkInfo, linkProperties,
                                EthernetManager.EVENT_STATIC_DISCONNECT_SUCCESSED);
                    }
                    break;

                case EthernetManager.EVENT_PPPOE_CONNECT_SUCCESSED:
                    if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)) {
                        notifyPppoeConnected();
                    }
                    break;
                case EthernetManager.EVENT_PPPOE_CONNECT_FAILED:
                    if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)) {
                        notifyPppoeDisconnected();
                    }
                    break;
                case EthernetManager.EVENT_PPPOE_DISCONNECT_SUCCESSED:
                    if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)) {
                        notifyPppoeDisconnected();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void sendStateBroadcast(NetworkInfo mNetworkInfo, LinkProperties mLinkProperties, int event) {
        Log.d(TAG,"sendStateBroadcast event = "+ event);
        Intent intent = new Intent(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                | Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(EthernetManager.EXTRA_NETWORK_INFO, mNetworkInfo);
        intent.putExtra(EthernetManager.EXTRA_LINK_PROPERTIES,
                new LinkProperties (mLinkProperties));
        intent.putExtra(EthernetManager.EXTRA_ETHERNET_STATE, event);
        mContext.sendBroadcastAsUser(intent,UserHandle.ALL);
    }

    private void notifyPppoeDisconnected() {
        Log.d(TAG, "notifyDisconnected");
        try {
            Intent intent = new Intent();
            intent.setAction(PppoeManager.PPPOE_STATE_CHANGED_ACTION);
            intent.putExtra(PppoeManager.PPPOE_STATE_STATUE, PppoeManager.PPPOE_STATE_CONNECT);
            intent.putExtra(PppoeManager.EXTRA_PPPOE_STATE, 1);
            mContext.sendBroadcastAsUser(intent,UserHandle.ALL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyPppoeConnected() {
        Log.d(TAG, "notifyConnected");
        try {
            Intent intent = new Intent();
            intent.setAction(PppoeManager.PPPOE_STATE_CHANGED_ACTION);
            intent.putExtra(PppoeManager.PPPOE_STATE_STATUE, PppoeManager.PPPOE_STATE_DISCONNECT);
            intent.putExtra(PppoeManager.EXTRA_PPPOE_STATE, 0);
            mContext.sendBroadcastAsUser(intent,UserHandle.ALL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
