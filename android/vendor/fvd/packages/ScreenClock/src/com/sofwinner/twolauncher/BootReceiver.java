package com.sofwinner.twolauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class BootReceiver extends BroadcastReceiver {  
	private static final String TAG = "BootReceiver";
    @Override  
    public void onReceive(Context context, Intent intent) {  
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {     // boot  
        	Log.d(TAG,"BootReceiver complete");
            Intent intent2 = new Intent(context, MainActivity.class);  
//          intent2.setAction("android.intent.action.MAIN");  
//          intent2.addCategory("android.intent.category.LAUNCHER");  
            intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
            context.startActivity(intent2);  
        }  
    }
}  
