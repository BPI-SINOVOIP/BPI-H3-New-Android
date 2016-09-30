package com.clock.pt1.keeptesting.storagetester;

import com.clock.pt1.keeptesting.R;
import com.clock.pt1.keeptesting.ShellUtil;

import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class StorageTestActivity extends TabActivity {

	public static final String TAG = "StorageTester";
	public static final String LOG_FILE_NAME = "storageTesterResult.html";
	private int tabCount = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent;
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		TabHost myTabhost = getTabHost();
		LayoutInflater.from(this).inflate(R.layout.tab_view,
				myTabhost.getTabContentView(), true);
		
		intent = new Intent().setClass(this, SpeedTesterActivity.class);
		myTabhost.addTab(myTabhost.newTabSpec("A")
				.setIndicator(this.getResources().getString(R.string.storage_test_read_write_text), null).setContent(intent));
		tabCount++;
		
		intent = new Intent().setClass(this, CopyTesterActivity.class);
		myTabhost.addTab(myTabhost.newTabSpec("B")
				.setIndicator(this.getResources().getString(R.string.storage_test_file_copy_text), null).setContent(intent));
		tabCount++;
		
		intent = new Intent().setClass(this, ResultReporterActivity.class);
		myTabhost.addTab(myTabhost.newTabSpec("C").
				setIndicator(this.getResources().getString(R.string.storage_test_result_text), null).setContent(intent));
		tabCount++;

		for(int i = 0; i < tabCount; i++ ){
			/* set the title font size */
			TextView title = (TextView) myTabhost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
			title.setTextSize(25);
		}
		
		myTabhost.setCurrentTab(0);
		/* check if the device is rooted */
		if(!ShellUtil.isDeviceRooted()) {
			Dialog alertDialog = new AlertDialog.Builder(this).setTitle(R.string.attention)
					.setMessage(R.string.device_not_rooted)
					.setPositiveButton(this.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							StorageTestActivity.this.finish();
						}
					}).create();
			alertDialog.show();
		}
	}
	
	private long lastPressTime = 0;
	private static final int EXIT_INTERVAL = 3000;
	@Override
	public void onBackPressed(){
		long pressTime = System.currentTimeMillis();
		long interval = pressTime - lastPressTime;
		lastPressTime = pressTime;
		if(interval < EXIT_INTERVAL){
			super.onBackPressed();
		}else{
			Toast.makeText(this, this.getResources().getString(R.string.storage_test_exit_message), Toast.LENGTH_SHORT).show();
		}
	}
}
