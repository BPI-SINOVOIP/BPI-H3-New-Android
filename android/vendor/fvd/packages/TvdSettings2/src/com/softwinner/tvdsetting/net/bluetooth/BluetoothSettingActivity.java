package com.softwinner.tvdsetting.net.bluetooth;

import com.softwinner.tvdsetting.R;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.softwinner.tvdsetting.net.bluetooth.LocalBluetoothAdapter;
import com.softwinner.tvdsetting.net.bluetooth.LocalBluetoothManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.view.KeyEvent;
import android.bluetooth.BluetoothAdapter;
import android.util.Log;
import android.os.SystemProperties;
import android.content.pm.PackageManager;

public class BluetoothSettingActivity extends Activity implements ListView.OnItemClickListener, ListView.OnKeyListener  {
	Context mContext;
	ListView mListView;
	private final int START_BLUETOOTH = 0;
	private final int START_DIRECTION = 1;
	private final int SEARCH_DEVICES = 2;

    private boolean mEnablerState;
    private boolean mDiscoverable;
    private boolean isNewFeature = false;
    private LocalBluetoothAdapter mLocalAdapter;
    private BluetoothSettingAdapter mSettingAdapter;
    private final IntentFilter mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Constant.ACTION_USBBTDEV_CHANGE)) {
			    //处理
				if(isNewFeature&&(SystemProperties.getInt(Constant.BT_HAS_USB,1) == 1)
						&&(SystemProperties.getInt(Constant.BT_USB_INSERTED, 0) == 0)){
					finish();
				}
			}else {
	            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
	            handleStateChanged(state);
			}
        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		this.setContentView(R.layout.bluetoothsetting);
		isNewFeature = SystemProperties.getInt(Constant.BT_HAS_USB, 33)== 33?false:true;
        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(mContext);
        if (manager == null) {
            // Bluetooth is not supported
            mLocalAdapter = null;
        } else {
            mLocalAdapter = manager.getBluetoothAdapter();
        }
		mListView = (ListView)this.findViewById(R.id.bluetoothsetting);
		mSettingAdapter = new BluetoothSettingAdapter();
		mListView.setAdapter(mSettingAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnKeyListener(this);
	}

    @Override
    public void onResume() {
        super.onResume();
        if (mLocalAdapter == null) {
            return;
        }
//        handleStateChanged(mLocalAdapter.getBluetoothState());
        if(mIntentFilter != null)
        	mIntentFilter.addAction(Constant.ACTION_USBBTDEV_CHANGE);
        mContext.registerReceiver(mReceiver, mIntentFilter);
		if(isNewFeature&&(SystemProperties.getInt(Constant.BT_HAS_USB,1) == 1)
				&&(SystemProperties.getInt(Constant.BT_USB_INSERTED, 0) == 0)) {
		    finish();
		}
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLocalAdapter == null) {
            return;
        }
        mContext.unregisterReceiver(mReceiver);
    }

	@Override
	public boolean onKey(View view, int keyCode, KeyEvent key) {
		// TODO Auto-generated method stub
		int index = mListView.getSelectedItemPosition();
		if(index == START_BLUETOOTH){
		    TextView enablerState = (TextView)mListView.getChildAt(START_BLUETOOTH).findViewById(R.id.state);
			if(key.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT){
				if(key.getAction() == KeyEvent.ACTION_DOWN){
					if(!mEnablerState){
                        if (mLocalAdapter != null) {
                            mLocalAdapter.setBluetoothEnabled(true);
                        }
						enablerState.setText(R.string.opening);
					} else {
						if (mLocalAdapter != null) {
                            mLocalAdapter.setBluetoothEnabled(false);
                        }
						enablerState.setText(R.string.closing);
					}
			}else if(key.getAction()==KeyEvent.ACTION_UP){

			}
			}else if(key.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT){

				if(key.getAction() == KeyEvent.ACTION_DOWN){
					if(mEnablerState){
                        if (mLocalAdapter != null) {
                            mLocalAdapter.setBluetoothEnabled(false);
                        }
						enablerState.setText(R.string.closing);
					} else {
						if (mLocalAdapter != null) {
                            mLocalAdapter.setBluetoothEnabled(true);
                        }
						enablerState.setText(R.string.opening);
					}
				}else if(key.getAction() == KeyEvent.ACTION_UP){

				}
			}
		} else if(index == START_DIRECTION) {
		    TextView discoverableState = (TextView)mListView.getChildAt(START_DIRECTION).findViewById(R.id.state);
			if(key.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT){
				if(key.getAction() == KeyEvent.ACTION_DOWN){
					if(!mDiscoverable){
                        if (mLocalAdapter != null) {
                            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
                        }
                        mDiscoverable = true;
						discoverableState.setText(R.string.open);
					} else {
						if (mLocalAdapter != null) {
                            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
                        }
                        mDiscoverable = false;
						discoverableState.setText(R.string.close);
					}
			}else if(key.getAction()==KeyEvent.ACTION_UP){

			}
			}else if(key.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT){

				if(key.getAction() == KeyEvent.ACTION_DOWN){
					if(mDiscoverable){
                        if (mLocalAdapter != null) {
                            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
                        }
                        mDiscoverable = false;
						discoverableState.setText(R.string.close);
					} else {
						if (mLocalAdapter != null) {
                            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
                        }
                        mDiscoverable = true;
						discoverableState.setText(R.string.open);
					}
				}else if(key.getAction() == KeyEvent.ACTION_UP){

				}
			}
		}
		return false;
	}

	class BluetoothSettingAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			if(mLocalAdapter != null ){
				if(mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_OFF){
					return 1;
				}else{
					return 3;
				}
			}else {
				return 1;
			}
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			convertView=LayoutInflater.from(mContext).inflate(R.layout.bluetoothsettingitem, null);
			TextView text1 = (TextView)convertView.findViewById(R.id.text1);
			TextView state = (TextView)convertView.findViewById(R.id.state);
			LinearLayout ll = (LinearLayout)convertView.findViewById(R.id.rightitem);
			if(position == START_BLUETOOTH){
				text1.setText(R.string.start_bt);
				if(mLocalAdapter != null && mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
				    mEnablerState = true;
				    state.setText(R.string.open);
				} else {
				    mEnablerState = false;
				    state.setText(R.string.close);
			    }
			}else if(position == START_DIRECTION){
				text1.setText(R.string.start_direction);
				if(mLocalAdapter != null && mLocalAdapter.getScanMode() ==
				            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
				    mDiscoverable = true;
				    state.setText(R.string.open);
				} else {
				    mDiscoverable = false;
				    state.setText(R.string.close);
			    }
			}else if(position == SEARCH_DEVICES){
				text1.setText(R.string.search_device);
				//state.setText(R.string.close);
				ll.setVisibility(View.INVISIBLE);
			}
			return convertView;
		}

	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_bluetooth);
			break;
		}
		return false;
	}

	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_bluetooth);
				finish();
			break;
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		switch(position){
		case START_BLUETOOTH:
			break;
		case START_DIRECTION:
			break;
		case SEARCH_DEVICES:
			Intent intent = new Intent();
			intent.setClass(BluetoothSettingActivity.this, BluetoothListActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
	}

    void handleStateChanged(int state) {
        TextView enablerState = (TextView)mListView.getChildAt(START_BLUETOOTH).findViewById(R.id.state);
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                mEnablerState = true;
                enablerState.setText(R.string.open);
        		if(mSettingAdapter != null){
        			mSettingAdapter.notifyDataSetChanged();
        		}
				if (mLocalAdapter != null) {
                    mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
                }
                break;
            case BluetoothAdapter.STATE_OFF:
                mEnablerState = false;
                enablerState.setText(R.string.close);
        		if(mSettingAdapter != null){
        			mSettingAdapter.notifyDataSetChanged();
        		}
                break;
            default:
                break;
        }
    }
}
