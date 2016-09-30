package com.clock.pt1.keeptesting.batterycontroller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.clock.pt1.keeptesting.LogUtil;
import com.clock.pt1.keeptesting.R;
import com.clock.pt1.keeptesting.ScreenUtil;
import com.clock.pt1.keeptesting.ServiceUtil;
import com.stericson.RootTools.RootTools;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class BatteryControllerActivity extends Activity {

	// constant
	// public static final int STATE_START = 0;
	// public static final int STATE_STOP = 1;
	public static final int DEFAULT_LOWER_BOUND = 8;
	public static final int DEFAULT_UPPER_BOUND = 10;
	public static final String TAG = "BatteryController";
	public static final String PREF_LOWER_BOUND = "battery_controll_lower_bound";
	public static final String PREF_UPPER_BOUND = "battery_controll_upper_bound";
	public static final String BROADCAST_BATTERY_INFO = "info";
	private static final String SERVICE_NAME = "com.clock.pt1.keeptesting.batterycontroller.BatteryControllerService";
	public static final String BATTERY_EVENT_INTENT = "com.clock.pt1.keeptesting.batterycontroller.BatteryInfo";
	public static final String LOG_FILE_NAME = "BatteryInfo.log";

	// UI
	private EditText lowerBoundEdit, upperBoundEdit;
	private Button startBtn;
	private ListView logList;

	// member
	private SharedPreferences prefs = null;
	private SharedPreferences.Editor localEditor = null;
	private Intent serviceIntent = null;
	private ArrayAdapter<String> adapter = null;
	private ArrayList<String> logArray = null;
	private BatteryEventReceiver receiver = null;

	
	public class BatteryEventReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			String info = intent.getStringExtra(BatteryControllerActivity.BROADCAST_BATTERY_INFO);
			Log.i(BatteryControllerActivity.TAG,"receive battery event info --"+info);
			
			//adapter.clear();
			logArray.add(0, info);
			//adapter.addAll(logArray);
			adapter.notifyDataSetChanged();
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ScreenUtil.getScreenOrientation(this));
		setContentView(R.layout.battery_controller_layout);

		lowerBoundEdit = (EditText) findViewById(R.id.lower_bound_edit);
		upperBoundEdit = (EditText) findViewById(R.id.upper_bound_edit);
		startBtn = (Button) findViewById(R.id.battery_controll_start_btn);
		logList = (ListView) findViewById(R.id.battery_controll_log_list);

		if (prefs == null) {
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
			localEditor = prefs.edit();
		}
		
		logArray = new ArrayList<String>();
		loadLogFromFile();
		adapter = new ArrayAdapter<String>(this,R.layout.battery_controller_log_list_item,logArray);
		logList.setAdapter(adapter);

		if (ServiceUtil.isServiceRunning(this, SERVICE_NAME)) {
			startBtn.setText(this.getResources().getString(
					R.string.battery_controller_stop));
			
			int lowerBound = 0;
			int upperBound = 0;
			lowerBound = prefs.getInt(BatteryControllerActivity.PREF_LOWER_BOUND,BatteryControllerActivity.DEFAULT_LOWER_BOUND);
			upperBound = prefs.getInt(BatteryControllerActivity.PREF_UPPER_BOUND,BatteryControllerActivity.DEFAULT_UPPER_BOUND);
			lowerBoundEdit.setText(Integer.toString(lowerBound));
			upperBoundEdit.setText(Integer.toString(upperBound));
		} else {
			startBtn.setText(this.getResources().getString(
					R.string.battery_controller_start));
		}

		serviceIntent = new Intent(BatteryControllerActivity.this,
				BatteryControllerService.class);
		
		// register broadcast receiver
		receiver = new BatteryEventReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(BATTERY_EVENT_INTENT);
		this.registerReceiver(receiver, filter);
		
		
		startBtn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (ServiceUtil.isServiceRunning(
						BatteryControllerActivity.this, SERVICE_NAME)) {
					Log.i(TAG, "battery controll service started");
					// stop the background service
					stopService(serviceIntent);
					startBtn.setText(BatteryControllerActivity.this
							.getResources().getString(
									R.string.battery_controller_start));
				} else {
					// going to enable background service
					// check the user input first
					int lowerBound = 0;
					int upperBound = 0;
					Log.i(TAG, "battery controll service stopped");
					try {
						lowerBound = Integer.parseInt(lowerBoundEdit.getText()
								.toString());
						upperBound = Integer.parseInt(upperBoundEdit.getText()
								.toString());
					} catch (NumberFormatException e2) {
						Toast.makeText(
								BatteryControllerActivity.this,
								BatteryControllerActivity.this
										.getResources()
										.getString(
												R.string.battery_controller_input_error),
								Toast.LENGTH_LONG).show();
						return;
					}

					if (upperBound <= lowerBound) {
						Toast.makeText(
								BatteryControllerActivity.this,
								BatteryControllerActivity.this
										.getResources()
										.getString(
												R.string.battery_controller_input_error),
								Toast.LENGTH_LONG).show();
						return;
					}
					// clean the log file first
					File logF = new File(LogUtil.PATH+BatteryControllerActivity.LOG_FILE_NAME);
					if(logF.exists()) {
						logF.delete();
						logArray.clear();
					}
					localEditor.putInt(PREF_LOWER_BOUND, lowerBound);
					localEditor.putInt(PREF_UPPER_BOUND, upperBound);
					localEditor.commit();
					startService(serviceIntent);
					startBtn.setText(BatteryControllerActivity.this
							.getResources().getString(
									R.string.battery_controller_stop));
				}
			}
		});

		if (!RootTools.isAccessGiven()) {
			Dialog alertDialog = new AlertDialog.Builder(this)
					.setTitle(R.string.attention)
					.setMessage(R.string.device_not_rooted)
					.setPositiveButton(
							this.getResources().getString(R.string.confirm),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									BatteryControllerActivity.this.finish();
								}
							}).create();
			alertDialog.show();
		}
	}
	
	private void loadLogFromFile() {
		String line = null;
		File logF = new File(LogUtil.PATH + BatteryControllerActivity.LOG_FILE_NAME);
		if(logF.exists()) {
			try {
				FileReader logFR = new FileReader(logF);
				BufferedReader bufferedReader = new BufferedReader(logFR);
				line = bufferedReader.readLine();
				while(line != null) {
					logArray.add(0, line);
					line = bufferedReader.readLine();
				}
				logFR.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		this.unregisterReceiver(receiver);
		super.onDestroy();
	}
}
