package com.clock.pt1.keeptesting.clock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.clock.pt1.keeptesting.MainActivity;
import com.clock.pt1.keeptesting.R;
import com.clock.pt1.keeptesting.ScreenUtil;
import com.clock.pt1.keeptesting.ServiceUtil;

public class SleepAndWakeUpActivity extends Activity {
	public static final String TAG = "SleepTest";
	private TextView testStatusText;
	private TextView countDownText;
	private EditText sleepIntervalEditText;
	private EditText wakeupIntervalEditText;
	private EditText startdelayEditText;
	private EditText loopEditText;
	private Button startBtn;
	private Button stopBtn;
	private int interval;
	private int sleepInterval;
	public static final int DEFAULT_LOOP_COUNT = 10000;
	public static final int DEFAULT_SLEEP_INTERVAL = 10;
	public static final int DEFAULT_IDLE_INTERVAL = 9;
	public static final int STATE_IDLE = 0;
	public static final int STATE_PAUSE = 1;
	public static final int STATE_RUNNING = 2;
	private int mState = STATE_IDLE;
	@SuppressLint("SdCardPath")
	private int loop = DEFAULT_LOOP_COUNT;
	private volatile int startDelay = 0;
	
	private static final String SERVICE_NAME = "com.clock.pt1.keeptesting.clock.SleepAndWakeUpService";
	private volatile boolean isServiceOn = false;
	private boolean isBinded = false;
	private ServiceMessageReceiver mServiceMessageReceiver = null;
	private SharedPreferences mPrefs = null;
	private SleepAndWakeUpService.MyBinder mService = null;
	
	private ServiceConnection mConnection = new ServiceConnection () {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			mService = (SleepAndWakeUpService.MyBinder)arg1;
			mState = STATE_IDLE;
			mState = mService.getCurrentStatus();
			if(mState == STATE_IDLE) {
				stopBtn.setEnabled(false);
				startBtn.setEnabled(true);
			} else if(mState == STATE_RUNNING) {
				startBtn.setText(SleepAndWakeUpActivity.this.getResources()
	                    .getString(R.string.sleep_wakeup_pause_text));
				stopBtn.setEnabled(true);
				startBtn.setEnabled(true);
			} else if(mState == STATE_PAUSE) {
				startBtn.setText(SleepAndWakeUpActivity.this.getResources()
	                    .getString(R.string.sleep_wakeup_start_text));
				// if status is pause, update the UI info
				int total = mService.getTotalCount();
				int current = mService.getCurrentCount();
				int secondLeft = mService.getSecondLeft()+1;
				countDownText.setText(secondLeft
                        + getResources().getString(
                                R.string.sleep_wakeup_second_left_text)
                        + "\n\n");
				testStatusText.setText(getResources().getString(
                        R.string.sleep_wakeup_already_test_text)
                        + current + "/" + total);
				stopBtn.setEnabled(true);
				startBtn.setEnabled(true);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			SleepAndWakeUpActivity.this.mService = null;
		}
		
	};

