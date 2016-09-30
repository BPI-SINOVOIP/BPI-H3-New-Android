package com.clock.pt1.keeptesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.app.AlertDialog;
import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;
import android.os.Message;
import android.os.RecoverySystem;
import android.preference.PreferenceManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;

public class OTAActivity extends Activity implements
		RecoverySystem.ProgressListener {
	// GUI
	private EditText totalTestCount, idleTime;
	private Button startButton, stopButton;
	private TextView remainCount, countDownTime, remainText, OTAProgress;
	private Dialog alertDialog;

	// constant
	private static final int HANDLE_MSG_COUNT_DOWN = 1;
	private static final int HANDLE_MSG_OTA_FAIL = 2;
	private static final int RECOVERY_VFY_PROGRESS = 3;
	private static final int RECOVERY_VFY_FAILED = 4;
	private static final int RECOVERY_CPY_PROGRESS = 5;
	private static final int RECOVERY_CPY_FAILED = 6;
	private static final int RECOVERY_PROGRESS = 7;
	private static final int RECOVERY_OTA_START = 8;
	
	private static final int DEFAULT_TEST_COUNT = 1000;
	private static final int DEFAULT_COUNT_DOWN = 10;
	private static final String TAG = "RepeatOTA";
	@SuppressLint("SdCardPath")
	private static final String PACKAGE_PATH = "/data/update.zip";
	@SuppressLint("SdCardPath")
	private static final String LAST_INSTALL_LOG_PATH = "/cache/recovery/last_install";

	// member
	private int mCurrentCount = 0;
	private int mTotalCount = DEFAULT_TEST_COUNT;
	private volatile boolean mIsRunning = false;
	private int mCountDown = DEFAULT_COUNT_DOWN;
	private SharedPreferences mPrefs;
	private String mLogFileName = null;

	/*
	 * check /cache/recovery/last_install to see if the last install success or
	 * not the format of last_install file: 
	 * -----------------------------------
	 * /cache/update.zip 
	 * 0 
	 * ----------------------------------- 
	 * second line, 0 means fail, 1 means success
	 */
	private boolean isOTASuccess() {
		File lastInstallLog = new File(LAST_INSTALL_LOG_PATH);
		if (lastInstallLog.exists()) {
			Log.e(TAG, "last install exist");
			try {
				FileReader fr = new FileReader(LAST_INSTALL_LOG_PATH);
				@SuppressWarnings("resource")
				BufferedReader br = new BufferedReader(fr);
				String line = br.readLine();
				line = br.readLine();
				if (Integer.parseInt(line) == 0) {
					return false;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/*
	 * Display the information to GUI, including timer count down and current
	 * factory reset count
	 */
	private void showMsgText() {
		remainText.setText(R.string.repeat_OTA_remain_text);
		remainCount.setText(String.valueOf(mCurrentCount) + "/"
				+ String.valueOf(mTotalCount));
		countDownTime.setText(String.valueOf(mCountDown));
	}

	/*
	 * Start OTA procedure
	 */
	private boolean startOTAUpgrade() {

		File OTAPackage = new File(PACKAGE_PATH);

		// record the configuration anyway
		SharedPreferences.Editor localEditor = mPrefs.edit();
		localEditor.putInt("totalcount", mTotalCount);
		localEditor.putInt("currentcount", mCurrentCount);
		localEditor.putString("logfile", mLogFileName);
		localEditor.commit();
		
		try {
			FileWriter temp = null;
			temp = new FileWriter(new File(LogUtil.PATH + "otatemp"),
					false);
			long start_time = System.currentTimeMillis();
			temp.write(Long.toString(start_time) + "\n");
			temp.flush();
			temp.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (OTAPackage.exists()) {

			try {
				RecoverySystem.verifyPackage(OTAPackage, this, null);
				RecoverySystem.installPackage(this, OTAPackage);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (GeneralSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}

			return true;
		} else {
			Log.e(TAG, "OTA package does not exist");
			return false;
		}
	}

	/*
	 * When starting the GUI, read configuration from preferenceManager
	 * configuration includes: total test count, current test count and timer
	 * count down(seconds)
	 */

	private void getOTAConfig() {
		if (mPrefs == null) {
			mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
		mTotalCount = mPrefs.getInt("totalcount", DEFAULT_TEST_COUNT);
		mCurrentCount = mPrefs.getInt("currentcount", 0);
		mCountDown = mPrefs.getInt("countdown", DEFAULT_COUNT_DOWN);
		mLogFileName = mPrefs.getString("logfile", "");

		Log.i(TAG, "Total:" + mTotalCount + " Current:" + mCurrentCount
				+ " CountDown:" + mCountDown);
	}
	
	@SuppressLint("SimpleDateFormat")
	private void updateLog() {
		FileWriter result = null;
		try {
			File tempf = new File(LogUtil.PATH + "otatemp");
			if(tempf.exists()) {
				FileReader temp = new FileReader(tempf);
				BufferedReader bufferedReader = new BufferedReader(temp);
				String line = bufferedReader.readLine();
				long start_time = Long.parseLong(line);
				long end_time = System.currentTimeMillis();
				if(!mLogFileName.isEmpty()) {
					result = new FileWriter(new File(LogUtil.PATH
							+ this.mLogFileName), true);
					SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss ");
					String timestamp = format.format(new Date());
					result.append(timestamp+"\t" + mCurrentCount + "\t" + ((end_time - start_time)/1000) + "\n");
					result.flush();
					result.close();
				}
				temp.close();
				tempf.delete();
			}
		} catch (IOException e1) {
			Log.e(TAG, "IOException:" + e1);
		}
	}

	@SuppressLint("HandlerLeak")
	private Handler myHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLE_MSG_COUNT_DOWN:
				showMsgText();
				break;
			case HANDLE_MSG_OTA_FAIL:
				OTAProgress.setText("OTA Fail!");
				break;
			case RECOVERY_VFY_PROGRESS:
				if (msg.arg1 < 100) {
					OTAProgress.setText(OTAActivity.this.getResources()
							.getString(R.string.repeat_OTA_verify_progress)
							+ Integer.toString(msg.arg1) + "%");
				} else {
					OTAProgress.setText(R.string.repeat_OTA_verify_finish);
				}
				break;
			case RECOVERY_VFY_FAILED:
				OTAProgress.setText(R.string.repeat_OTA_verify_fail);
				break;
			case RECOVERY_CPY_PROGRESS:
				if (msg.arg1 < 100) {
					OTAProgress.setText(OTAActivity.this.getResources()
							.getString(R.string.repeat_OTA_copy_progress)
							+ Integer.toString(msg.arg1) + "%");
				} else {
					OTAProgress.setText(R.string.repeat_OTA_copy_finish);
				}
				break;
			case RECOVERY_CPY_FAILED:
				OTAProgress.setText(R.string.repeat_OTA_copy_fail);
				break;
			case RECOVERY_PROGRESS:
				OTAProgress.setText(msg.arg1 + "%");
				break;
			case RECOVERY_OTA_START:
				startButton.setEnabled(false);
				stopButton.setEnabled(false);
				break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		setRequestedOrientation(ScreenUtil.getScreenOrientation(this));
		setContentView(R.layout.repeat_ota_layout);

		totalTestCount = (EditText) this.findViewById(R.id.TotalTestCount);
		idleTime = (EditText) this.findViewById(R.id.IdleTime);
		startButton = (Button) this.findViewById(R.id.ota_start_button);
		stopButton = (Button) this.findViewById(R.id.ota_stop_button);

		remainCount = (TextView) this.findViewById(R.id.RemainCount);
		remainText = (TextView) this.findViewById(R.id.RemainText);

		countDownTime = (TextView) this.findViewById(R.id.CountDownTime);
		OTAProgress = (TextView) this.findViewById(R.id.OTAProgress);
		
		getOTAConfig();
		updateLog();
		mCurrentCount++;

		// finish all the test or just start
		if (mCurrentCount > mTotalCount || mCurrentCount == 1) {
			startButton.setEnabled(true);
			stopButton.setEnabled(false);
			mIsRunning = false;
			if (mCurrentCount > mTotalCount) {
				remainText.setText(R.string.repeat_OTA_finish_message);
				remainCount.setText(Long.toString(mTotalCount));
			}
		} else {
			if (!isOTASuccess()) {
				startButton.setEnabled(false);
				stopButton.setEnabled(false);
				mIsRunning = false;
				OTAProgress.setText(R.string.repeat_OTA_fail);
			} else {
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
				mIsRunning = true;
				showMsgText();
			}
		}

		// a confirm dialog for stopping
		alertDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.DialogTitle)
				.setMessage(R.string.DialogMessage)
				.setPositiveButton(R.string.Yes,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mIsRunning = false;
								showMsgText();
								SharedPreferences.Editor localEditor = mPrefs.edit();
								mCurrentCount = 0;
								localEditor.putInt("currentcount", mCurrentCount);
								localEditor.commit();
								startButton.setEnabled(true);
								stopButton.setEnabled(false);
								Log.i(TAG, " yes press.");
							}
						})
				.setNegativeButton(R.string.No,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Log.i(TAG, " no press.");
							}
						}).create();

		Log.i(TAG, "creating OTA monitor thread...");

		Thread countDownThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (mIsRunning) {
						if (mCountDown > 0) {
							Message message = new Message();
							message.what = HANDLE_MSG_COUNT_DOWN;
							mCountDown--;
							Log.i(TAG, "mCountDown=" + mCountDown);
							myHandler.sendMessage(message);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
						if (mCountDown == 0)
							break;
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
				}
				if (!mIsRunning) {
					Log.i(TAG, " thread exiting......");
					return;
				}
				// record the current factory reset count to a file
				if (mCountDown == 0) {
					Log.i(TAG, "Start OTA Test");
					Message message = new Message();
					message.what = RECOVERY_OTA_START;
					myHandler.sendMessage(message);
					
					if (startOTAUpgrade() == false) {
						message = new Message();
						message.what = HANDLE_MSG_OTA_FAIL;
						Log.e(TAG, "OTA Fail!!!");
						myHandler.sendMessage(message);
					}
				}
			}
		});
		countDownThread.start();

		startButton.setOnClickListener(new Button.OnClickListener() {
			@SuppressLint("SimpleDateFormat")
			public void onClick(View v) {
				
				Log.i(TAG, "debugprint");
				/* check if update.zip exists in /data/update.zip */
				File updateZipF = new File("/data/update.zip");
				if(!updateZipF.exists() || !updateZipF.canRead()) {
					Dialog alertDialog = new AlertDialog.Builder(OTAActivity.this)
					.setTitle(R.string.repeat_OTA_error)
					.setMessage(R.string.repeat_OTA_no_update_file)
					.setPositiveButton(
							OTAActivity.this.getResources().getString(R.string.confirm),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).create();
					alertDialog.show();
					return;
				}
				mCountDown = Integer.parseInt(idleTime.getText().toString());
				mTotalCount = Integer.parseInt(totalTestCount.getText()
						.toString());

				mCurrentCount = 1;
				Log.i(TAG, "countdown=" + mCountDown + "    totalnumbers"
						+ mTotalCount);
				
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
				String str = format.format(new Date());
		        mLogFileName = "OTAResult-"+str;

				SharedPreferences.Editor localEditor = mPrefs.edit();
				localEditor.putInt("totalcount", mTotalCount);
				localEditor.putInt("currentcount", mCurrentCount);
				localEditor.putInt("countdown", mCountDown);
				localEditor.putInt("autoboot", MainActivity.AUTO_BOOT_OTA);

				localEditor.commit();

				mIsRunning = true;
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
			}
		});
		stopButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				alertDialog.show();
			}
		});
        if (!mIsRunning) {
            Intent intent = getIntent();
            boolean autoStart = intent.getBooleanExtra(MainActivity.IS_AUTO_FLAG,
                false);
            if (autoStart)
                startButton.performClick();
        }
	}

	/*
	 * for RecoveryProgress methods
	 */
	/*
	 * @Override public void onVerifyProgress(int progress) { Message message =
	 * new Message(); message.what = HANDLE_MSG_VFY_PROGRESS; message.arg1 =
	 * progress; myHandler.sendMessage(message); }
	 * 
	 * @Override public void onVerifyFailed(int errorCode, Object object) {
	 * Message message = new Message(); message.what = RECOVERY_VFY_FAILED;
	 * myHandler.sendMessage(message); }
	 * 
	 * @Override public void onCopyProgress(int progress) { Message message =
	 * new Message(); message.what = HANDLE_MSG_CPY_PROGRESS; message.arg1 =
	 * progress; myHandler.sendMessage(message); }
	 * 
	 * @Override public void onCopyFailed(int errorCode, Object object) {
	 * Message message = new Message(); message.what = RECOVERY_CPY_FAILED;
	 * myHandler.sendMessage(message); }
	 */

	@Override
	public void onProgress(int progress) {
		Message message = new Message();
		message.what = RECOVERY_PROGRESS;
		message.arg1 = progress;
		myHandler.sendMessage(message);
	}
}
