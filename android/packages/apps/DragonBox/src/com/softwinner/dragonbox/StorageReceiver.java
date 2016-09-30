package com.softwinner.dragonbox;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.SystemProperties;

import com.softwinner.dragonbox.config.ConfigManager;

public class StorageReceiver extends BroadcastReceiver {
    private static final String TAG = "StorageReceiver";

	public void onReceive(Context context, Intent intent) {
		if (!ConfigManager.startConfigAPK(context, ConfigManager.CONFIG_DRAGON_BOX, false)) {
			if (!ConfigManager.startConfigAPK(context, ConfigManager.CONFIG_DRAGON_SN, false)) {
				if (!ConfigManager.startConfigAPK(context, ConfigManager.CONFIG_DRAGON_AGING, false)) {
					Log.d(TAG,"config file not exist");
				}
			}
		}
	}
}
