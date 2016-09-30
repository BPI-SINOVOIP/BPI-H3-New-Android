package com.clock.pt1.keeptesting.clock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.clock.pt1.keeptesting.LogUtil;
import com.clock.pt1.keeptesting.MainActivity;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

@SuppressWarnings("deprecation")
public class SleepAndWakeUpService extends Service {
	public static final String TAG = "SleepTest";

	public static final String SERVICE_CONTROL = "com.clock.pt1.keeptesting.sleepwakeup.CONTROL_SERVICE";
	public static final String UPDATEMESSAGE_ACTION = "com.clock.pt1.keeptesting.sleepwakeup.UPDATE_MESSAGE";
	public static final String UPDATE_INFO = "info";
	public static final String UPDATE_DONE = "done";
	public static final String SERVICE_STOP = "stop";
	public static final String SERVICE_PAUSE = "pause";
	public static final String SERVICE_RESTART = "restart";
	private String logFileName = "SleepResult-";
	private int mTotalCount = 1;
	private volatile int mCurrentCount = 0;
	private int mSleepTime = SleepAndWakeUpActivity.DEFAULT_SLEEP_INTERVAL;
	private int mIdleTime = SleepAndWakeUpActivity.DEFAULT_IDLE_INTERVAL;
	private int mStartDelay = 0;
	private int mSecondLeft = 0;
	private int mState = SleepAndWakeUpActivity.STATE_IDLE;
	private SharedPreferences mPrefs = null;
	private boolean mSleepRunning = true;
	private boolean normalSDK = true;
	private volatile boolean mIsWakeup = false;
	private AlarmManager mAlarmMgr = null;
	private Intent mIntent;
	private PendingIntent pendIntent;
	private AlarmWakeupBroadcastReceiver mAlarmWakeupBroadcastReceiver = null;
	private MyBinder mBinder = new MyBinder();

	class AlarmWakeupBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			mIsWakeup = true;
			mSecondLeft = mSleepTime;
			mCurrentCount++;
			Log.v(TAG, "1.curLoop =" + mCurrentCount + ", loop ="
					+ mTotalCount + ",mState =" + mState);

			if (mPrefs == null) {
				mPrefs = PreferenceManager.getDefaultSharedPreferences(SleepAndWakeUpService.this);
			}
			SharedPreferences.Editor localEditor = mPrefs.edit();
			localEditor.putInt(MainActivity.CURRENT_NUMBER, mCurrentCount);
			localEditor.commit();
			
