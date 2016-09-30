package com.clock.pt1.keeptesting.batterycontroller;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.clock.pt1.keeptesting.LogUtil;
import com.clock.pt1.keeptesting.R;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Shell;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class BatteryControllerService  extends Service {
	private static final int NOTIFICATION_ID = 1001;
	
	private Shell shell;
	private int lowerBound = BatteryControllerActivity.DEFAULT_LOWER_BOUND;
	private int upperBound = BatteryControllerActivity.DEFAULT_UPPER_BOUND;
	private int batteryLevel = -1;
	private int plugged = -1;
	private int voltage = -1;
	private int preplugged = -1;
	private int prebatteryLevel = -1;
	private volatile boolean turnOnChgen = true;
	private volatile boolean mAction = false;
	private volatile boolean mRunning = true;
	private SharedPreferences prefs = null;
	private ChgenCtrl chgen = null;
	private PowerManager.WakeLock lock = null;
	private BatteryInfoReceiver receiver = null;
	private NotificationManager notificationManager;
	private Notification notification;

	public class BatteryInfoReceiver extends BroadcastReceiver {

		@SuppressLint("SimpleDateFormat")
		@Override
		public void onReceive(Context context, Intent intent) {
			String pluggedString = null;
			String infoString = null;
			
			batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
			voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
			
			// check if need to control the chgen sys node
			// send a notification to the system if
			// 1. battery level reach the lower bound without charger
			// 2. battery level reach the upper bound with charger
			if(batteryLevel < lowerBound) {
				Log.i(BatteryControllerActivity.TAG,"Battery level < lower bound!");
				turnOnChgen = true;
				mAction = true;
			} else if(batteryLevel > upperBound) {
				Log.i(BatteryControllerActivity.TAG,"Battery level > upper bound!");
				turnOnChgen = false;
				mAction = true;
			}
			
			// write the information into log file
			switch(plugged) {
				case BatteryManager.BATTERY_PLUGGED_AC:
					pluggedString = "AC";
					break;
				case BatteryManager.BATTERY_PLUGGED_USB:
					pluggedString = "USB";
					break;
				default:
					pluggedString = "";
					break;
			}
			Log.i(BatteryControllerActivity.TAG,"Battery Level="+batteryLevel+"% Voltage="+voltage+" "+pluggedString);
			// only write log and broadcast message in the following 4 cases
			// 1. plug in USB/AC
			// 2. plug out USB/AC
			// 3. battery level lower then lower bound without charger
			// 4. battery level upper then upper bound with charger
			if(batteryLevel < lowerBound &&
					plugged != BatteryManager.BATTERY_PLUGGED_AC &&
					plugged != BatteryManager.BATTERY_PLUGGED_USB &&
					(prebatteryLevel >= lowerBound || prebatteryLevel == -1)) {
				sendNotification(BatteryControllerService.this.getString(R.string.battery_controller_reach_lower_bound_without_charger));
				infoString = BatteryControllerService.this.getString(R.string.battery_controller_battery_level) + batteryLevel
						+ "%   " + BatteryControllerService.this.getString(R.string.battery_controller_reach_lower_bound_without_charger);
			} else if(batteryLevel > upperBound &&
					(plugged == BatteryManager.BATTERY_PLUGGED_AC ||
					plugged == BatteryManager.BATTERY_PLUGGED_USB) &&
					(prebatteryLevel <= upperBound || preplugged != plugged)) {
				sendNotification(BatteryControllerService.this.getString(R.string.battery_controller_reach_upper_bound_with_charger));
				infoString = BatteryControllerService.this.getString(R.string.battery_controller_battery_level) + batteryLevel
						+ "%   " + BatteryControllerService.this.getString(R.string.battery_controller_reach_upper_bound_with_charger);
			} else if((plugged == BatteryManager.BATTERY_PLUGGED_AC ||
					plugged == BatteryManager.BATTERY_PLUGGED_USB) &&
					plugged != preplugged) {
				infoString = BatteryControllerService.this.getString(R.string.battery_controller_battery_level) + batteryLevel
						+ "%  " + pluggedString
						+ " " + BatteryControllerService.this.getString(R.string.battery_controller_charger_plugin);
			} else if((preplugged == BatteryManager.BATTERY_PLUGGED_AC ||
					preplugged == BatteryManager.BATTERY_PLUGGED_USB) &&
					plugged != preplugged) {
				infoString = BatteryControllerService.this.getString(R.string.battery_controller_battery_level) + batteryLevel
						+ "%  " 
						+ BatteryControllerService.this.getString(R.string.battery_controller_charger_plugout);
			} 
			
			if(infoString != null) {
				FileWriter logfw = null;
				try {
				    logfw = new FileWriter(LogUtil.PATH+BatteryControllerActivity.LOG_FILE_NAME, true);
				    SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss ");
				    infoString = format.format(new Date())+infoString;
				    logfw.append(infoString +"\n");
				    logfw.flush();
				    logfw.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
	
				// broadcast a message to Activity to refresh the log listview
				Intent batteryEventIntent = new Intent();
				batteryEventIntent.setAction(BatteryControllerActivity.BATTERY_EVENT_INTENT);
				batteryEventIntent.putExtra(BatteryControllerActivity.BROADCAST_BATTERY_INFO,infoString);
				BatteryControllerService.this.sendBroadcast(batteryEventIntent);
			}
			preplugged = plugged;
			prebatteryLevel = batteryLevel;
		}
	}
	
	private void sendNotification(String title) {
		Intent intent = new Intent(this, BatteryControllerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
				Notification.FLAG_AUTO_CANCEL);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentIntent(pIntent);
		builder.setSmallIcon(android.R.drawable.ic_menu_info_details);
		builder.setContentTitle(title);

		notification = builder.build();
		notification.defaults = Notification.DEFAULT_SOUND;
		notificationManager.notify(NOTIFICATION_ID, notification);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(BatteryControllerActivity.TAG,"BatteryControllerService + onStat +");
		try {
            shell = RootTools.getShell(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
		
		if (prefs == null) {
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
		
		notificationManager = (NotificationManager) getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		lowerBound = prefs.getInt(BatteryControllerActivity.PREF_LOWER_BOUND,BatteryControllerActivity.DEFAULT_LOWER_BOUND);
		upperBound = prefs.getInt(BatteryControllerActivity.PREF_UPPER_BOUND,BatteryControllerActivity.DEFAULT_UPPER_BOUND);
		mRunning = true;
		chgen = new ChgenCtrl(shell);
		
		Intent currentBatteryIntent = BatteryControllerService.this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		batteryLevel = currentBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		
		receiver = new BatteryInfoReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		this.registerReceiver(receiver, filter);
		startMonitor();
	}

	@Override
	public void onDestroy() {
		mRunning = false;
		if(lock != null && lock.isHeld()) {
			lock.release();
		}
		chgen.setValue(1);
		super.onDestroy();
        try {
            RootTools.closeAllShells();
        } catch (IOException e) {
            e.printStackTrace();
        }
        notificationManager.cancel(NOTIFICATION_ID);
		if (receiver != null)
			this.unregisterReceiver(receiver);
	}
	
	private void startMonitor() {
		Thread monitorThread = new Thread() {
			@Override
			public void run() {
				Log.i(BatteryControllerActivity.TAG,"startMonitor +  +");
				PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);  
				lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "battery_controller");  

				if(lock != null && !lock.isHeld()) {
					lock.acquire();
				}
		        
				if(batteryLevel < lowerBound) {
					turnOnChgen = true;
					mAction = true;
				} else if(batteryLevel > upperBound) {
					turnOnChgen = false;
					mAction = true;
				}
				
				while(mRunning) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					if(mAction) {
						if(turnOnChgen) {
							chgen.setValue(1);
						} else {
							chgen.setValue(0);
						}
						mAction = false;
					}
				}
			}
		};
		monitorThread.start();
	}
}
