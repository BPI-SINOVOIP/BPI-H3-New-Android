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

public class WipedataActivity  extends Activity {
	private static final String TAG = "RebootTest";
    private static final String LOGFILE="/cache/recovery/last_wipedata";
    private static final String LOGDIR="/cache/recovery";
	@SuppressLint("SdCardPath")
	private static final int DEFAULT_REBOOT_COUNT = 10000;
	private static final int DEFAULT_IDLE_TIME = 5;


	private static final int COUNT_DOWN = 0;
	private static final int WIPEDATA = 1;


	private TextView testStatusText;
	private TextView countDownNumText;
    private TextView currentRebootNumText;
	private Button stopBtn, startBtn;
	private int wipedataCount = -1;
	private int timers=DEFAULT_IDLE_TIME;

	private int mState;
	private int timersold;

	private Thread rebootThread = null;
	private String logFileName = null;
	private PowerManager.WakeLock lock = null;

	private volatile boolean rebootRunning = true;
	private volatile int startDelay = 0;

	public static int getWipedataTimes() {
        int times = -1;
		try {
			File logFile = new File(LOGFILE);
			if(logFile.exists()) {
				FileReader fr = new FileReader(logFile);
				BufferedReader bufferedReader = new BufferedReader(fr);
				String line = bufferedReader.readLine();
				if(line != null) {
					try {
						times = Integer.parseInt(line);
				} catch (NumberFormatException e2) {
						Log.e(TAG, "Invalid number format!!");
					}
				}

				fr.close();
			}
		} catch (IOException e1) {
			Log.i(TAG, "IOException:" + e1);
		}
        return times;
	}

	private void logAndWipedata() {
        File logDir = new File(LOGDIR);
        if (!(logDir.mkdirs() || logDir.isDirectory()))
        {
            Log.e(TAG,"something wrong with "+LOGDIR);
            return;
        }
		FileWriter fw = null;
        try {
            fw = new FileWriter(LOGFILE, false);
            int tmp=wipedataCount+1;
            fw.write(String.valueOf(tmp));
        } catch (Exception e) {
            System.out.println(e.toString());
            return;
        } finally {
            if (fw != null)
                try {
                    fw.close();
                } catch (IOException e) {
                    throw new RuntimeException("wtf:" + LOGFILE + " can not close");
                }
        }
        sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case COUNT_DOWN:
                timers--;
		        countDownNumText.setText(String.valueOf(timers));
                if (timers<1)
                    this.sendEmptyMessage(WIPEDATA);
                else
                    this.sendEmptyMessageDelayed(COUNT_DOWN,1000);
				break;
            case WIPEDATA:
                logAndWipedata();
                break;
            default:
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
		setContentView(R.layout.repeat_wipedata_layout);

		testStatusText = (TextView) findViewById(R.id.test_status_text);
		currentRebootNumText = (TextView) findViewById(R.id.current_wipedata_num_text);
		countDownNumText = (TextView) findViewById(R.id.count_down_num_text);
		stopBtn = (Button) findViewById(R.id.wipedata_stop_button);
		startBtn = (Button) findViewById(R.id.wipedata_start_button);


		wipedataCount=getWipedataTimes();
        if (wipedataCount<0)
		{
            currentRebootNumText.setText(String.valueOf(0));
   			startBtn.setEnabled(true);
			stopBtn.setEnabled(false);
	    }
        else
        {
			startBtn.setEnabled(false);
			stopBtn.setEnabled(true);
            currentRebootNumText.setText(String.valueOf(wipedataCount));
            mHandler.sendEmptyMessageDelayed(COUNT_DOWN,1000);
        }
		//PATH = ((MainActivity)this.getActivity()).getPath();
		FileWriter result = null;

		startBtn.setOnClickListener(new Button.OnClickListener() {
			@SuppressLint("SimpleDateFormat")
			public void onClick(View v) {

                if (wipedataCount<0)
                    wipedataCount=0;
                timers=DEFAULT_IDLE_TIME;
		        countDownNumText.setText(String.valueOf(timers));
                mHandler.sendEmptyMessageDelayed(COUNT_DOWN,1000);
				startBtn.setEnabled(false);
				stopBtn.setEnabled(true);
			}
		});

		stopBtn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
                mHandler.removeMessages(COUNT_DOWN);
                cleanFiles();
				startBtn.setEnabled(true);
				stopBtn.setEnabled(false);
			}
		});

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "destroy");
	}

	private void cleanFiles() {
		File tempf = new File(LOGFILE);
		if(tempf.exists()) {
			tempf.delete();
		}
	}
}
