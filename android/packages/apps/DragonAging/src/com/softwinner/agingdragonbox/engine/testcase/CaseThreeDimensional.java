package com.softwinner.agingdragonbox.engine.testcase;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.SystemProperties;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;




import com.softwinner.agingdragonbox.R;
import com.softwinner.agingdragonbox.engine.BaseCase;
import com.softwinner.agingdragonbox.engine.testcase.ThreeDimensionalView;
import com.softwinner.agingdragonbox.xml.Node;
import com.softwinner.agingdragonbox.engine.BaseCase;
import com.softwinner.agingdragonbox.engine.Utils;
import com.softwinner.agingdragonbox.xml.Node;
import com.softwinner.Gpio;
import com.softwinner.agingdragonbox.Thread_var;


/**
 * 3D测试
 * 
 * @author zhengxiangna
 * 
 */

public class CaseThreeDimensional extends BaseCase {
	private ThreeDimensionalView mThreeDimensionalView;
	private ViewGroup iewGroup;
	private TimerTask mTimerTask;
	private Timer mTimer = new Timer();
	private boolean ThreadExit = true;
	private boolean ThreadExit_ddr = true;
	private static int LedTime = 500;
	Thread browseThread = null;
	Thread_var thread_var = new Thread_var();
	@Override
	protected void onInitialize(Node attr) {
		setView(R.layout.case_threedimensional);
		setName(R.string.case_memory_name);
		mThreeDimensionalView = new ThreeDimensionalView(mContext);
		iewGroup = (ViewGroup)getView().findViewById(R.id.myViewGroup);
	}

	@Override
	protected boolean onCaseStarted() {
		iewGroup.addView(mThreeDimensionalView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		return false;
	}

	

	@Override
	protected void onCaseFinished() {

	}

	@Override
	protected void onRelease() {

	}
	private void chanceLedStatus(int status) {
		char portType = 'x';
		int portNum = 1;
		Gpio.writeGpio(portType, portNum, status);
	}

	private void startCtrlLedThread() {
		browseThread = new Thread() {
			public void run() {
				try {
					ThreadExit = thread_var.ThreadExit;
					ThreadExit_ddr = thread_var.ThreadExit_ddr;
					while (ThreadExit_ddr && ThreadExit) {
						if (LedTime <= 0) {
							LedTime = 500;
						}
						chanceLedStatus(0);
						Thread.sleep(LedTime);
						chanceLedStatus(1);
						Thread.sleep(LedTime);
						ThreadExit = thread_var.ThreadExit;
						ThreadExit_ddr = thread_var.ThreadExit_ddr;
					}
					chanceLedStatus(0);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};
		browseThread.start();
	}

}
