package com.softwinner.tvdsetting.applications;

import com.softwinner.tvdsetting.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

public class AppManagerActivity extends Activity implements ListView.OnItemClickListener{

	private static final String TAG = "AppManagerActivity";
	ListView mAppList;
    ApplicationsState mApplicationsState;
    ApplicationsAdapter mApplications;
	 @Override
	  protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.appmanager);
			mAppList = (ListView) this.findViewById(R.id.applist);
			//mAppList.setAdapter(new AppListAdapter(this));
			mApplicationsState = ApplicationsState.getInstance(getApplication());
			mApplications = new ApplicationsAdapter(mApplicationsState,this,0);
			mApplications.resume(4);
			mAppList.setAdapter(mApplications);
			mAppList.setRecyclerListener(mApplications);
			mAppList.setOnItemClickListener(this);
	 }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id ) {
		// TODO Auto-generated method stub
        ApplicationsState.AppEntry entry = mApplications.getAppEntry(position);
        String pkgName = entry.info.packageName;
        Bundle args = new Bundle();
        args.putString(AppDetialActivity.ARG_PACKAGE_NAME, pkgName);
		Intent intent = new Intent();
		intent.putExtras(args);
		intent.setClass(AppManagerActivity.this,AppDetialActivity.class);
		startActivity(intent);
	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_appmanager_nor);
			break;
		}
		return false;
	}

	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
				title.setImageResource(R.drawable.ic_word_appmanager_nor);
				finish();
			break;
		}
		return false;
	}
}