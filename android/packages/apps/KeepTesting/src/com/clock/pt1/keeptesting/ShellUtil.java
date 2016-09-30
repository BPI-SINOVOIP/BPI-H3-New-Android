package com.clock.pt1.keeptesting;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import android.util.Log;

public class ShellUtil {
	private final static String TAG = "ShellUtil";
	public static boolean isDeviceRooted() {
		Process p = null;
		DataOutputStream dos = null;
		BufferedReader dis = null;
		String line = null;
		try {
			p = Runtime.getRuntime().exec("/system/xbin/su");
			
			dos = new DataOutputStream(p.getOutputStream());
			dis = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			dos.writeBytes("id\n");
			dos.flush();
			
			while ((line = dis.readLine()) != null) {
				if(line.contains("uid=0")) {
					dos.writeBytes("exit\n");
					dos.flush();
					Log.v(TAG,"device is rooted");
					return true;
				}
			}
			Log.v(TAG,"check rooted end!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}
	
	public static void runRootCmdWaitFinish(String command) {
		Process p = null;
		DataOutputStream dos = null;

		try {
			p = Runtime.getRuntime().exec("/system/xbin/su");
			dos = new DataOutputStream(p.getOutputStream());

			Log.v(TAG,"running root command: "+command);
			dos.writeBytes(command+"\n");
			dos.flush();
			
			dos.writeBytes("exit\n");
			dos.flush();

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
