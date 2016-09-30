package com.clock.pt1.keeptesting;

import android.annotation.SuppressLint;
import java.io.File;

@SuppressLint("SdCardPath")
public class LogUtil {
	public static final String PATH = "/sdcard/Keeptesting/";
	public static void prepareFolder() {
		File logFolder = new File(PATH);
		if(!logFolder.exists()) {
			logFolder.mkdirs();
		}
	}
}
