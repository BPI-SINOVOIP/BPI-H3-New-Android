package com.clock.pt1.keeptesting.storagetester;

import java.io.File;

import com.clock.pt1.keeptesting.LogUtil;
import com.clock.pt1.keeptesting.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class ResultReporterActivity extends Activity {

	private WebView mResultWebView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_reporter);
		mResultWebView = (WebView) findViewById(R.id.result_web_view);
		
		File logFile = new File(LogUtil.PATH+StorageTestActivity.LOG_FILE_NAME);
		if(logFile.exists()) {
			mResultWebView.loadUrl("file://"+LogUtil.PATH+StorageTestActivity.LOG_FILE_NAME);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		File logFile = new File(LogUtil.PATH+StorageTestActivity.LOG_FILE_NAME);
		if(logFile.exists()) {
			mResultWebView.loadUrl("file://"+LogUtil.PATH+StorageTestActivity.LOG_FILE_NAME);
		}
	}
}
