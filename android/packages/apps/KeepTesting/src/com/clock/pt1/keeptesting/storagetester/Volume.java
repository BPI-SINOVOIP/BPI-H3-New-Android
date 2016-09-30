package com.clock.pt1.keeptesting.storagetester;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.clock.pt1.keeptesting.ShellUtil;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;

public class Volume {
	/*add this method so that we can compile this project in a standard ADT environment*/
	public static Object[] getVolumeList(StorageManager sm) {
		Class<StorageManager> sc = StorageManager.class;
		Method getVolumeListM;
		Object[] result = null;
		try {
			getVolumeListM = sc.getMethod("getVolumeList");
			result = (Object[])getVolumeListM.invoke(sm);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public static String getVolumeState(StorageManager sm,String path) {
		Class<StorageManager> sc = StorageManager.class;
		Method getVolumeStateM;
		String result = null;
		try {
			getVolumeStateM = sc.getMethod("getVolumeState",String.class);
			result = (String)getVolumeStateM.invoke(sm,path);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public static String getPath(Object v) {
		Class<?> sv;
		Method getPathM;
		String result = null;
		try {
			sv = Class.forName("android.os.storage.StorageVolume");
			getPathM = sv.getMethod("getPath");
			result = (String) getPathM.invoke(v);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public static ArrayList<Object> getStorageVolumes(Context context) {
		StorageManager mStorageManager = (StorageManager) context
				.getSystemService(Context.STORAGE_SERVICE);
		Object[] volumes = getVolumeList(mStorageManager);
		ArrayList<Object> volumeLists = new ArrayList<Object>();
		for(int i = 0; i < volumes.length; i++) {
			if (!Environment.MEDIA_MOUNTED.equals(getVolumeState(mStorageManager,getPath(volumes[i])))) {
				continue;
			}
			volumeLists.add(volumes[i]);
		}
		return volumeLists;
	}
	
	public static ArrayList<String> getStorageVolumePaths(Context context) {
		ArrayList<Object> volumes = getStorageVolumes(context);
		StorageManager mStorageManager = (StorageManager) context
				.getSystemService(Context.STORAGE_SERVICE);
		ArrayList<String> volumePath = new ArrayList<String>();
		
		
		for (Object v : volumes) {
			Log.v("StorageTester", "volumes:" + getPath(v));
			if (!Environment.MEDIA_MOUNTED.equals(getVolumeState(mStorageManager,getPath(v)))) {
				continue;
			}
			volumePath.add(getPath(v));
		}
		
		return volumePath;
	}
	
	@SuppressWarnings("deprecation")
	public static long getVolumeFreeSpace(String path, Context context) {
		ArrayList<Object> volumes = getStorageVolumes(context);
		for(int i = 0; i < volumes.size(); i++) {
			Object volume = volumes.get(i);
			if(path.equals(getPath(volume))) {
				StatFs stat = new StatFs(getPath(volume));
				long blockSize = stat.getBlockSize();
				return stat.getAvailableBlocks() * blockSize;
			}
		}
		return 0;
	}
	
	public static void cleanPageCache() {
		ShellUtil.runRootCmdWaitFinish("echo 1 > /proc/sys/vm/drop_caches");
	}
}
