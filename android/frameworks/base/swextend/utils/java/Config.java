package com.softwinner.utils;

import java.lang.String;
import android.util.Log;

/**
 * Class that provides some api to get system config.
 *
 * {@hide}
 */
public class Config{
    public static final int TARGET_BOARD_PLATFORM = 0x00;       //ICƽ̨����
    public static final int TARGET_BUSINESS_PLATFORM = 0x01;    //ҵ��ƽ̨����
    public static final int PLATFORM_JAWS = 0x00;         //����ΪA80
    public static final int PLATFORM_FIBER = 0x01;        //����ΪA31
    public static final int PLATFORM_EAGLE   = 0x02;        //for H8
    public static final int PLATFORM_DOLPHIN = 0x03;        //for H3
    public static final int PLATFORM_CMCCWASU = 0x08;     //����Ϊ�ƶ�����
    public static final int PLATFORM_ALIYUN = 0x09;       //����Ϊaliyun
    public static final int PLATFORM_TVD = 0x0A;          //����Ϊ����
    public static final int PLATFORM_IPTV = 0x0B;          //IPTV
    public static final int PLATFORM_UNKNOWN = 0xFF;      //δ֪ƽ̨
    
    static {
		System.loadLibrary("config_jni");
	}
	
	public static int getTargetPlatform(int platformType){
	    return nativeGetTargetPlatform(platformType);    
	}
	
	private static native int nativeGetTargetPlatform(int type);
}
