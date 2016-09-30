package com.softwinner.dragonbox.testcase;

import java.util.List;
import java.util.Stack;

import org.xmlpull.v1.XmlPullParser;

import android.app.AlertDialog;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.entity.WifiConnInfo;
import com.softwinner.dragonbox.manager.WifiConnManager;
import com.softwinner.dragonbox.manager.WifiConnManager.OnWifiConnChangeListener;
import com.softwinner.dragonbox.utils.WifiUtil;
import com.softwinner.dragonbox.view.WifiConnListAdapter;
import com.softwinner.dragonbox.view.WifiScanListAdapter;

public class CaseWifi extends IBaseCase implements OnWifiConnChangeListener {
	private static final String TAG = "CaseWifi";
	private WifiConnManager mWifiConnManager;

	private WifiManager mWifiManager;

	private ListView mMaxScanResultLV;
	private ListView mMaxConnResultLV;
	private WifiScanListAdapter mScanListAdapter;
	private WifiConnListAdapter mConnListAdapter;

	private TextView mMinWifiStatusTV;
	private TextView mMinConnNameTV;
	private TextView mMinIpAddrTV;
	private TextView mMinConnStrengthTV;

	public CaseWifi(Context context) {
		super(context, R.string.case_wifi_name, R.layout.case_wifi_max,
				R.layout.case_wifi_min, TYPE_MODE_AUTO);
		mWifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		mMaxScanResultLV = (ListView) mMaxView
				.findViewById(R.id.case_wifi_scan_list);
		mMaxConnResultLV = (ListView) mMaxView
				.findViewById(R.id.case_wifi_conn_list);

		mMinWifiStatusTV = (TextView) mMinView
				.findViewById(R.id.case_wifi_status_text);
		mMinConnNameTV = (TextView) mMinView
				.findViewById(R.id.case_wifi_conn_name);
		mMinIpAddrTV = (TextView) mMinView.findViewById(R.id.case_wifi_ip_addr);
		mMinConnStrengthTV = (TextView) mMinView
				.findViewById(R.id.case_wifi_conn_strength);

		mScanListAdapter = new WifiScanListAdapter(mContext);
		mConnListAdapter = new WifiConnListAdapter(mContext);
		mMaxScanResultLV.setAdapter(mScanListAdapter);
		mMaxConnResultLV.setAdapter(mConnListAdapter);
	}

	public CaseWifi(Context context, XmlPullParser xmlParser) {
		this(context);
		WifiConnInfo info = new WifiConnInfo();
		info.mMaxRSSI = Integer.parseInt(xmlParser.getAttributeValue(null,
				"maxRSSI"));
		info.SSID = xmlParser.getAttributeValue(null, "wifiSSID");
		info.mWifiPWD = xmlParser.getAttributeValue(null, "wifiPWD");
		info.isConfig = true;
		mWifiConnManager = new WifiConnManager(context, info);
		mWifiConnManager.setOnWifiConnChangeListener(this);

	}

	@Override
	public void onStartCase() {
		mWifiConnManager.startWifiTest();
		setDialogPositiveButtonEnable(false);
	}

	@Override
	public void onStopCase() {
		mWifiConnManager.stopScanWifi();
	}

	@Override
	public void onWifiStateChange(int state) {

	}

	@Override
	public void onSearching(List<WifiConnInfo> wifiConnInfos) {
		mScanListAdapter.setWifiConnInfos(wifiConnInfos);
		mMinWifiStatusTV.setText(R.string.case_wifi_status_searching);
	}

	@Override
	public void onSearchEnd() {
		mMinWifiStatusTV.setText(R.string.case_wifi_status_search_end);
	}

	@Override
	public void onConnWifi(List<WifiInfo> wifiInfos, WifiConnInfo connInfo) {
		// Log.d(TAG, " rss= wifiInfos.size()=" + wifiInfos.size());
		boolean result = true;
		for (WifiInfo wifiinfo : wifiInfos) {
			if (wifiinfo == null || wifiinfo.getSSID() == null
					|| !wifiinfo.getSSID().contains(connInfo.SSID)
					|| wifiinfo.getIpAddress() == 0 || wifiInfos.size() < 3
					|| wifiinfo.getRssi() <= connInfo.mMaxRSSI) {
				result = false;
				break;
			}
		}
		mMinConnNameTV.setText(mContext.getString(R.string.case_wifi_conn_name)
				+ "[" + wifiInfos.get(0).getSSID() + "]");
		mMinIpAddrTV
				.setText(mContext.getString(R.string.case_wifi_ip_addr) + "["
						+ WifiUtil.intToIp(wifiInfos.get(0).getIpAddress())
						+ "]");
		mMinConnStrengthTV.setText(mContext
				.getString(R.string.case_wifi_conn_strength)
				+ "["
				+ wifiInfos.get(0).getRssi() + "DB]");
		if (result) {
			stopCase();
		}

		setDialogPositiveButtonEnable(result);
		setCaseResult(result);
		mConnListAdapter.setWifiConnInfos(wifiInfos);
	}

	@Override
	public void reset() {
		super.reset();
		mMinWifiStatusTV.setText(R.string.case_wifi_status_text);
		mMinConnNameTV.setText(R.string.case_wifi_conn_name);
		mMinIpAddrTV.setText(R.string.case_wifi_ip_addr);
		mMinConnStrengthTV.setText(R.string.case_wifi_conn_strength);
	}
}
