package com.softwinner.tvdsetting;

import java.util.List;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.IOException;
import android.os.SystemProperties;

import com.softwinner.tvdsetting.DisplayAdjuestDialog.DisplayDialogInterface;
import com.softwinner.tvdsetting.widget.ExtImageButton;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;


public class DisplaySetting extends Activity implements View.OnClickListener, ExtImageButton.KeyEventInterface, View.OnFocusChangeListener{

    private static final String TAG = "DisplaySetting";

    private TextView mResTextView;
    private TextView mRecommendTextView;
    private ImageView mUpImgView;
    private ImageView mDownImgView;
    private Drawable mUpPress,mUpNor;
    private Drawable mDownPress,mDownNor;
    private List<String> mDisplayModeEntries = new ArrayList<String>();
    private List<Integer> mDisplayModeValues = new ArrayList<Integer>();
    String[] mode_entries ;
    int[] mode_value ;
    String[] hdmiModeEntries ;
    int[] hdmiModeValue ;
    String[] cvbsModeEntries ;
    int[] cvbsModeValue ;

    Resources res;
    boolean plugged = true;
    private int cnt = 0;
    private boolean isSupport = false;
    private boolean isSameMode = false;
    private int oldValue;
    private int newValue;
    private DisplayManager mDisplayManager;
    private BroadcastReceiver mDisplayPluggedListener = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };

    private void initDisplayModeList() {
        mDisplayModeEntries.clear();
        mDisplayModeValues.clear();
        if (isHdmiMode()) {
            for(int i=0;i<hdmiModeValue.length;i++){
                mDisplayModeEntries.add(hdmiModeEntries[i]);
                mDisplayModeValues.add(hdmiModeValue[i]);
            }
            mRecommendTextView.setVisibility(View.VISIBLE);
        } else {
            for(int i=0;i<cvbsModeValue.length;i++){
                mDisplayModeEntries.add(cvbsModeEntries[i]);
                mDisplayModeValues.add(cvbsModeValue[i]);
            }
            mRecommendTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void updateUI() {
        int value = mDisplayManager.getDisplayOutput(android.view.Display.TYPE_BUILT_IN);
        initDisplayModeList();
        if(mDisplayModeValues.indexOf(value)<0){
            Log.e(TAG,"unexpected state:");
            Log.e(TAG,"hdmi plugged : " + isHdmiMode());
            Log.e(TAG,"mDisplayManager.getDisplayOutput (BUILT_IN): " + value);
            return;
        }
        cnt = mDisplayModeValues.indexOf(value);
        mResTextView.setText(mDisplayModeEntries.get(cnt));
        oldValue = value;
        newValue = oldValue;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaysetting);

        res =getResources();

        ExtImageButton mScaleSetting = (ExtImageButton)this.findViewById(R.id.scaleandshift);
        ExtImageButton mResresolution = (ExtImageButton)this.findViewById(R.id.resolutionsetting);
        mUpImgView = (ImageView)this.findViewById(R.id.ic_up);
        mDownImgView = (ImageView)this.findViewById(R.id.ic_down);
        mUpNor = res.getDrawable(R.drawable.displaysetting_arrow_up);
        mUpPress = res.getDrawable(R.drawable.displaysetting_arrow_up);
        mDownNor = res.getDrawable(R.drawable.displaysetting_arrow_down);
        mDownPress = res.getDrawable(R.drawable.displaysetting_arrow_down);
        mResTextView = (TextView)this.findViewById(R.id.resolutions);
        mRecommendTextView = (TextView)this.findViewById(R.id.recommends);
        mScaleSetting.setOnClickListener(this);
        mResresolution.setOnClickListener(this);
        mResresolution.setOnDispatchKeyEvent(this);
        mResresolution.setOnFocusChangeListener(this);
        mDisplayManager = (DisplayManager)getSystemService(
                Context.DISPLAY_SERVICE);

        final int hdmi_4k_ban = SystemProperties.getInt("persist.sys.hdmi_4k_ban", 0);
        if (hdmi_4k_ban == 0) {
            hdmiModeEntries = res.getStringArray(R.array.hdmi_output_mode_entries);
            hdmiModeValue = res.getIntArray(R.array.hdmi_output_mode_values);
        } else {
            hdmiModeEntries = res.getStringArray(R.array.hdmi_output_mode_without_4k_entries);
            hdmiModeValue = res.getIntArray(R.array.hdmi_output_mode_without_4k_values);
        }
        cvbsModeEntries = res.getStringArray(R.array.cvbs_output_mode_entries);
        cvbsModeValue = res.getIntArray(R.array.cvbs_output_mode_values);
        initDisplayModeList();

        registerReceiver(mDisplayPluggedListener, new IntentFilter(
                    Intent.ACTION_HDMISTATUS_CHANGED));
        updateUI();
    }

    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub
        if(arg0.getId() == R.id.scaleandshift){
            Intent intent = new Intent();
            intent.setClass(DisplaySetting.this, DisplayScaleSetting.class);
            startActivity(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mDisplayPluggedListener);
    }

    @Override
    public boolean onDispatchKeyEvent(View view,KeyEvent event) {
        // TODO Auto-generated method stub
        boolean retval = false;
        if(event.getKeyCode()==KeyEvent.KEYCODE_DPAD_UP){
            if(event.getAction() == KeyEvent.ACTION_DOWN){
                if(cnt<mDisplayModeValues.size() - 1){
                    cnt += 1;
                } else {
                    cnt = 0;
                }
                newValue = mDisplayModeValues.get(cnt);
                mResTextView.setText(mDisplayModeEntries.get(cnt));
                mUpImgView.setImageDrawable(mUpPress);
            }else if(event.getAction() == KeyEvent.ACTION_UP){
                mUpImgView.setImageDrawable(mUpNor);
            }
            retval = true;
        }else if(event.getKeyCode()==KeyEvent.KEYCODE_DPAD_DOWN){
            if(event.getAction() == KeyEvent.ACTION_DOWN){
                if(cnt>0){
                    cnt -= 1;
                } else {
                    cnt = mDisplayModeValues.size() - 1;
                }
                newValue = mDisplayModeValues.get(cnt);
                mResTextView.setText(mDisplayModeEntries.get(cnt));
                mDownImgView.setImageDrawable(mDownPress);
            }else if(event.getAction() == KeyEvent.ACTION_UP){
                mDownImgView.setImageDrawable(mDownNor);
            }
            retval = true;
        }else if(event.getKeyCode()==KeyEvent.KEYCODE_DPAD_CENTER){
            if(event.getAction() == KeyEvent.ACTION_DOWN){
                if(newValue!=oldValue){
                    showChangeDialog(mDisplayModeEntries.get(cnt),oldValue,newValue);
                    switchDispFormat(mDisplayModeValues.get(cnt),false);
                }
            }
        }
        return retval;
    }

    private boolean isHdmiMode(){

        int value = mDisplayManager.getDisplayOutput(android.view.Display.TYPE_BUILT_IN);
        for (int i = 0; i < hdmiModeValue.length; i++) {
            if (value == hdmiModeValue[i]) {
                return true;
            }
        }
        return false;
    }

    private void showChangeDialog(String text,int oldvalue,int newvalue){
        final int valueNew = newvalue;
        final int valueOld = oldvalue;
        final DisplayAdjuestDialog dd = new DisplayAdjuestDialog(this,R.style.CommonDialog,text);
        DisplayDialogInterface ddinterface = new DisplayDialogInterface(){
            @Override
            public void onButtonYesClick() {
                switchDispFormat(valueNew,true);
                dd.dismiss();
                setOldValue(valueNew);
            }

            @Override
            public void onButtonNoClick() {
                newValue = valueOld;
                switchDispFormat(valueOld,false);
                dd.dismiss();
            }

            @Override
            public void onTimeOut() {
                if(dd.isShowing()){
                    Log.d(TAG,"switchDispFormat to old mode " + valueOld);
                    switchDispFormat(valueOld,false);
                    dd.dismiss();
                }
            }
        };
        dd.setDisplayDialogInterface(ddinterface);
        dd.show();
    }

    private void setOldValue(int value){
        oldValue = value;
    }

    private void switchDispFormat(int value, boolean save) {
        try {
            int format = value;
            final DisplayManager dm = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
            int dispformat = dm.getDisplayModeFromFormat(format);
            int mCurType = dm.getDisplayOutputType(android.view.Display.TYPE_BUILT_IN);
            dm.setDisplayOutput(android.view.Display.TYPE_BUILT_IN, format);
            if(save){
                Settings.System.putString(getContentResolver(), Settings.System.DISPLAY_OUTPUT_FORMAT, Integer.toHexString(value));
            }
            Log.d(TAG,"switchDispFormat = " + mDisplayModeValues.indexOf(value));
            cnt = mDisplayModeValues.indexOf(value);
            mResTextView.setText(mDisplayModeEntries.get(cnt));

        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid display output format!");
        }
    }

    @Override
    public void onFocusChange(View arg0, boolean hasFocus) {
        switch(arg0.getId()){
            case R.id.resolutionsetting:
                if (hasFocus) {
                    mUpImgView.setVisibility(View.VISIBLE);
                    mDownImgView.setVisibility(View.VISIBLE);
                } else {
                    mUpImgView.setVisibility(View.INVISIBLE);
                    mDownImgView.setVisibility(View.INVISIBLE);
                }
                break;
            default:
                break;
        }

    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event){
        ImageView title = (ImageView) this.findViewById(R.id.title);
        switch(keyCode){
            case KeyEvent.KEYCODE_BACK:
                title.setImageResource(R.drawable.ic_word_display_setting_nor);
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
        }
        return false;
    }
}
