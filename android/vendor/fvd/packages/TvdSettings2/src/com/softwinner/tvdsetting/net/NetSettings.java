package com.softwinner.tvdsetting.net;

import java.util.ArrayList;
import java.util.List;

import com.softwinner.tvdsetting.R;
import com.softwinner.tvdsetting.Settings;
import com.softwinner.tvdsetting.applications.AppManagerActivity;
import com.softwinner.tvdsetting.net.bluetooth.BluetoothListActivity;
import com.softwinner.tvdsetting.net.bluetooth.BluetoothSettingActivity;
import com.softwinner.tvdsetting.net.ethernet.EthernetSettingActivity;
import com.softwinner.tvdsetting.net.wifi.SoftapActivity;
import com.softwinner.tvdsetting.net.wifi.WifiSettingActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.os.SystemProperties;
import android.content.IntentFilter;
import com.softwinner.tvdsetting.net.bluetooth.Constant;

public class NetSettings extends Activity implements ListView.OnItemClickListener,ListView.OnItemSelectedListener{

	private static final int NETSETTING_BASE = 0;
	private static final int NETSETTING_WIRELESS = NETSETTING_BASE + 0;
	private static final int NETSETTING_ETHERNET = NETSETTING_BASE + 1;
	private static final int NETSETTING_SOTFAP = NETSETTING_BASE + 2;
	private static final int NETSETTING_BLUETOOTH = NETSETTING_BASE +3;
	private static final int NETSETTING_NETINFO = NETSETTING_BASE + 4;
	private boolean isNewFeature = false;
	private static final String TAG = "NetSettings";

	ListView mListView;
	Context mContext;
	List<ImageView> mImageList;

	private NetSettingItemAdapter mAdapter = null;


	private BroadcastReceiver mUSBBTReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG,"getItem action = " +  action);
			if (action.equals(Constant.ACTION_USBBTDEV_CHANGE)) {
			    //处理
				if(mAdapter != null){
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.netsetting);
		mContext = this;
		isNewFeature = SystemProperties.getInt(Constant.BT_HAS_USB, 33)== 33?false:true;
		mListView = (ListView)this.findViewById(R.id.netitemlist);
		mAdapter = new NetSettingItemAdapter();
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemSelectedListener(this);
		mImageList = new ArrayList();
	}


	@Override
	protected void onResume() {
	// TODO Auto-generated method stub

		IntentFilter filter = new IntentFilter();
		filter.addAction(Constant.ACTION_USBBTDEV_CHANGE);
		//注册广播
		registerReceiver(mUSBBTReceiver, filter);
		if(mAdapter != null){
			mAdapter.notifyDataSetChanged();
		}

		super.onResume();
	}

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUSBBTReceiver);
    }

	class NetSettingItemAdapter extends BaseAdapter{


		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			if(!isNewFeature){
				if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
					return NETSETTING_NETINFO + 1;
				}else {
					return NETSETTING_NETINFO;
				}
			}else{
				if(SystemProperties.getInt(Constant.BT_HAS_USB,1) == 0) {
				    return NETSETTING_NETINFO + 1;
				}else if(SystemProperties.getInt(Constant.BT_USB_INSERTED, 0) == 1) {
				    return NETSETTING_NETINFO + 1;
				} else {
				    return NETSETTING_NETINFO;
				}
			}
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			Log.d(TAG,"getItem arg0 = " +  arg0);
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			Log.d(TAG,"getItemId arg0 = " +  arg0);
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			convertView=LayoutInflater.from(mContext).inflate(R.layout.netsettingitem, null);
			TextView text = (TextView)convertView.findViewById(R.id.netsettingitemtext);
			ImageView image = (ImageView)convertView.findViewById(R.id.netitemicon);
			mImageList.add(image);
			switch(position){
			case NETSETTING_WIRELESS:
				text.setText(R.string.wireless);
				image.setImageResource(R.drawable.ic_wireless_working);
				break;
			case NETSETTING_ETHERNET:
				text.setText(R.string.ethernet);
				image.setImageResource(R.drawable.ic_ethernet_working);
				break;
			case NETSETTING_SOTFAP:
				text.setText(R.string.softap);
				image.setImageResource(R.drawable.ic_hotspot_working);
				break;
			case NETSETTING_BLUETOOTH:
				if(!isNewFeature){
					if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
						text.setText(R.string.bluetooth);
						image.setImageResource(R.drawable.ic_bluetooth_working);
						break;
					}
				}else{
					if(SystemProperties.getInt(Constant.BT_HAS_USB,1) == 0) {
						text.setText(R.string.bluetooth);
						image.setImageResource(R.drawable.ic_bluetooth_working);
						break;
					}else if(SystemProperties.getInt(Constant.BT_USB_INSERTED, 0) == 1){
						text.setText(R.string.bluetooth);
						image.setImageResource(R.drawable.ic_bluetooth_working);
						break;
					}
				}
			case NETSETTING_NETINFO:
				text.setText(R.string.netinfo);
				break;
			default:
				break;
			}
			image.setVisibility(View.INVISIBLE);
			return convertView;
		}


	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id ) {
		// TODO Auto-generated method stub
		Log.d(TAG,"position = " + position);
		Intent intent = new Intent();
		switch(position){
		case NETSETTING_WIRELESS:
			//intent.putExtra(WifiSettingActivity.LAUNCHER_FIRST_START_FLAG, true);
			intent.setClass(NetSettings.this, WifiSettingActivity.class);
			startActivity(intent);
			break;
		case NETSETTING_ETHERNET:
			intent.setClass(NetSettings.this, EthernetSettingActivity.class);
			startActivity(intent);
			break;
		case NETSETTING_SOTFAP:
			intent.setClass(NetSettings.this, SoftapActivity.class);
			startActivity(intent);
			break;
		case NETSETTING_BLUETOOTH:
			if(!isNewFeature){
				if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
					intent.setClass(NetSettings.this, BluetoothSettingActivity.class);
					startActivity(intent);
					break;
				}
			}else{
				if(SystemProperties.getInt(Constant.BT_HAS_USB,1) == 0) {
					intent.setClass(NetSettings.this, BluetoothSettingActivity.class);
					startActivity(intent);
					break;
				}else if(SystemProperties.getInt(Constant.BT_USB_INSERTED, 0) == 1){
					intent.setClass(NetSettings.this, BluetoothSettingActivity.class);
					startActivity(intent);
					break;
				}
			}
		case NETSETTING_NETINFO:
			intent.setClass(NetSettings.this, NetStatsActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		for(int i=0;i<mImageList.size();i++){
			mImageList.get(i).setVisibility(View.INVISIBLE);
		}
		mImageList.get(position).setVisibility(View.VISIBLE);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_network_setting);
			break;
		}
		return false;
	}

	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_network_setting);
				finish();
			break;
		}
		return false;
	}
}
