package com.clock.pt1.keeptesting.orientationtester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.clock.pt1.keeptesting.LogUtil;
import com.clock.pt1.keeptesting.R;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;

public class OrientationService extends Service {

	private enum OrientationMode {
		ORI_CLOCKWISE_90,
		ORI_COUNTERCLOCKWISE_90,
		ORI_180,
		ORI_RANDOM
	};
	
	private static final int NOTIFICATION_ID = 1000;
	private static final int DEFAULT_DELAY_AFTER_START = 10000;
	private WindowManager mWindow;
	private CustomLayout mLayout;

	private boolean flag = true;
	
	//4 directions
	private int[] orientation = 
		{ ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, 
		  ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, 
		  ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, 
		  ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT};
	private int orientationIndex = 0;
	private OrientationMode mode = OrientationMode.ORI_RANDOM;

	private Notification notification;
	private NotificationManager notificationManager;
	private StopServiceReceiver receiver = null;
	
	// we need some default value here, for some devices
	// with low memory, this service will be killed and restarted
	// by the system.
	private int peroid = 3000;
	private String numOfDegree = null;
	private String direction = null;
	private int remainNum = 2500;
	private int totalNum = 2500;
	
	private View oldView = null;
	


	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		receiver = new StopServiceReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.clock.pt1.keeptesting.orientationtester.STOP_SERVICE");
		this.registerReceiver(receiver, filter);
	}

	public class StopServiceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			NotificationManager notificationManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			Log.i(OrientationTestActivity.TAG, "stopping service");
			notificationManager.cancel(NOTIFICATION_ID);
			// 获得提示信息
			String value = intent.getStringExtra("key");
			if (value != null && value.equals("stop"))
				OrientationService.this.stopSelf();
		}
	}

	public void onStart(Intent intent, int startId) {

		// create a notification
		notificationManager = (NotificationManager) getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent1 = new Intent(this, OrientationTestActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent1,
				Notification.FLAG_AUTO_CANCEL);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this);
		builder.setContentIntent(pIntent);
		builder.setSmallIcon(android.R.drawable.ic_menu_info_details);
		RemoteViews remoteView = new RemoteViews(getPackageName(),
				R.layout.orientation_notification_layout);
		Intent active = new Intent("com.clock.pt1.keeptesting.orientationtester.STOP_SERVICE");
		active.putExtra("key", "stop");
		PendingIntent pintent2 = PendingIntent.getBroadcast(this, 0, active, 0);
		remoteView.setOnClickPendingIntent(R.id.stop_service_button, pintent2);

		notification = builder.build();
		notification.contentView = remoteView;
		notificationManager.notify(NOTIFICATION_ID, notification);

		numOfDegree = this.getResources().getString(R.string.orientation_random);
		direction = this.getResources().getString(R.string.orientation_clockwise);
		if (intent != null) {
			Bundle bunde = intent.getExtras();
			peroid = bunde.getInt("peroid") * 1000;
			numOfDegree = bunde.getString("NumDegree");
			direction = bunde.getString("Direction");
			remainNum = bunde.getInt("Number");
			totalNum = remainNum;
		}
		
		if (numOfDegree.equals(this.getResources().getString(R.string.orientation_random))) {
			mode = OrientationMode.ORI_RANDOM;
		} else if(numOfDegree.equals("90")) {
			if(direction.equals(this.getResources().getString(R.string.orientation_clockwise))) {
				mode = OrientationMode.ORI_CLOCKWISE_90;
			} else {
				mode = OrientationMode.ORI_COUNTERCLOCKWISE_90;
			}
		} else if(numOfDegree.equals("180")) {
			mode = OrientationMode.ORI_180;
		}

		createFloatView();
	}

	@SuppressLint("SimpleDateFormat")
	private void createFloatView() {

		mWindow = (WindowManager) getSystemService(WINDOW_SERVICE);
		Thread mThread = new Thread() {
			@Override
			public void run() {
				try {
					sleep(DEFAULT_DELAY_AFTER_START);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
				String str = format.format(new Date());
				String logFile = LogUtil.PATH+"OrientationResult"+str;

				while (flag && remainNum > 0) {
					RemoteViews contentView = notification.contentView;
					contentView.setTextViewText(
							R.id.orientation_test_info,
							OrientationService.this.getResources().getString(
									R.string.orientation_test_info)
									+ totalNum + "/" + (totalNum - remainNum));
					notificationManager.notify(NOTIFICATION_ID, notification);
					remainNum--;
					Message msg = new Message();
					msg.what = 0;
					mHandler.sendMessage(msg);

					try {
						sleep(peroid);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					try {
						FileWriter result = null;
						result = new FileWriter(new File(logFile), true);
						SimpleDateFormat timeStampformat = new SimpleDateFormat("MM-dd HH:mm:ss ");
						String timestamp = timeStampformat.format(new Date());
						result.append(timestamp+" "+OrientationService.this.getResources().getString(R.string.orientation_log_count_text) + (totalNum - remainNum) + "\n");
						result.flush();
						result.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		mThread.start();
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (mode == OrientationMode.ORI_CLOCKWISE_90) {
				Log.v(OrientationTestActivity.TAG, "in ORI_CLOCKWISE_90 mode");
				orientationIndex--;
				if (orientationIndex == -1)
					orientationIndex = 3;
			} else if (mode == OrientationMode.ORI_COUNTERCLOCKWISE_90) {
				Log.v(OrientationTestActivity.TAG, "in ORI_COUNTERCLOCKWISE_90 mode");
				orientationIndex++;
				if (orientationIndex == 4)
					orientationIndex = 0;
			}else if (mode == OrientationMode.ORI_180) {
				Log.v(OrientationTestActivity.TAG, "in ORI_180 mode");
				orientationIndex = (orientationIndex == 0) ? 2 : 0;
			}else if (mode == OrientationMode.ORI_RANDOM) {
				Log.v(OrientationTestActivity.TAG, "in ORI_RANDOM mode");
				Random r = new Random();
				int index = 0;
				int limit = 100;
				while (limit-- > 0) {
					index = r.nextInt(4);
					if (index != orientationIndex) {
						break;
					}
				}
				orientationIndex = index;
			}
			if(oldView != null) {
				mWindow.removeView(oldView);
			}
			mLayout = new CustomLayout(orientation[orientationIndex]);
			oldView = new View(OrientationService.this);
			mWindow.addView(oldView, mLayout);
			super.handleMessage(msg);
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		flag = false;
		if(oldView != null) {
			mWindow.removeView(oldView);
		}
		if (receiver != null)
			this.unregisterReceiver(receiver);
	}

	public class CustomLayout extends android.view.WindowManager.LayoutParams {
		public CustomLayout(int paramInt) {
			// TYPE_SYSTEM_OVERLAY: the layer that is on the top of the window
			super(0, 0, TYPE_SYSTEM_OVERLAY, FLAG_FULLSCREEN
					| FLAG_NOT_FOCUSABLE, PixelFormat.RGBX_8888);
			// push object to the top of its container, not changing its size.
			this.gravity = Gravity.TOP;
			// set the screenOrientation as desired
			this.screenOrientation = paramInt;
		}
	}
}
