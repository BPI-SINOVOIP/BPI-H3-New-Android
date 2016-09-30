package com.clock.pt1.keeptesting;

import java.util.List;

import android.app.ActivityManager;
import android.content.Context;

public class ServiceUtil {
	public static boolean isServiceRunning(Context context, String serviceName) {
		boolean isRunning = false;
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager
				.getRunningServices(30);
		if (!(serviceList.size() > 0)) {
			return false;
		}
		for (int i = 0; i < serviceList.size(); i++) {
			if (serviceList.get(i).service.getClassName().equals(serviceName) == true) {
				isRunning = true;
				break;
			}
		}
		return isRunning;
	}
}
