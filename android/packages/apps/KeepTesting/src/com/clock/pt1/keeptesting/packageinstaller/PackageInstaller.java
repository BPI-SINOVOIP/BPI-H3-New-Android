package com.clock.pt1.keeptesting.packageinstaller;

import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageManager;

public class PackageInstaller {
	public static void installPackage(String path) {
		Process p = null;
		
		String cmd = "pm install -r "+path;
		
		try {
			p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static boolean isPackageInstalled(Context context,String packageName) {
		PackageManager pm = context.getPackageManager();
		boolean installed =false;
	    try {  
	        pm.getPackageInfo(packageName,PackageManager.GET_ACTIVITIES);  
	        installed =true;  
	    } catch(PackageManager.NameNotFoundException e) {  
	        installed =false;  
	    }  
	    return installed;
	}
}
