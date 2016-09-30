package com.softwinner.dragonbox.platform;

import java.lang.reflect.InvocationTargetException;

import android.content.Context;
import android.net.ethernet.EthernetManager;
import android.os.Build;

public class CommonPlatformUtil {

	private static final int[] sA20_CPU_FREQ_STEP = new int[] { 60000, 132000,
			288000, 408000, 528000, 600000, 720000, 768000, 864000, 912000,
			1008000, 1056000 };
	private static final int[] sA31_CPU_FREQ_STEP = new int[] { 120000, 132000,
			288000, 408000, 528000, 600000, 720000, 768000, 864000, 912000,
			1008000, 1056000, 1200000};
	private static final int[] sA10_CPU_FREQ_STEP = new int[] { 60000, 132000,
			288000, 408000, 528000, 600000, 720000, 768000, 864000, 912000,
			1008000, 1056000 };
	private static final int[] sA80_CPU_FREQ_STEP = new int[] { 60000, 132000,
			288000, 408000, 528000, 600000, 720000, 768000, 864000, 912000,
			1008000, 1056000, 1296000, 1440000, 1536000, 1608000, 1800000};

	public static int[] getCpuFreqStep() {
		String platform = Build.HARDWARE;
		if (platform.equals("sun6i")) {
			return sA31_CPU_FREQ_STEP;
		} else if (platform.equals("sun7i")) {
			return sA20_CPU_FREQ_STEP;
        } else if (platform.equals("sun9i")) {
			return sA80_CPU_FREQ_STEP;
		} else {
			return sA10_CPU_FREQ_STEP;
		}
	}

	public static EthernetManager getEthernetManager(Context context){
		String platform = Build.HARDWARE;
		if (platform.equals("sun6i") || platform.equals("sun7i") || platform.equals("sun9i")) {
			try {
				return (EthernetManager) EthernetManager.class.getMethod("getInstance").invoke(null);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {//sun5i"
			return (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
		}
		return null;
	}
	
	public static int checkLink(EthernetManager ethManager, String typeStr) {
		String platform = Build.HARDWARE;
		String method = "";
		if (platform.equals("sun6i") || platform.equals("sun9i")) {
			method = "checkLink";
		} else {//"sun7i,sun5i"
			method = "CheckLink";
		}
		try {
			return (Integer) EthernetManager.class.getDeclaredMethod(method,
					String.class)
					.invoke(ethManager, typeStr);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
}
