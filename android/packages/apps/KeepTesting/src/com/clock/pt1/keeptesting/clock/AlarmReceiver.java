package com.clock.pt1.keeptesting.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class AlarmReceiver extends BroadcastReceiver {
	public static final String ALARM_RECEIVE_ACTION = "com.pt1.keeptesting.alarmreceive";
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(); 
		i.setAction(ALARM_RECEIVE_ACTION);
		context.sendBroadcast(i);
		Log.i(SleepAndWakeUpActivity.TAG,"alarmReceiver arrive!");
    }
}