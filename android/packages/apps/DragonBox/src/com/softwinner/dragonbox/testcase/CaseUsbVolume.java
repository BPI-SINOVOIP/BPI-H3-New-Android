package com.softwinner.dragonbox.testcase;

import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.TextView;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.entity.StorageInfo;
import com.softwinner.dragonbox.manager.StorageInfoManager;

public class CaseUsbVolume extends IBaseCase {
	private StorageInfoManager mStorageInfoManager;

	TextView mMaxMountStatus;
	TextView mMaxRWStatus;

	TextView mMinMountStatus;
	TextView mMinRWStatus;

	private BroadcastReceiver mBroadCastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
				updateStatus();
			}
		}
	};

	public CaseUsbVolume(Context context) {
		super(context, R.string.case_usbvolume_name,
				R.layout.case_usbvolume_max, R.layout.case_usbvolume_min,
				TYPE_MODE_AUTO);
		mStorageInfoManager = new StorageInfoManager(mContext);
		mMaxMountStatus = (TextView) mMaxView
				.findViewById(R.id.case_usbvolume_mount_status);
		mMaxRWStatus = (TextView) mMaxView
				.findViewById(R.id.case_usbvolume_rw_status);
		mMinMountStatus = (TextView) mMinView
				.findViewById(R.id.case_usbvolume_mount_status);
		mMinRWStatus = (TextView) mMinView
				.findViewById(R.id.case_usbvolume_rw_status);
	}

	public CaseUsbVolume(Context context, XmlPullParser xmlParser) {
		this(context);
	}

	@Override
	public void onStartCase() {
		setDialogPositiveButtonEnable(false);
		IntentFilter filter = new IntentFilter();
		filter.addDataScheme("file");
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		mContext.registerReceiver(mBroadCastReceiver, filter);
		updateStatus();
	}

	private void updateStatus() {

		List<StorageInfo> infos = mStorageInfoManager.getUsbdeviceInfos();
		boolean haveMounted = false;
		boolean allRWable = false;
		for (StorageInfo info : infos) {
			if (info.isMounted()) {
				haveMounted = true;
				allRWable = true;
				if (!info.isRWable()) {
					allRWable = false;
					break;
				}
			}
		}
		mMaxMountStatus
				.setText(haveMounted ? R.string.case_usbvolume_mount_status_success
						: R.string.case_usbvolume_mount_status_fail);
		mMaxRWStatus.setText(allRWable ? R.string.case_usbvolume_status_success
				: R.string.case_usbvolume_status_fail);
		mMinMountStatus
				.setText(haveMounted ? R.string.case_usbvolume_mount_status_success
						: R.string.case_usbvolume_status_fail);
		mMinRWStatus.setText(allRWable ? R.string.case_usbvolume_status_success
				: R.string.case_usbvolume_status_fail);

		boolean result = haveMounted && allRWable;
		setCaseResult(result);
		setDialogPositiveButtonEnable(result);
	}

	@Override
	public void onStopCase() {
		mContext.unregisterReceiver(mBroadCastReceiver);
	}

	@Override
	public void reset() {
		super.reset();
	}
}
