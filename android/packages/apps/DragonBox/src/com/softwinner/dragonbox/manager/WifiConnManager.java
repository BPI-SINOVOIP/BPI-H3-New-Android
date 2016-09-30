package com.softwinner.dragonbox.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import android.app.Activity;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.IpAssignment;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.ProxySettings;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.softwinner.dragonbox.entity.WifiConnConfigInfo;
import com.softwinner.dragonbox.entity.WifiConnInfo;
import com.softwinner.dragonbox.utils.WifiUtil;

public class WifiConnManager {
	public static final String TAG = "WifiConnManager";
	private static final int MAX_SIZE = 3;
	private Context mContext;
	private WifiConnInfo mConnInfo;
	private IntentFilter mFilter;

	private WifiManager mWifiManager;

	private List<WifiConnInfo> mWifiConnInfos = new ArrayList<WifiConnInfo>();
	private boolean mIsScaning;
	protected OnWifiConnChangeListener mWifiConnChangeListener;
	public WifiConnManager(Context context, WifiConnInfo connInfo) {
		mContext = context;
		mConnInfo = connInfo;
		mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
	}

	private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			NetworkInfo networkInfo = (NetworkInfo) intent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			Log.d(TAG, "mWifiReceiver action=" + action);
			if (WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION
							.equals(action)
					|| WifiManager.LINK_CONFIGURATION_CHANGED_ACTION
							.equals(action)
					|| WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				List<WifiConnInfo> connInfos = updateWifiConnInfos();
				if (mWifiConnChangeListener != null) {
					mWifiConnChangeListener.onSearching(connInfos);
				}
				
			} else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION
					.equals(action)) {
			} else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
				int wifiState = intent.getIntExtra(
						WifiManager.EXTRA_WIFI_STATE, 0);
				Log.d(TAG, "mWifiReceiver wifiState=" + wifiState);
				if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
					mScanner.resume();
				}
			}
		}
	};

	private Scanner mScanner = new Scanner();

	/* wifi ap 扫描器 */
	private class Scanner extends Handler {
		private int mRetry = 0;

		void resume() {
			if (!hasMessages(0)) {
				sendEmptyMessage(0);
			}
		}

		void forceScan() {
			removeMessages(0);
			sendEmptyMessage(0);
		}

		void pause() {
			mRetry = 0;
			removeMessages(0);
		}

		@Override
		public void handleMessage(Message message) {
			Log.d(TAG, "Scanner");
			if (mWifiManager.startScan()) {
				mRetry = 0;
			} else if (++mRetry >= 3) {
				mRetry = 0;
				return;
			}
			sendEmptyMessageDelayed(0, 500);
		}
	}

	public void startWifiTest() {
		mFilter = new IntentFilter();
		mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
		mFilter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
		mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

		mContext.registerReceiver(mWifiReceiver, mFilter);
		mWifiConnInfos.clear();
		mWifiManager.setWifiApEnabled(null, false);
		mWifiManager.setWifiEnabled(true);
		if (!mWifiManager.isWifiEnabled()) {
		} else {
			mScanner.resume();
		}
		mIsScaning = true;
		startConnWifi();
	}

	private void startConnWifi() {
		Thread thread = new Thread() {
			List<WifiInfo> wifiInfos = new LinkedList<WifiInfo>();
			@Override
			public void run() {
				while (mIsScaning) {
					
					int networkId = mWifiManager.addNetwork(getConfig(mConnInfo));
					mWifiManager.enableNetwork(networkId, true);
					WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
					//int rss = wifiInfo.getRssi();
					//String ssidstr = wifiInfo.getSSID();
					//ssidstr = ssidstr.substring(1, ssidstr.length() - 1);
					int ipAdd=wifiInfo.getIpAddress();
					wifiInfos.add(wifiInfo);
					while (wifiInfos.size() > MAX_SIZE) {
						wifiInfos.remove(0);
					}
					if (mWifiConnChangeListener != null) {
						((Activity)mContext).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mWifiConnChangeListener.onConnWifi(wifiInfos, mConnInfo);
							}
						});
					}
					
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();
	}
	
	private WifiConfiguration getConfig(WifiConnInfo connInfo) {
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = "\"" + connInfo.SSID + "\"";
		// config.SSID = wifiSSIDStr;
		String password = connInfo.mWifiPWD;
		switch (connInfo.security) {
		case WifiUtil.SECURITY_NONE:
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			break;

		case WifiUtil.SECURITY_WEP:
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
			// String password = mWifiDialog.getPassword();

			int length = password.length();
			// WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
			if ((length == 10 || length == 26 || length == 58)
					&& password.matches("[0-9A-Fa-f]*")) {
				config.wepKeys[0] = password;
			} else {
				config.wepKeys[0] = '"' + password + '"';
			}
			break;

		case WifiUtil.SECURITY_PSK:
			config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
			password = connInfo.mWifiPWD;
			length = password.length();
			if (password.matches("[0-9A-Fa-f]{64}")) {
				config.preSharedKey = password;
			} else {
				config.preSharedKey = '"' + password + '"';
			}
			break;

		default:
			return null;
		}
		config.proxySettings = ProxySettings.UNASSIGNED;
		config.ipAssignment = IpAssignment.UNASSIGNED;
		config.linkProperties = new LinkProperties();
		
		return config;
	}

	private List<WifiConnInfo> updateWifiConnInfos() {

		final List<ScanResult> results = mWifiManager.getScanResults();
		if (results != null) {
			for (ScanResult result : results) {

				if (result.SSID == null || result.SSID.length() == 0
						|| result.capabilities.contains("[IBSS]")) {
					continue;
				}
				WifiConnInfo wifiConnInfo = null;
				for (WifiConnInfo info : mWifiConnInfos) {
					if (info.SSID.equals(result.SSID)) {
						wifiConnInfo = info;
						break;
					}
				}

				if (wifiConnInfo == null) {
					if (mConnInfo.SSID.equals(result.SSID)) {
						wifiConnInfo = mConnInfo;
					} else {
						wifiConnInfo = new WifiConnInfo();
					}
					mWifiConnInfos.add(wifiConnInfo);
				}
				wifiConnInfo.updateWifiConnInfo(result);
			}
		}
		Collections.sort(mWifiConnInfos);
		return mWifiConnInfos;
	}

	public void stopScanWifi() {
		if (mIsScaning) {
			mContext.unregisterReceiver(mWifiReceiver);
			mScanner.pause();
			mWifiManager.setWifiEnabled(false);
			if (mWifiConnChangeListener != null) {
				mWifiConnChangeListener.onSearchEnd();
			}
		}
		mIsScaning = false;
	}
	
	public WifiConnInfo getWifiConnInfo(){
		return mConnInfo;
	}
	
	public void setOnWifiConnChangeListener(OnWifiConnChangeListener listener){
		this.mWifiConnChangeListener = listener;
	}

	public interface OnWifiConnChangeListener {
		public void onWifiStateChange(int state);
		public void onSearching(List<WifiConnInfo> wifiConnInfos);
		public void onConnWifi(List<WifiInfo> wifiInfos, WifiConnInfo connInfo);
		public void onSearchEnd();
	}

}