	public class ServiceMessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getStringExtra("action");
			if (action != null) {
				if(action.equals(SleepAndWakeUpService.UPDATE_INFO)) {
					int total = intent.getIntExtra("total", 0);
					int current = intent.getIntExtra("current", 0);
					int secondLeft = intent.getIntExtra("secondleft", 0)+1;
					countDownText.setText(secondLeft
	                        + getResources().getString(
	                                R.string.sleep_wakeup_second_left_text)
	                        + "\n\n");
					testStatusText.setText(getResources().getString(
                            R.string.sleep_wakeup_already_test_text)
                            + current + "/" + total);
				} else if(action.equals(SleepAndWakeUpService.UPDATE_DONE)) {
					int total = intent.getIntExtra("total", 0);
					int current = intent.getIntExtra("current", 0);
	                countDownText.setText(R.string.sleep_wakeup_test_finish_text);
					testStatusText.setText(getResources().getString(
                            R.string.sleep_wakeup_already_test_text)
                            + current + "/" + total);
	                startBtn.setText(SleepAndWakeUpActivity.this.getResources()
	                        .getString(R.string.sleep_wakeup_start_text));
	                mState = STATE_IDLE;
	                stopBtn.setEnabled(false);
	                startBtn.setEnabled(true);					
				}
			}
		}
	}
	
	@SuppressLint("SimpleDateFormat")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ScreenUtil.getScreenOrientation(this));
		setContentView(R.layout.sleep_and_wake_up_layout);

		testStatusText = (TextView) findViewById(R.id.test_status_text);
		countDownText = (TextView) findViewById(R.id.count_down_text);
		sleepIntervalEditText = (EditText) findViewById(R.id.sleep_interval);
		wakeupIntervalEditText = (EditText) findViewById(R.id.wakeup_interval);
		startdelayEditText = (EditText) findViewById(R.id.start_delay);
		loopEditText = (EditText) findViewById(R.id.loop);
		startBtn = (Button) findViewById(R.id.sleep_start_button);
		stopBtn = (Button) findViewById(R.id.sleep_stop_button);


		testStatusText.setText(R.string.sleep_wakeup_test_finish_text);

		// Check if this activity is started by 'am' command and parameters are
		// given
		Intent intent = getIntent();
		int cmdLoop = intent.getIntExtra(MainActivity.LOOP_NUMBER, 18);
		int cmdSleep = intent.getIntExtra(MainActivity.SLEEP_INTERVAL, 19);
		int cmdWakeup = intent.getIntExtra(MainActivity.WAKEUP_INTERVAL, 20);
		startDelay = intent.getIntExtra(MainActivity.START_DELAY, 0);
		boolean cmdIsAutoFlag = intent.getBooleanExtra(
				MainActivity.IS_AUTO_FLAG, false);
		Log.d(TAG, "Auto Standby: iLoop = " + cmdLoop + "  iWakeup = "
				+ cmdWakeup + "  iSleep = " + cmdSleep + " isAutoFlag = "
				+ cmdIsAutoFlag);
		
		// register update message receiver
		mServiceMessageReceiver = new ServiceMessageReceiver();
		IntentFilter updateFilter = new IntentFilter();
		updateFilter.addAction(SleepAndWakeUpService.UPDATEMESSAGE_ACTION);
		this.registerReceiver(mServiceMessageReceiver, updateFilter);
		
		isServiceOn = ServiceUtil.isServiceRunning(SleepAndWakeUpActivity.this, SERVICE_NAME);
		if(isServiceOn) {
			startBtn.setEnabled(false);
			stopBtn.setEnabled(true);
		} else {
			startBtn.setEnabled(true);
			stopBtn.setEnabled(false);
		}
		// load the last test result:
		if (mPrefs == null) {
			mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
		int totalCount = this.mPrefs.getInt(MainActivity.LOOP_NUMBER, DEFAULT_LOOP_COUNT);
		int sleepTime = this.mPrefs.getInt(MainActivity.SLEEP_INTERVAL,
				DEFAULT_SLEEP_INTERVAL);
		int idleTime = this.mPrefs.getInt(MainActivity.WAKEUP_INTERVAL,
				DEFAULT_IDLE_INTERVAL);
		int currentCount = this.mPrefs.getInt(
				MainActivity.CURRENT_NUMBER, 0);
		testStatusText.setText(getResources().getString(
                R.string.sleep_wakeup_already_test_text)
                + currentCount + "/" + totalCount);
		loopEditText.setText(Integer.toString(totalCount));
		sleepIntervalEditText.setText(Integer.toString(sleepTime));
		wakeupIntervalEditText.setText(Integer.toString(idleTime));
		
		startBtn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Log.e(TAG, "press start button. current state: "+mState);
				if (mState == STATE_IDLE) {
					// get parameters from UI elements
					String StringInterval = wakeupIntervalEditText
							.getText().toString();
					String StringLoop = loopEditText.getText().toString();

					if (null != StringLoop && 0 != StringLoop.length()) {
						loop = Integer.parseInt(StringLoop);
					} else {
						loop = DEFAULT_LOOP_COUNT;
					}

					if (null != StringInterval
							&& 0 != StringInterval.length()) {
						interval = Integer.parseInt(StringInterval);
					} else {
						interval = DEFAULT_SLEEP_INTERVAL;
					}

					String StringSleepInterval = sleepIntervalEditText
							.getText().toString();
					if (null != StringSleepInterval
							&& 0 != StringSleepInterval.length()) {
						sleepInterval = Integer
								.parseInt(StringSleepInterval);
					} else {
						sleepInterval = DEFAULT_IDLE_INTERVAL;
					}

					String StringStartDelay = startdelayEditText.getText()
							.toString();
					if (null != StringStartDelay
							&& 0 != StringStartDelay.length()) {
						startDelay = Integer.parseInt(StringStartDelay);
					} else {
						startDelay = 0;
					}
					
					// run in background, start background service

					Intent it = new Intent(SleepAndWakeUpActivity.this,SleepAndWakeUpService.class);
					Bundle bundle = new Bundle();
					bundle.putInt(MainActivity.LOOP_NUMBER, loop);
					bundle.putInt(MainActivity.SLEEP_INTERVAL, sleepInterval);
					bundle.putInt(MainActivity.WAKEUP_INTERVAL, interval);
					bundle.putInt(MainActivity.START_DELAY, startDelay);
					it.putExtras(bundle);
						
					startService(it);
					bindService(new Intent(SleepAndWakeUpActivity.this, SleepAndWakeUpService.class), SleepAndWakeUpActivity.this.mConnection, Service.BIND_ABOVE_CLIENT);
					isBinded = true;
					mState = STATE_RUNNING;
					startBtn.setText(SleepAndWakeUpActivity.this.getResources()
                            .getString(R.string.sleep_wakeup_pause_text));
					stopBtn.setEnabled(true);
				} else if (mState == STATE_RUNNING) {
                    mState = STATE_PAUSE;
                    startBtn.setText(SleepAndWakeUpActivity.this.getResources()
                            .getString(R.string.sleep_wakeup_start_text));
                    mService.pauseService();
                } else if (mState == STATE_PAUSE) {
                	startBtn.setText(SleepAndWakeUpActivity.this.getResources()
                            .getString(R.string.sleep_wakeup_pause_text));
                	mState = STATE_RUNNING;
                	mService.restartService();
                }
			}
		});
		
		stopBtn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				//sendBroadcastMessage(SleepAndWakeUpService.SERVICE_STOP);
				Log.i(TAG,"press stop button.");
				mService.stopService();
				unbindService(SleepAndWakeUpActivity.this.mConnection);
				isBinded = false;
				mState = STATE_IDLE;
                startBtn.setText(SleepAndWakeUpActivity.this.getResources()
                        .getString(R.string.sleep_wakeup_start_text));
				stopBtn.setEnabled(false);
				startBtn.setEnabled(true);
			}
		});

		// 'am' command start sleep test
		if (cmdIsAutoFlag && cmdLoop > 0) {
			Log.d(TAG, "Auto Standby: set ui");
			loopEditText.setText("" + cmdLoop);
			wakeupIntervalEditText.setText("" + cmdWakeup);
			sleepIntervalEditText.setText("" + cmdSleep);
			startBtn.performClick();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		isServiceOn = ServiceUtil.isServiceRunning(SleepAndWakeUpActivity.this, SERVICE_NAME);
		if(isServiceOn && !isBinded) {
			bindService(new Intent(SleepAndWakeUpActivity.this, SleepAndWakeUpService.class), SleepAndWakeUpActivity.this.mConnection, Service.BIND_ABOVE_CLIENT);
			isBinded = true;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		isServiceOn = ServiceUtil.isServiceRunning(SleepAndWakeUpActivity.this, SERVICE_NAME);
		if(isBinded) {
			unbindService(this.mConnection);
			isBinded = false;
		}
	}
	
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mServiceMessageReceiver != null) {
        	unregisterReceiver(mServiceMessageReceiver);
        }
    }
}