			if (mState != SleepAndWakeUpActivity.STATE_IDLE) {

				Log.v(TAG, "2.mState =" + mState + ",curLoop ="
						+ mCurrentCount + ", loop =" + mTotalCount);

				if (mCurrentCount == mTotalCount) {
					SleepAndWakeUpService.this.unregisterReceiver(this);
					SleepAndWakeUpService.this.mAlarmWakeupBroadcastReceiver = null;
				}
			}
			wakeup();
			Log.v(TAG, "3.mState =" + mState + ",curLoop =" + mCurrentCount
					+ ", loop =" + mTotalCount);
		}
	}
	
	public class MyBinder extends Binder {
		public int getCurrentStatus() {
			return mState;
		}
		
		public void stopService() {
			SleepAndWakeUpService.this.mSleepRunning = false;
			SleepAndWakeUpService.this.stopSelf();
		}
		
		public void pauseService() {
			mState = SleepAndWakeUpActivity.STATE_PAUSE;
		}
		
		public void restartService() {
			mState = SleepAndWakeUpActivity.STATE_RUNNING;
		}
		
		public int getTotalCount() {
			return mTotalCount;
		}
		
		public int getCurrentCount() {
			return mCurrentCount;
		}
		
		public int getSecondLeft() {
			return mSecondLeft;
		}
	}
	
	private void sendBroadcastMessage(String action) {
        Intent intent = new Intent();  
        intent.setAction(UPDATEMESSAGE_ACTION);
        intent.putExtra("action", action);
        intent.putExtra("total", mTotalCount);
        intent.putExtra("current", mCurrentCount);
        intent.putExtra("secondleft", mSecondLeft);
        this.sendBroadcast(intent);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
    
	private void parseParameters(Intent intent) {
		// start service from Activity
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			mTotalCount = bundle.getInt(MainActivity.LOOP_NUMBER);
			mSleepTime = bundle.getInt(MainActivity.SLEEP_INTERVAL);
			mIdleTime = bundle.getInt(MainActivity.WAKEUP_INTERVAL);
			mStartDelay = bundle.getInt(MainActivity.START_DELAY);

			if (mPrefs == null) {
				mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			}
			SharedPreferences.Editor localEditor = mPrefs.edit();
			localEditor.putInt(MainActivity.LOOP_NUMBER, mTotalCount);
			localEditor.putInt(MainActivity.SLEEP_INTERVAL, mSleepTime);
			localEditor.putInt(MainActivity.CURRENT_NUMBER, 0);
			localEditor.putInt(MainActivity.WAKEUP_INTERVAL, mIdleTime);
			localEditor.commit();
			Log.i(TAG, "Starting sleep wakeup service. TotalCount:"
					+ mTotalCount + " sleep interval:" + mSleepTime
					+ " wakeup interval:" + mIdleTime + " start delay:"
					+ mStartDelay);
		// restart service by system e.g. OOM killer
		} else {
			if (mPrefs == null) {
				mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			}
			this.mTotalCount = this.mPrefs.getInt(MainActivity.LOOP_NUMBER, 1);
			this.mSleepTime = this.mPrefs.getInt(MainActivity.SLEEP_INTERVAL,
					SleepAndWakeUpActivity.DEFAULT_SLEEP_INTERVAL);
			this.mIdleTime = this.mPrefs.getInt(MainActivity.WAKEUP_INTERVAL,
					SleepAndWakeUpActivity.DEFAULT_IDLE_INTERVAL);
			this.mCurrentCount = this.mPrefs.getInt(
					MainActivity.CURRENT_NUMBER, 0);
		}
	}
	
	@SuppressLint("SimpleDateFormat")
	public void onStart(Intent intent, int startId) {
		// get test parameter
		parseParameters(intent);
		
		mAlarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		mIntent = new Intent(this.getApplicationContext(), AlarmReceiver.class);
		pendIntent = PendingIntent.getBroadcast(this.getApplicationContext(),
				0, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		// register alarm receiver
		IntentFilter alarmFilter = new IntentFilter();
		alarmFilter.addAction(AlarmReceiver.ALARM_RECEIVE_ACTION);
		mAlarmWakeupBroadcastReceiver = new AlarmWakeupBroadcastReceiver();
		this.registerReceiver(mAlarmWakeupBroadcastReceiver, alarmFilter);

		mIsWakeup = false;
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
		String str = format.format(new Date());
		logFileName = "SleepResult-" + str;
		mCurrentCount = 0;
		mSecondLeft = mSleepTime;
		mState = SleepAndWakeUpActivity.STATE_RUNNING;
		
		runSleepWakeUpTest();
	}

	@SuppressLint("SimpleDateFormat")
	private void runSleepWakeUpTest() {
		mSleepRunning = true;
		Thread mThread = new Thread() {
			@SuppressLint("NewApi")
			@Override
			public void run() {
				if (mStartDelay > 0) {
					try {
						Thread.sleep(mStartDelay * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				while (mSleepRunning) {
					if (mState == SleepAndWakeUpActivity.STATE_PAUSE
							|| mState == SleepAndWakeUpActivity.STATE_PAUSE) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Log.v(TAG, "thread interrupted");
						}
					} else if (mState == SleepAndWakeUpActivity.STATE_RUNNING) {
						if (mCurrentCount == mTotalCount) {
							Log.v(TAG, "curloop = " + mCurrentCount
									+ ",loop = " + mTotalCount + ". Stop?");
							sendBroadcastMessage(UPDATE_DONE);
							mSleepRunning = false;
							SleepAndWakeUpService.this.stopSelf();
							mAlarmMgr.cancel(pendIntent);
							mState = SleepAndWakeUpActivity.STATE_IDLE;

							continue;
						}
						Message message = new Message();
						message.what = 1;
						message.arg1 = mSecondLeft;
						mSecondLeft--;
						Log.i(TAG, "timers=" + mSecondLeft);
						// update UI information
						sendBroadcastMessage(UPDATE_INFO);

						if (mSecondLeft < 0) {
							try {
								File resultF = new File(LogUtil.PATH
										+ logFileName);
								if (!resultF.exists()) {
									resultF.createNewFile();
								}
								FileWriter result = new FileWriter(resultF);
								SimpleDateFormat format = new SimpleDateFormat(
										"MM-dd HH:mm:ss ");
								String timestamp = format.format(new Date());
								result.write(timestamp + "round "
										+ (mCurrentCount+1) + " start!\n");
								result.flush();
								result.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							if (normalSDK) {
								mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP,
										System.currentTimeMillis()
												+ (mSleepTime * 1000),
										pendIntent);
							} else {
								try {
									mAlarmMgr.set(4, System.currentTimeMillis()
											+ (mSleepTime * 1000), pendIntent);
								} catch (Exception e) {
									normalSDK = true;
									mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP,
											System.currentTimeMillis()
													+ (mSleepTime * 1000),
											pendIntent);
								}
							}
							standby();

							while (mState != SleepAndWakeUpActivity.STATE_IDLE
									&& !mIsWakeup)
								;

							mAlarmMgr.cancel(pendIntent);
							mIsWakeup = false;
						} else {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					}

				}

				Log.v(TAG, "sleep thread exiting");
				mAlarmMgr.cancel(pendIntent);
				mState = SleepAndWakeUpActivity.STATE_IDLE;
			}
		};
		mThread.start();
	}

	private void standby() {
		Log.v(TAG, "standby");
		PowerManager pm = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		pm.goToSleep(SystemClock.uptimeMillis());
	}

	private void wakeup() {
		Log.v(TAG, "wakeUp");
		KeyguardManager km = (KeyguardManager) this
				.getSystemService(Context.KEYGUARD_SERVICE);
		KeyguardLock kl = km.newKeyguardLock("unLock");
		kl.disableKeyguard();

		PowerManager pm = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		if (getAndroidSDKVersion() >= 17) {
			pm.wakeUp(SystemClock.uptimeMillis());
			
		} else {
			pm.userActivity(SystemClock.uptimeMillis(), false);
		}
	}

	private int getAndroidSDKVersion() {
		int version = 0;
		try {
			version = Integer.valueOf(android.os.Build.VERSION.SDK);
		} catch (NumberFormatException e) {
		}
		return version;
	}

	@Override
	public void onDestroy() {
		mSleepRunning = false;
		super.onDestroy();
		if (mAlarmWakeupBroadcastReceiver != null)
			this.unregisterReceiver(mAlarmWakeupBroadcastReceiver);
	}
}
