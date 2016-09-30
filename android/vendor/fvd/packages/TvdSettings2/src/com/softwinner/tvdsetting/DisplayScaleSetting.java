package com.softwinner.tvdsetting;


import com.softwinner.tvdsetting.widget.ExtImageButton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class DisplayScaleSetting extends Activity implements  SeekBar.OnSeekBarChangeListener {

    private int MAXIMUM_VALUE = 100;
    private int MINIMUM_VALUE = 50;
    private String DISPLAY_AREA_RADIO = "display.area_radio";
    private String TAG = "DisplayScaleSetting";
    private DisplayManager mDisplayManager;
    private SeekBar mSeekBar;
    private TextView mPrecents;
    private int oldValue;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mDisplayManager = (DisplayManager)
                this.getSystemService(Context.DISPLAY_SERVICE);
		setContentView(R.layout.displayscale);
		mPrecents = (TextView)this.findViewById(R.id.disp_precents);
		mSeekBar = (SeekBar)this.findViewById(R.id.progressBar1);
		mSeekBar.setOnSeekBarChangeListener(this);
		oldValue=100;
		try {
			oldValue = getSysInt();
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mPrecents.setText(Integer.toString(oldValue));
		mSeekBar.setProgress(oldValue);
	}

    private int getSysInt() throws SettingNotFoundException{
        return Settings.System.getInt(getContentResolver(),
                   Settings.System.DISPLAY_AREA_RATIO, MINIMUM_VALUE);
    }

    private boolean putSysInt(int value) {
        Settings.System.putInt(getContentResolver(),
                Settings.System.DISPLAY_AREA_H_PERCENT, value);
        Settings.System.putInt(getContentResolver(),
                Settings.System.DISPLAY_AREA_V_PERCENT, value);
        return Settings.System.putInt(getContentResolver(),
                Settings.System.DISPLAY_AREA_RATIO, value);
    }

    private void setDisplayPercent(int value) {
        mDisplayManager.setDisplayPercent(android.view.Display.TYPE_BUILT_IN, value);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	ImageView title = (ImageView) this.findViewById(R.id.title);
    	switch(keyCode){
    	case KeyEvent.KEYCODE_DPAD_CENTER:
    		putSysInt(mSeekBar.getProgress());
    		break;
    	case KeyEvent.KEYCODE_BACK:
    			title.setImageResource(R.drawable.ic_word_display_setting_nor);
    			setDisplayPercent(oldValue);
    		break;

    	}
    	return false;
    }
	@Override
	public boolean onKeyUp (int keyCode, KeyEvent event){
		ImageView title = (ImageView) this.findViewById(R.id.title);
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
			title.setImageResource(R.drawable.ic_word_display_setting_nor);
			finish();
			break;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			finish();
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		if(progress>MAXIMUM_VALUE){
			progress = MAXIMUM_VALUE;
		}
		if(progress<MINIMUM_VALUE){
			progress = MINIMUM_VALUE;
		}
		setDisplayPercent(progress);
		mPrecents.setText(Integer.toString(progress));
		mSeekBar.setProgress(progress);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}
}
