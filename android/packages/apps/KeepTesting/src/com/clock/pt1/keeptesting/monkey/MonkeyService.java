package com.clock.pt1.keeptesting.monkey;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.clock.pt1.keeptesting.LogUtil;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.Shell;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class MonkeyService extends Service {

	private static final String LOG_FOLDER = "MonkeyTest";
	private static final String LOG_FILE = "MonkeyEvents";
	private static final long HOURS2MILLS = 60*60*1000;
	private static final int DEFAULT_MODE = MonkeyConfigActivity.BLACK_MODE;
	private static final int  DEFAULT_DELAY = 500;
	private static final boolean DEFAULT_IGNORE_CRASH = false;
	private static final String DEFAULT_DEBUG_LEVEL = "-v -v -v";
	
	private Shell shell;
	private String testFolderPath;
	private int cmdIdx = 1;
	
	// parameter from UI
	private int duration = 12;
	private int mode;
	private int delay;
	private boolean ignoreCrash;
	private String debugLevel;
	private long target = 0;
	private SharedPreferences prefs = null;
	
	private MonkeyCommand monkeyCommand;
	private String baseCommand;
	
	@SuppressLint("SimpleDateFormat")
	class MonkeyCommand extends Command {
        public MonkeyCommand(int id, long timeout, String command) {
			super(id, timeout, command);
		}

		@Override
        public void commandOutput(int id, String line) {
        }

        @Override
        public void commandTerminated(int id, String reason) {
            Log.v(MonkeyConfigActivity.TAG,"command terminated!");
            long t = System.currentTimeMillis();
            Log.v(MonkeyConfigActivity.TAG,"Starting another monkey command millis left:"+(target-t));
            if(target > t) {
				try {
					SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
					String str = format.format(new Date());
					String logFile = testFolderPath+LOG_FILE+str;	
					String completeCommand = baseCommand+(" > "+logFile)+" 2>&1";
					shell.add(new MonkeyCommand(++cmdIdx, target-System.currentTimeMillis(), completeCommand));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } else {
                synchronized (MonkeyService.this) {

                    try {
                        RootTools.closeAllShells();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            	MonkeyService.this.stopSelf();
            }
        }

        @Override
        public void commandCompleted(int id, int exitCode) {

            Log.v(MonkeyConfigActivity.TAG,"command complete!");
            long t = System.currentTimeMillis();
            Log.v(MonkeyConfigActivity.TAG,"Starting another monkey command millis left:"+(target-t));
            if(target > t) {
				try {
					SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
					String str = format.format(new Date());
					String logFile = testFolderPath+LOG_FILE+str;	
					String completeCommand = baseCommand+(" > "+logFile)+" 2>&1";
					shell.add(new MonkeyCommand(++cmdIdx, target-System.currentTimeMillis(), completeCommand));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } else {
                synchronized (MonkeyService.this) {

                    try {
                        RootTools.closeAllShells();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            	MonkeyService.this.stopSelf();
            }
        }
    
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressLint("SimpleDateFormat")
	public void onStart(Intent intent, int startId) {
		// starting service from UI
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			mode = bundle.getInt("mode");
			duration = bundle.getInt("duration");
			target = System.currentTimeMillis()+duration*HOURS2MILLS;
			delay = bundle.getInt("delay");
			ignoreCrash = bundle.getBoolean("ignoreCrash");
			debugLevel = bundle.getString("debugLevel");
			
			//record all the parameters
			if (prefs == null) {
				prefs = PreferenceManager.getDefaultSharedPreferences(this);
			}
			SharedPreferences.Editor localEditor = prefs.edit();
			localEditor.putInt("mode", mode);
			localEditor.putLong("target", target);
			localEditor.putInt("delay", delay);
			localEditor.putBoolean("ignoreCrash", ignoreCrash);
			localEditor.putString("debugLevel", debugLevel);
			String str = format.format(new Date());
			testFolderPath = LogUtil.PATH+(LOG_FOLDER+str);
			localEditor.putString("testFolderPath", testFolderPath);
			localEditor.commit();
			Log.i(MonkeyConfigActivity.TAG,"Starting service by UI");
		// service is restarted by system
		} else {
			if (prefs == null) {
				prefs = PreferenceManager.getDefaultSharedPreferences(this);
			}
			mode = prefs.getInt("mode",DEFAULT_MODE);
			target = prefs.getLong("target", System.currentTimeMillis()+12*HOURS2MILLS);
			delay = prefs.getInt("delay", DEFAULT_DELAY);
			ignoreCrash = prefs.getBoolean("ignoreCrash", DEFAULT_IGNORE_CRASH);
			debugLevel = prefs.getString("debugLevel", DEFAULT_DEBUG_LEVEL);
			testFolderPath = prefs.getString("testFolderPath", LogUtil.PATH+(LOG_FOLDER+format.format(new Date())));
			Log.i(MonkeyConfigActivity.TAG,"Starting service by system");
		}

		Log.i(MonkeyConfigActivity.TAG,"start monkey loop - target:"+target+" current:"+System.currentTimeMillis()+" mode:"+mode+" delay:"+delay+" ignoreCrash:"+ignoreCrash+" debugLevel:"+debugLevel);
		runMonkey();
	}

	@SuppressLint("SimpleDateFormat")
	private void runMonkey() {
		Thread mThread = new Thread() {
			@Override
			public void run() {
				
				/* create a folder for this test */
				File testFolder = new File(testFolderPath);
				String logFile = "";
				if(!testFolder.exists()) {
					testFolder.mkdir();
				}
				testFolderPath += "/";
				
				// if the service is restarted by system, a previous monkey task may running
				// Let wait until it terminated, then start another. Or else multiple monkey
				// will run at the same time.
				while(RootTools.isProcessRunning("com.android.commands.monkey")) {
					try {
						Log.i(MonkeyConfigActivity.TAG,"waiting 60s for previous monkey task to finish");
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				SimpleDateFormat format = new SimpleDateFormat(
						"yyyyMMdd-HHmmss");
				String str = format.format(new Date());
				logFile = testFolderPath + LOG_FILE + str;

				// build the command
				baseCommand = "monkey";
				String completeCommand;
				baseCommand += (" "+debugLevel);
				if(mode == MonkeyConfigActivity.BLACK_MODE) {
					baseCommand += (" --pkg-blacklist-file "+MonkeyConfigActivity.BLACK_LIST);
				} else {
					baseCommand += (" --pkg-whitelist-file "+MonkeyConfigActivity.WHITE_LIST);
				}
				baseCommand += (" --throttle "+delay);
				if(ignoreCrash) {
					baseCommand += (" --ignore-crashes");
				}
				baseCommand += (" --hprof --ignore-timeouts --pct-anyevent 0 100000");
				completeCommand = baseCommand+(" > "+logFile+" 2>&1");
				Log.i(MonkeyConfigActivity.TAG,"Running monkey command: "+completeCommand);
				
				try {
	                shell = RootTools.getShell(true);
	
	                monkeyCommand = new MonkeyCommand(cmdIdx, target-System.currentTimeMillis(), completeCommand);
	
	                shell.add(monkeyCommand);

	            } catch (Exception e) {
	                e.printStackTrace();
	            }
			}
		};
		mThread.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        try {
            RootTools.closeAllShells();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}

}
