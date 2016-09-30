package com.clock.pt1.keeptesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RebootActivity  extends Activity {
	private static final String TAG = "RebootTest";
	@SuppressLint("SdCardPath")
	private static final int DEFAULT_REBOOT_COUNT = 10000;
	private static final int DEFAULT_IDLE_TIME = 10;
	private static final int STATE_IDLE = 0;
	private static final int STATE_PAUSE = 1;
	private static final int STATE_RUNNING = 2;
	
	private EditText timerEditText, totalRebootEditText;
	private TextView testStatusText;
	private TextView currentRebootNumText;
	private TextView remainRebootText;
	private TextView remainRebootNumText;
	private TextView countDownText;
	private TextView countDownNumText;
	private SharedPreferences prefs = null;
	private Button stopBtn, startBtn;
	private int totalnumbers;
	private int rebootnumber = -1;
	private int timers;

	private int mState;
	private int timersold;

	private Thread rebootThread = null;
	private String logFileName = null;
	private PowerManager.WakeLock lock = null;
	
	private volatile boolean rebootRunning = true;
	private volatile int startDelay = 0;

	private void showMsgText() {
		testStatusText.setText(R.string.repeat_reboot_current_count_text);
		remainRebootText.setText(R.string.repeat_reboot_remain_count_text);
		countDownText.setText(R.string.repeat_reboot_second_left_text);
		currentRebootNumText.setText(String.valueOf(rebootnumber));
		remainRebootNumText.setText(String.valueOf(totalnumbers - rebootnumber));
		countDownNumText.setText(String.valueOf(timers));
	}

	@SuppressLint("SimpleDateFormat")
	private void getnumbers() {
		
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
		String str = format.format(new Date());

		if (prefs == null) {
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
		this.totalnumbers = this.prefs.getInt("totalnumbers",
				DEFAULT_REBOOT_COUNT);
		this.rebootnumber = this.prefs.getInt("rebootnumber", 0);
		this.timersold = this.timers = this.prefs.getInt("timers",
				DEFAULT_IDLE_TIME);
		this.logFileName = this.prefs.getString("logfile", "RebootResult-"+str);
		Log.i(TAG, "[getnumbser]totalnumbers=" + totalnumbers
				+ " rebootnumber=" + rebootnumber + " timers=" + timers);
	}

	private void startReboot() {
		SharedPreferences.Editor localEditor = prefs.edit();
		localEditor.putInt("timers", timersold);
		localEditor.putInt("totalnumbers", totalnumbers);
		localEditor.putInt("rebootnumber", rebootnumber);
		localEditor.putString("logfile", logFileName);
		localEditor.putInt("autoboot", MainActivity.AUTO_BOOT_REBOOT);
		localEditor.commit();
		Intent intent = new Intent(Intent.ACTION_REBOOT);
		intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@SuppressLint("HandlerLeak")
	private Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				showMsgText();
				break;
			}
			super.handleMessage(msg);
		}
	};
	
	@SuppressLint("SimpleDateFormat")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		setRequestedOrientation(ScreenUtil.getScreenOrientation(this));
		setContentView(R.layout.repeat_reboot_layout);
		
		timerEditText = (EditText) findViewById(R.id.countdown_timer);
		totalRebootEditText = (EditText) findViewById(R.id.total_reboot_count);
		testStatusText = (TextView) findViewById(R.id.test_status_text);
		currentRebootNumText = (TextView) findViewById(R.id.current_reboot_num_text);
		remainRebootText = (TextView) findViewById(R.id.remain_reboot_text);
		remainRebootNumText = (TextView) findViewById(R.id.remain_reboot_num_text);
		countDownText = (TextView) findViewById(R.id.count_down_text);
		countDownNumText = (TextView) findViewById(R.id.count_down_num_text);
		stopBtn = (Button) findViewById(R.id.reboot_stop_button);
		startBtn = (Button) findViewById(R.id.reboot_start_button);

		Log.i(TAG, "onCreate");
		
		//lock = ((MainActivity)this.getActivity()).wakeLock;

		getnumbers();
		rebootnumber++;
		showMsgText();
		if (rebootnumber == 1) {
			mState = STATE_IDLE;
		} else {
			mState = STATE_RUNNING;
		}

		if (rebootnumber > totalnumbers) {
			testStatusText.setText(" ");
			testStatusText.setText(R.string.repeat_reboot_test_done_text);
			currentRebootNumText.setText(" ");
			currentRebootNumText.setText(String.valueOf(totalnumbers));
			mState = STATE_IDLE;
			totalRebootEditText.setText(String.valueOf(this.totalnumbers));
			timerEditText.setText(String.valueOf(this.timersold));

			remainRebootText.setText(" ");
			countDownText.setText(" ");
			remainRebootNumText.setText(" ");
			countDownNumText.setText(" ");
			
			if(lock != null && lock.isHeld()) {
				lock.release();
			}
		}
		
		if(mState == STATE_IDLE) {
			// INIT state
			startBtn.setEnabled(true);
			stopBtn.setEnabled(false);
		} else {
			// we are rebooting
			startBtn.setEnabled(true);
			stopBtn.setEnabled(true);
			startBtn.setText(RebootActivity.this.getResources().getString(R.string.repeat_reboot_pause_button));
			totalRebootEditText.setText(String.valueOf(this.totalnumbers));
			timerEditText.setText(String.valueOf(this.timersold));
		}

		//PATH = ((MainActivity)this.getActivity()).getPath();
		FileWriter result = null;
		try {
			File tempf = new File(LogUtil.PATH + "temp");
			if(tempf.exists()) {
				FileReader temp = new FileReader(tempf);
				BufferedReader bufferedReader = new BufferedReader(temp);
				String line = bufferedReader.readLine();
				if(line != null) {
					try {
						long start_time = Long.parseLong(line);
						long end_time = System.currentTimeMillis();
						result = new FileWriter(new File(LogUtil.PATH
								+ this.logFileName), true);
						SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss ");
						String timestamp = format.format(new Date());
						result.append(timestamp+"Duration:" + ((end_time - start_time)/1000) + "\n");
						result.flush();
						result.close();
					} catch (NumberFormatException e2) {
						Log.e(TAG, "Invalid number format!!");
					}
				}

				temp.close();
				tempf.delete();
			}
		} catch (IOException e1) {
			Log.i(TAG, "IOException:" + e1);
		}
		
		// Check if this activity is started by 'am' command and parameters are given
		Intent intent = getIntent();
        int cmdLoop = intent.getIntExtra(MainActivity.LOOP_NUMBER, 18);
        int cmdIdle = intent.getIntExtra(MainActivity.IDLE_INTERVAL, 10);
        startDelay = intent.getIntExtra(MainActivity.START_DELAY, 0);
        boolean cmdIsAutoFlag = intent.getBooleanExtra(MainActivity.IS_AUTO_FLAG,
                false);
        Log.d(TAG, "Auto Reboot: loop = " + cmdLoop + "  idle = " + cmdIdle
                + " isAutoFlag = " + cmdIsAutoFlag);
        
		if (rebootThread == null) {
			Log.i(TAG, "creating reboot monitor thread...");
			rebootRunning = true;
			rebootThread = new Thread(new Runnable() {

				public void run() {
					if(startDelay > 0) {
						try {
							Thread.sleep(startDelay*1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					while (rebootRunning) {
						if ((rebootnumber <= totalnumbers) && (mState == STATE_RUNNING)
								&& (timers > 0)) {
							Message message = new Message();
							message.what = 1;
							timers--;
							Log.i(TAG, "timers=" + timers);
							myHandler.sendMessage(message);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						} else if(mState == STATE_PAUSE || mState == STATE_IDLE) {
							// release CPU
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
						if (timers == 0)
							break;
					}
					if (!rebootRunning) {
						Log.i(TAG, " thread exiting......");
						return;
					}
					if (timers == 0) {
						Log.i(TAG, "startReboot");
						try {
							FileWriter result = null;
							FileWriter temp = null;

							result = new FileWriter(new File(LogUtil.PATH
									+ logFileName), true);
							temp = new FileWriter(new File(LogUtil.PATH + "temp"),
									false);
							SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss ");
							String timestamp = format.format(new Date());
							result.append(timestamp+"reboot rount:" + rebootnumber + "\n");
							long start_time = System.currentTimeMillis();
							temp.write(Long.toString(start_time) + "\n");
							result.flush();
							temp.flush();
							result.close();
							temp.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						startReboot();
					}
				}
			});
			rebootThread.start();
		}
		
		startBtn.setOnClickListener(new Button.OnClickListener() {
			@SuppressLint("SimpleDateFormat")
			public void onClick(View v) {
				if(mState == STATE_IDLE) {
					/* clean all files since this is a restart */
					cleanFiles();
					if(lock != null && !lock.isHeld()) {
						lock.acquire();
					}
					timersold = timers = Integer.parseInt(timerEditText.getText()
							.toString());
					totalnumbers = Integer.parseInt(totalRebootEditText.getText()
							.toString());
					rebootnumber = 1;
					Log.i(TAG, "timers=" + timers + "rebootnumber"
							+ rebootnumber + "    totalnumbers" + totalnumbers);
					
					SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
					String str = format.format(new Date());
					logFileName = "RebootResult-"+str;
					stopBtn.setEnabled(true);
					mState = STATE_RUNNING;
					startBtn.setText(RebootActivity.this.getResources().getString(R.string.repeat_reboot_pause_button));
				} else if(mState == STATE_RUNNING) {
					mState = STATE_PAUSE;
					startBtn.setText(RebootActivity.this.getResources().getString(R.string.repeat_reboot_start_button));
				} else if(mState == STATE_PAUSE){
					startBtn.setText(RebootActivity.this.getResources().getString(R.string.repeat_reboot_pause_button));
					// in case user change the parameter during the test
					// have to update the variable before reboot
					if(timersold  != Integer.parseInt(timerEditText.getText()
							.toString())) {
						timersold = Integer.parseInt(timerEditText.getText()
								.toString());
						timers = timersold;
					}
					if(totalnumbers != Integer.parseInt(totalRebootEditText.getText()
							.toString())) {
						totalnumbers = Integer.parseInt(totalRebootEditText.getText()
								.toString());
					}
					mState = STATE_RUNNING;
				}
			}
		});
		
		stopBtn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				mState = STATE_IDLE;
				showMsgText();
				startBtn.setText(RebootActivity.this.getResources().getString(R.string.repeat_reboot_start_button));
				if(lock != null && lock.isHeld()) {
					lock.release();
				}
				rebootnumber = 0;
				SharedPreferences.Editor localEditor = prefs.edit();
				localEditor.putInt("rebootnumber", rebootnumber);
				localEditor.putInt("autoboot", MainActivity.AUTO_BOOT_MAIN);
				localEditor.commit();
				
				/* enable "continue" button once we press the "pause" button */
				startBtn.setEnabled(true);
				stopBtn.setEnabled(false);
			}
		});
		
		// 'am' command start reboot test
		if(cmdIsAutoFlag && cmdLoop > 0) {
            totalRebootEditText.setText("" + cmdLoop);
            timerEditText.setText("" + cmdIdle);
            Log.d(TAG, "Auto Reboot:  click start button");
            startBtn.performClick();
		}
	}
    
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "destroy");
		rebootRunning = false;
		rebootThread  = null;
		mState    = STATE_IDLE;
	}
	
	private void cleanFiles() {
		File tempf = new File(LogUtil.PATH + "temp");
		if(tempf.exists()) {
			tempf.delete();
		}
	}
}
