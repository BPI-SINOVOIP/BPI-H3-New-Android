package com.softwinner.tvdsetting.net.wifi;

import com.softwinner.tvdsetting.R;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.net.wifi.WifiConfiguration;
import android.content.DialogInterface;
import android.view.View;
import android.util.Log;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.provider.Settings;

public class SoftapActivity extends Activity implements ListView.OnItemClickListener, ListView.OnKeyListener {
	Context mContext;
	ListView mListView;
	SoftapListAdapter mSoftapListAdapter;
	private int START_SOFTAP = 0;
	private int SET_SOFTAP = 1;
	private boolean mState;
	Drawable mLeftNor,mLeftPress,mRightNor,mRightPress;
	TextView text1;
	TextView text2;
	TextView smalltext;

	private WifiManager mWifiManager;
	private WifiConfiguration mWifiConfig = null;

	private static final int SOFTAP_SUBTEXT = R.string.softap_subtext;
	protected static final int SET_TEXT_OPEN = 0;
	protected static final int SET_TEXT_CLOSE = 1;
	private String[] mSecurityType = {"","WPA PSK", "WPA2 PSK"};
	boolean withStaEnabled;

	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			TextView text1 = null;
			/*if(mListView!=null){
				View item = mListView.getChildAt(START_SOFTAP);
				if(item!=null){
					text1 = (TextView)item.findViewById(R.id.text2);
				}
			}*/
			if(null == mListView.getSelectedView()) {
				return;
			}
			text1 = (TextView)mListView.getSelectedView().findViewById(R.id.text2);
			switch(msg.what){
			case SET_TEXT_OPEN:
				if(mSoftapListAdapter!=null){
					mSoftapListAdapter.setSoftApStat(true);
					mSoftapListAdapter.notifyDataSetChanged();
				}
				//text1.setText(R.string.open);
				break;
			case SET_TEXT_CLOSE:
				if(mSoftapListAdapter!=null){
					mSoftapListAdapter.setSoftApStat(false);
					mSoftapListAdapter.notifyDataSetChanged();
				}
				//text1.setText(R.string.close);
				break;
			default:
				break;
			}
		}
	};
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiApStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_DISABLED));
            }
        }
    };
    private final IntentFilter mIntentFilter = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		this.setContentView(R.layout.softapactivity);
		mListView = (ListView)this.findViewById(R.id.softapactivity);
		mSoftapListAdapter = new SoftapListAdapter();
		mListView.setAdapter(mSoftapListAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnKeyListener(this);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mWifiConfig = mWifiManager.getWifiApConfiguration();
	}

	@Override
    protected void onResume() {
        super.onResume();
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mContext.unregisterReceiver(mReceiver);
    }

	class SoftapListAdapter extends BaseAdapter{

		private boolean softapstat ;
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 2;
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

		public void setSoftApStat(boolean stat){
			softapstat = stat;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			convertView=LayoutInflater.from(mContext).inflate(R.layout.softaplistitem, null);
			text1 = (TextView)convertView.findViewById(R.id.text1);
			text2 = (TextView)convertView.findViewById(R.id.text2);
			smalltext = (TextView)convertView.findViewById(R.id.smalltext);
			LinearLayout ll = (LinearLayout)convertView.findViewById(R.id.rightitem);
			if(position == START_SOFTAP){
				text1.setText(R.string.start_softap);
				if(softapstat)
					text2.setText(R.string.open);
				else
					text2.setText(R.string.close);
				smalltext.setVisibility(View.GONE);
			}else if(position == SET_SOFTAP){
				text1.setText(R.string.set_softap);
				if(mWifiConfig != null) {
				    smalltext.setText(String.format(getString(SOFTAP_SUBTEXT),
                            mWifiConfig.SSID,
                            mSecurityType[SoftapSettingActivity.getSecurityTypeIndex(mWifiConfig)]));
                } else {
                    smalltext.setText(R.string.softap_subtext);
                }
				ll.setVisibility(View.INVISIBLE);
			}
			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id ) {
		// TODO Auto-generated method stub
		if(position == START_SOFTAP){

		}else if(position == SET_SOFTAP){
			Intent intent = new Intent();
			intent.setClass(SoftapActivity.this, SoftapSettingActivity.class);
			startActivityForResult(intent, 0);
		}
	}

	@Override
	public boolean onKey(View view, int keyCode, KeyEvent key) {
		// TODO Auto-generated method stub

		int index = mListView.getSelectedItemPosition();
		if(index<0 || index > mSoftapListAdapter.getCount() || null == mListView.getSelectedView() ) {
			return false;
		}
		TextView text2 = (TextView)mListView.getSelectedView().findViewById(R.id.text2);
		if(index==START_SOFTAP){
			if(key.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT){
				if(key.getAction()==KeyEvent.ACTION_DOWN){
					if(!mState){
						setSoftapEnabled(true);
						text2.setText(R.string.open);
					}else{
						setSoftapEnabled(false);
						text2.setText(R.string.close);
					}
			}else if(key.getAction()==KeyEvent.ACTION_UP){

			}
			}else if(key.getKeyCode()==KeyEvent.KEYCODE_DPAD_RIGHT){

				if(key.getAction()==KeyEvent.ACTION_DOWN){
					if(mState){
						setSoftapEnabled(false);
						text2.setText(R.string.close);
					}
					else{
						setSoftapEnabled(true);
						text2.setText(R.string.open);
					}
				}else if(key.getAction()==KeyEvent.ACTION_UP){

				}
			}
		}
		return false;
	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_hot_spot_nor);
			break;
		}
		return false;
	}

	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_hot_spot_nor);
				finish();
			break;
		}
		return false;
	}

    public void setSoftapEnabled(boolean enable) {
        final ContentResolver cr = mContext.getContentResolver();
        /**
         * Disable Wifi if enabling tethering
         */
        int wifiState = mWifiManager.getWifiState();
        if (enable && ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                    (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
            mWifiManager.setWifiEnabled(false);
            Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 1);
        }

        if (mWifiManager.setWifiApEnabled(null, enable)) {
        } else {
            mState = false;
            text2.setText(R.string.close);
        }

        /**
         *  If needed, restore Wifi on tether disable
         */
        if (!enable) {
            int wifiSavedState = 0;
            withStaEnabled = false;
            try {
                wifiSavedState = Settings.Global.getInt(cr, Settings.Global.WIFI_SAVED_STATE);
                withStaEnabled = wifiSavedState == 1? true : false;
            } catch (Settings.SettingNotFoundException e) {
                ;
            }
            if (wifiSavedState == 1) {
                mWifiManager.setWifiEnabled(true);
                Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 0);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!=null){
        	if (resultCode == 0) {
        		WifiConfiguration wifiConfig = data.getParcelableExtra(SoftapSettingActivity.SOFTAP_CONFIG);
        		smalltext.setText(String.format(getString(SOFTAP_SUBTEXT),
                    wifiConfig.SSID,
                    mSecurityType[SoftapSettingActivity.getSecurityTypeIndex(wifiConfig)]));
        	}
        }
    }

    private void handleWifiApStateChanged(int state) {
        //TextView text2 = (TextView)mListView.getChildAt(START_SOFTAP).findViewById(R.id.text2);

        //TextView text2 = (TextView)mListView.getChildAt(START_SOFTAP).findViewById(R.id.text2);

        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLED:
                mState = true;
                //if(text2!=null)
               // text2.setText(R.string.open);
                mHandler.sendEmptyMessage(SET_TEXT_OPEN);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                mState = false;
                //if(text2!=null)
                //text2.setText(R.string.close);
                mHandler.sendEmptyMessage(SET_TEXT_CLOSE);
                break;
            default:
                break;
        }
    }
}
