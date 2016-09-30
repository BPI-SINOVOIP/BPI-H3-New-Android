package com.clock.pt1.keeptesting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	private SharedPreferences prefs;

	public void onReceive(Context paramContext, Intent paramIntent) {

        if (WipedataActivity.getWipedataTimes()>0) {
            Intent intent = new Intent(paramContext, WipedataActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			paramContext.startActivity(intent);
        }
        else {
		int autoboot = 0;
		Class<?> cls = null;
		prefs = PreferenceManager.getDefaultSharedPreferences(paramContext);
		autoboot = prefs.getInt("autoboot",MainActivity.AUTO_BOOT_MAIN);

		switch(autoboot) {
			case MainActivity.AUTO_BOOT_MAIN:
				break;
			case MainActivity.AUTO_BOOT_REBOOT:
				cls = RebootActivity.class;
				break;
			case MainActivity.AUTO_BOOT_OTA:
				cls = OTAActivity.class;
				break;
			default:
				break;
		}
		
		Log.v("Keeptesting boot receiver", "reboot reciver:"+autoboot);
		if(cls != null) {
			Intent localIntent = new Intent(paramContext, cls);
			localIntent.setFlags(268435456);
			paramContext.startActivity(localIntent);
		}
        }
	}
}

