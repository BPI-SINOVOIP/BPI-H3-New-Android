package com.softwinner.utils;

import java.lang.String;
import android.util.Log;

/**
 * Class that provides some api to get system config.
 *
 * {@hide}
 */
public class Config{
    public static final int TARGET_BOARD_PLATFORM = 0x00;       //IC平台类型
    public static final int TARGET_BUSINESS_PLATFORM = 0x01;    //业务平台类型
    public static final int PLATFORM_JAWS = 0x00;         //定义为A80
    public static final int PLATFORM_FIBER = 0x01;        //定义为A31
    public static final int PLATFORM_EAGLE   = 0x02;        //for H8
    public static final int PLATFORM_DOLPHIN = 0x03;        //for H3
    public static final int PLATFORM_CMCCWASU = 0x08;     //定义为移动华数
    public static final int PLATFORM_ALIYUN = 0x09;       //定义为aliyun
    public static final int PLATFORM_TVD = 0x0A;          //定义为数码
    public static final int PLATFORM_IPTV = 0x0B;          //IPTV
    public static final int PLATFORM_UNKNOWN = 0xFF;      //未知平台
    
    static {
		System.loadLibrary("config_jni");
	}
	
	public static int getTargetPlatform(int platformType){
	    return nativeGetTargetPlatform(platformType);    
	}
	
	private static native int nativeGetTargetPlatform(int type);
}
