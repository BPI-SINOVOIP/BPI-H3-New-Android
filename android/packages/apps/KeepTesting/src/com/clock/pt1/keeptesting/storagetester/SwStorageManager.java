package com.clock.pt1.keeptesting.storagetester;

import java.io.File;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

public class SwStorageManager {

	private static final String TAG = "SwStorageManager";

	private StorageManager mStorageManager;
	private StorageObserver mStorageObserver;
	private Context mContext;
	private ArrayList<Object> mVolumes;
	private ArrayList<OnVolumeChangeListener> mListeners;

	private SwStorageManager(Context context) {
		mStorageManager = (StorageManager) context
				.getSystemService(Context.STORAGE_SERVICE);
		mStorageObserver = new StorageObserver();
		mVolumes = new ArrayList<Object>();
		mListeners = new ArrayList<OnVolumeChangeListener>();
		mContext = context;

		startObserve();

		Object[] volumes = Volume.getVolumeList(mStorageManager);
		for (Object v : volumes) {
			Log.v(TAG, "volumes:" + Volume.getPath(v));
			if (!Environment.MEDIA_MOUNTED.equals(Volume
					.getVolumeState(mStorageManager,Volume.getPath(v)))) {
				continue;
			}
			mVolumes.add(v);
		}
	}

	private static SwStorageManager mInstance;

	public static final SwStorageManager getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new SwStorageManager(context);
		}
		return mInstance;
	}

	public static final void delInstance() {
		if(mInstance != null){
			mInstance.cancelObserve();
			mInstance.unregisterAll();
		}
		mInstance = null;
	}
	
	public ArrayList<Object> getMountedVolume() {
		ArrayList<Object> ret = new ArrayList<Object>();
		synchronized (mVolumes) {
			ret.addAll(mVolumes);
		}
		return ret;
	}

	private void addVolume(Object volume) {
		for (Object v : mVolumes) {
			if (Volume.getPath(v).equals(Volume.getPath(volume))) {
				return;
			}
		}
		mVolumes.add(volume);
		perfromAddListener(volume);
	}

	private void delVolume(Object volume) {
		for (Object v : mVolumes) {
			if (Volume.getPath(v).equals(Volume.getPath(volume))) {
				mVolumes.remove(v);
				perfromDelListener(volume);
			}
		}
	}

	private void perfromAddListener(Object v) {
		for (OnVolumeChangeListener l : mListeners) {
			l.onVolumeAdd(v);
		}
	}

	private void perfromDelListener(Object v) {
		for (OnVolumeChangeListener l : mListeners) {
			l.onVolumeDel(v);
		}
	}

	public void registerListener(OnVolumeChangeListener l) {
		mListeners.add(l);
	}

	public void unregisterListener(OnVolumeChangeListener l) {
		mListeners.remove(l);
	}
	
	public void unregisterAll(){
		mListeners.clear();
	}

	private void startObserve() {
		Log.v(TAG,"start observe");
		IntentFilter filter = new IntentFilter();
		filter.addDataScheme("file");
		filter.setPriority(1000);
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		mContext.registerReceiver(mStorageObserver, filter);
	}
	
	public static final ArrayList<String> getMountedPartitionList(String devPath) {
		File file = new File(devPath);
		String minor = null;
		String major = null;
		String[] list = file.list();
		if (list == null) {
			return null;
		}
		if (list.length > 0 && list.length <= 10) {
			for (String listItem : list) {
				int index = listItem.lastIndexOf("_");
				if (index != -1 && index != (listItem.length() - 1)) {
					major = listItem.substring(0, index);
					minor = listItem.substring(index + 1, listItem.length());
					try {
						Integer.valueOf(major);
						Integer.valueOf(minor);
					} catch (NumberFormatException e) {
						return null;
					}
				} else {
					return null;
				}
			}
			ArrayList<String> partitionList = new ArrayList<String>();
			for (String listItem : list) {
				partitionList.add(listItem);
			}
			return partitionList;
		}
		return null;
	}

	private void cancelObserve() {
		Log.v(TAG,"cancel observe");
		mContext.unregisterReceiver(mStorageObserver);
	}

	private class StorageObserver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.v(TAG, "get a new action:" + action);
			if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
				String path = intent.getData().getPath();
				Object[] volumes = Volume.getVolumeList(mStorageManager);
				for (Object volume : volumes) {
					if (Volume.getPath(volume).equals(path)) {
						addVolume(volume);
					}
				}
			} else {
				String path = intent.getData().getPath();
				Object[] volumes = Volume.getVolumeList(mStorageManager);
				for (Object volume : volumes) {
					if (Volume.getPath(volume).equals(path)) {
						delVolume(volume);
					}
				}
			}
		}
	}

	public interface OnVolumeChangeListener {
		public void onVolumeAdd(Object v);

		public void onVolumeDel(Object v);
	}
}
