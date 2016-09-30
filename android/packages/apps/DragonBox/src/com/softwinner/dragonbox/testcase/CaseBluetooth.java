package com.softwinner.dragonbox.testcase;

import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.entity.BluetoothDeviceInfo;
import com.softwinner.dragonbox.manager.BluetoothManager;
import com.softwinner.dragonbox.manager.BluetoothManager.OnBluetoothSearchListener;

public class CaseBluetooth extends IBaseCase implements OnBluetoothSearchListener{

	private BluetoothViewAdapter bluetoothAdapter;

	private TextView mBluetoothSearchStatusTV;
	private ListView mBluetoothSearchResultLV;
	
	private BluetoothManager mBluetoothManager;
	public CaseBluetooth(Context context) {
		super(context, R.string.case_bluetooth_name, R.layout.case_bluetooth_max,
				R.layout.case_bluetooth_min, TYPE_MODE_AUTO);
		mBluetoothManager = new BluetoothManager(context);
		
		mBluetoothSearchStatusTV = (TextView) mMaxView.findViewById(R.id.case_bluetooth_status);
		mBluetoothSearchResultLV = (ListView) mMaxView.findViewById(R.id.case_bluetooth_result_list);
		bluetoothAdapter = new BluetoothViewAdapter();
		mBluetoothSearchResultLV.setAdapter(bluetoothAdapter);
		mBluetoothManager.setOnBluetoothSearchListener(this);
	}

	public CaseBluetooth(Context context, XmlPullParser xmlParser) {
		this(context);
	}
	
	@Override
	public void onStartCase() {
		mBluetoothManager.statDiscovery();

	}

	@Override
	public void onStopCase() {
		

	}

	@Override
	public void onSearchStart(List<BluetoothDeviceInfo> deviceList) {
		bluetoothAdapter.setDeviceList(deviceList);
		mBluetoothSearchStatusTV.setText(R.string.case_bluetooth_status_seach_start);
	}

	@Override
	public void onSearching(List<BluetoothDeviceInfo> deviceList) {
		bluetoothAdapter.setDeviceList(deviceList);
		mBluetoothSearchStatusTV.setText(R.string.case_bluetooth_status_seaching);
	}

	@Override
	public void onSearchEnd(List<BluetoothDeviceInfo> deviceList) {
		bluetoothAdapter.setDeviceList(deviceList);
		mBluetoothSearchStatusTV.setText(R.string.case_bluetooth_status_seach_end);
		if (deviceList.size() == 0) {
			mBluetoothManager.statDiscovery();
		} else {
			setCaseResult(true);
		}
	}
	
	
	private class BluetoothViewAdapter extends BaseAdapter {
		private List<BluetoothDeviceInfo> mDeviceList = new ArrayList<BluetoothDeviceInfo>();
		private class ViewHolder {
			TextView bluetoothName;
			TextView strength;
		}

		public void setDeviceList(List<BluetoothDeviceInfo> deviceList){
			this.mDeviceList = deviceList;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mDeviceList.size();
		}

		@Override
		public Object getItem(int position) {
			
			return mDeviceList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convert, ViewGroup root) {
			if (convert == null) {
				convert = LayoutInflater.from(mContext).inflate(R.layout.case_bluetooth_item, null);
				ViewHolder holder = new ViewHolder();
				holder.bluetoothName = (TextView) convert.findViewById(R.id.case_bluetooth_name);
				holder.strength = (TextView) convert
						.findViewById(R.id.case_bluetooth_strength);
				convert.setTag(holder);
			}
			ViewHolder holder = (ViewHolder) convert.getTag();
			holder.bluetoothName.setText(mDeviceList.get(position).getmBuletoothDevice().getName());
			short rssi = mDeviceList.get(position).getRSSI();
			if (rssi != 0) {
				holder.strength.setText(rssi + "");
			} else {
				holder.strength.setText("已配对");
			}
			
			return convert;
		}
	}


	@Override
	public void reset() {
		super.reset();
		
	}

}
