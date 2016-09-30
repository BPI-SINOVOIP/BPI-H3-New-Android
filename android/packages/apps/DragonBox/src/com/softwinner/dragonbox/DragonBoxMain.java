package com.softwinner.dragonbox;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.softwinner.dragonbox.config.ConfigManager;
import com.softwinner.dragonbox.testcase.CaseEthernet;
import com.softwinner.dragonbox.testcase.CasePerformance;
import com.softwinner.dragonbox.testcase.CaseWifi;
import com.softwinner.dragonbox.testcase.IBaseCase;

public class DragonBoxMain extends Activity {

	// public ViewGroup fullContainer;
	public LinearLayout caseContainner;
	private List<IBaseCase> mAllCases = new ArrayList<IBaseCase>();
	private List<IBaseCase> mAllAutoCases = new ArrayList<IBaseCase>();
	private List<IBaseCase> mAllManualCases = new ArrayList<IBaseCase>();

	private int backPressTimes = 0;
	private static final int BACK_PRESS_MAX_TIMES = 3;
	private int prePressNum = -1;
	private static final int WHAT_HANDLE_RESTART = 0;
	private static final int WHAT_HANDLE_KEY = 1;
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case WHAT_HANDLE_RESTART:
				restartAllTest();
				break;
			case WHAT_HANDLE_KEY:
				int positionNum = msg.arg1;
				if (positionNum > mAllCases.size() && positionNum >= 0){
					Toast.makeText(DragonBoxMain.this, R.string.case_main_alert_item_not_support, Toast.LENGTH_SHORT).show();
				} else {
					mAllCases.get(positionNum - 1).mActionView.performClick();
				}
				prePressNum = -1;
				break;
			}
		};
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		caseContainner = (LinearLayout) findViewById(R.id.main_case_container);
		try {
			List<IBaseCase> cases = ConfigManager.getInstence(this)
					.parseConfig();
			mAllCases = cases;
			for (int i = 0; i < cases.size(); i++) {
				IBaseCase curCase = cases.get(i);
				TextView numTV = (TextView) curCase.mMinView.findViewById(R.id.case_min_num);
				if (numTV != null) {
					numTV.setText((i + 1) + "");
				}
				caseContainner.addView(curCase.mMinView);
				if (curCase.getType() == IBaseCase.TYPE_MODE_AUTO) {
					//curCase.startCaseforAuto();
					mAllAutoCases.add(curCase);
				} else if (curCase.getType() == IBaseCase.TYPE_MODE_MANUAL) {
					mAllManualCases.add(curCase);
				}
			}
		} catch (Exception e) {
			Toast.makeText(this, "ERROR,Please check config files", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

		mHandler.sendEmptyMessageDelayed(WHAT_HANDLE_RESTART, 100);

	}

	private void generateLinkCases(List<IBaseCase> cases) {
		if (cases == null || cases.size() == 0) {
			return;
		}
		int size = cases.size();
		for (int i = 0; i < size - 1; i++) {
			cases.get(i).setNextCase(cases.get(i + 1));
		}
		cases.get(0).mActionView.performClick();
	}

	interface onFullWindowAct {
		void onFullWindowShow();
		void onFullWindowHide();
	}

	public void cancelAllTest(){
		for (IBaseCase baseCase : mAllCases) {
			baseCase.stopCase();
			baseCase.cancel();
		}
	}
	
	public void restartAllTest() {
		boolean haveEthCase = false;
		for (IBaseCase baseCase : mAllCases) {
			if (baseCase instanceof CaseEthernet) {
				haveEthCase = true;
			}
			baseCase.stopCase();
			baseCase.reset();
		}
		
		for (IBaseCase baseCase : mAllAutoCases) {

			if (baseCase instanceof CaseWifi && haveEthCase) {
				baseCase.startCaseDelay(4000);
			} else if (baseCase instanceof CasePerformance) {
				baseCase.startCaseDelay(6000);
			} else {
				baseCase.startCase();
			}
		}
		generateLinkCases(mAllManualCases);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			backPressTimes++;
			if (backPressTimes >= BACK_PRESS_MAX_TIMES) {
				finish();
				Runtime.getRuntime().exit(0);
			} else {
				Toast.makeText(this, "再按两次返回键退出应用", Toast.LENGTH_SHORT).show();
			}
			return true;
		case KeyEvent.KEYCODE_0:
		case KeyEvent.KEYCODE_1:
		case KeyEvent.KEYCODE_2:
		case KeyEvent.KEYCODE_3:
		case KeyEvent.KEYCODE_4:
		case KeyEvent.KEYCODE_5:
		case KeyEvent.KEYCODE_6:
		case KeyEvent.KEYCODE_7:
		case KeyEvent.KEYCODE_8:
		case KeyEvent.KEYCODE_9:
			mHandler.removeMessages(WHAT_HANDLE_KEY);
			Message msg = mHandler.obtainMessage();
			msg.what = WHAT_HANDLE_KEY;
			if (prePressNum == -1) {
				prePressNum = keyCode - KeyEvent.KEYCODE_0;
				msg.arg1 = prePressNum;
				if (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_1) {
					mHandler.sendMessageDelayed(msg, 1000);					
				} else {
					mHandler.sendMessage(msg);
				}
			} else {
				msg.arg1 = prePressNum * 10 + (keyCode - KeyEvent.KEYCODE_0);
				mHandler.sendMessage(msg);
			}
			backPressTimes = 0;
			return true;
		default:
			break;
		}
		backPressTimes = 0;
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.menu_retest:
			restartAllTest();
			break;
		case R.id.menu_go_dragonsn:
			ConfigManager.startConfigAPK(this, ConfigManager.CONFIG_DRAGON_SN, true);
			break;
		case R.id.menu_go_dragonaging:
			ConfigManager.startConfigAPK(this, ConfigManager.CONFIG_DRAGON_AGING, true);
			break;
		}
		return true;
	}
	
	/*
	 * void showFullWindow(View view){ fullContainer.removeAllViews();
	 * fullContainer.addView(((IBaseCase) view.getTag()).mMaxView);
	 * fullContainer.setVisibility(View.VISIBLE); }
	 * 
	 * private void hideFullWindow(){ if (fullContainer.getChildCount() > 0) {
	 * ((IBaseCase) fullContainer.getChildAt(0).getTag()).stopCase(); }
	 * fullContainer.removeAllViews(); fullContainer.setVisibility(View.GONE); }
	 */
	/*
	 * @Override public void onClick(View view) { showFullWindow(view);
	 * ((IBaseCase) view.getTag()).startCaseforAuto(); }
	 */

}
