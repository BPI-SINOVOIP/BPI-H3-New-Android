
package android.hardware.display;

import java.util.ArrayList;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.Settings.SettingNotFoundException;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import android.hardware.display.outputstate.*;

/** @hide */
public class DisplayManagerPolicy2 {

    private static final String TAG = "DisplayManagerPolicy2";
    private Context mContext;
    private DisplayManager mDm;
    private Handler mH = new Handler(Looper.getMainLooper());
    private boolean mBootCompleted = false;

    private static File mDisp2EnhanceIdFile;
    private static File mDisp2EnhanceModeFile;
    private static boolean canSetDisp2Enhance;

    private DispOutputState mainDispToDev0PlugIn;
    private DispOutputState mainDispToDev0PlugInExt;
    private DispOutputState mainDispToDev0PlugOut;
    private DispOutputState mainDispToDev1PlugIn;
    private DispOutputState mainDispToDev1PlugOut;
    private DispOutputState dualDisplayOutput;
    private DispOutputState mDispOutputState;

    class DataBaseObserver extends ContentObserver {
        DataBaseObserver(Handler handler) {
        super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
           /*
            resolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.DISPLAY_AREA_RATIO), false, this);
            update();

            int hpercent = Settings.System.getInt(resolver,
                Settings.System.DISPLAY_AREA_H_PERCENT, 100);
            int vpercent = Settings.System.getInt(resolver,
                Settings.System.DISPLAY_AREA_V_PERCENT, 100);
            mDm.setDisplayMargin(Display.TYPE_BUILT_IN, hpercent, vpercent);
            */
            int brightness = Settings.System.getInt(resolver,
                Settings.System.COLOR_BRIGHTNESS, 100);
            mDm.setDisplayBright(Display.TYPE_BUILT_IN, brightness);
            int contrast = Settings.System.getInt(resolver,
                Settings.System.COLOR_CONTRAST, 100);
            mDm.setDisplayContrast(Display.TYPE_BUILT_IN, contrast);
            int saturation = Settings.System.getInt(resolver,
                Settings.System.COLOR_SATURATION, 100);
            mDm.setDisplaySaturation(Display.TYPE_BUILT_IN, saturation);
        }

        @Override public void onChange(boolean selfChange) {
            update();
        }

        public void update() {
        /*
            ContentResolver resolver = mContext.getContentResolver();
            try{
            int dispAreaRatio = Settings.System.getInt(resolver,
                Settings.System.DISPLAY_AREA_RATIO);
            Settings.System.putInt(resolver,
                Settings.System.DISPLAY_AREA_H_PERCENT, dispAreaRatio);
            Settings.System.putInt(resolver,
                Settings.System.DISPLAY_AREA_V_PERCENT, dispAreaRatio);
            }catch(SettingNotFoundException e){
                Log.e(TAG, Settings.System.DISPLAY_AREA_RATIO +" not found");
            }
         */
        }
    }

    private BroadcastReceiver mBootCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBootCompleted = true;
        }
    };

    private class DispDevice {
        int type;
        int revertPlugStateType;
        boolean hotplugSupport;
    };
    private ArrayList<DispDevice> mDispDevices =  new ArrayList<DispDevice>();

    public DisplayManagerPolicy2(Context context) {
        mContext = context;
        mDm = new DisplayManager(context);

        DataBaseObserver observer = new DataBaseObserver(new Handler());
        observer.observe();

        IntentFilter filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        mContext.registerReceiver(mBootCompletedReceiver, filter);

        mDisp2EnhanceIdFile = new File("/sys/class/disp/disp/attr/disp");
        mDisp2EnhanceModeFile = new File("/sys/class/disp/disp/attr/enhance_mode");
        if(mDisp2EnhanceIdFile.exists() && mDisp2EnhanceModeFile.exists()) {
            canSetDisp2Enhance = true;
            int enhanceMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DISPLAY2_ENHANCE_MODE, 0);
            setDisplay2EnhanceMode(0, enhanceMode);
        } else {
            canSetDisp2Enhance = false;
        }

        initOutputState();
    }

    private void initOutputState() {
        int dispFormat;
        DispDevice dispDevice;
        mainDispToDev0PlugIn = new MainDispToDev0PlugIn(this);
        mainDispToDev0PlugInExt = new MainDispToDev0PlugInExt(this);
        mainDispToDev0PlugOut = new MainDispToDev0PlugOut(this);
        mainDispToDev1PlugIn = new MainDispToDev1PlugIn(this);
        mainDispToDev1PlugOut= new MainDispToDev1PlugOut(this);
        dualDisplayOutput = new DualDisplayOutput(this);

        // 1: init the mDispDevices
        String defFormats[] = mContext.getResources().getStringArray(
            com.android.internal.R.array.default_display_format);
        for (int i = 0; i < defFormats.length; i++) {
            dispDevice = new DispDevice();
            dispFormat = Integer.valueOf(defFormats[i], 16);
            dispDevice.type = mDm.getDisplayTypeFromFormat(dispFormat);
            dispDevice.hotplugSupport = getHotplugSupport(dispDevice.type);
            dispDevice.revertPlugStateType = getRevertHotplugType(dispDevice.type);
            Log.d(TAG, "device[" + i + "]: type=" + dispDevice.type
                + ", hotPlugSupport=" + dispDevice.hotplugSupport
                + ", RPST=" + dispDevice.revertPlugStateType);
            mDispDevices.add(i, dispDevice);
        }

        // 2: init the mDispOutputState --> the current output state
        int dispType;
        int currentDispNum = 0;
        int mainPriority = 0;

        dispType = mDm.getDisplayOutputType(Display.TYPE_BUILT_IN);
        dispDevice = getDispDeviceByType(dispType);
        if(mDispDevices.contains(dispDevice)) {
            currentDispNum++; // always has maindisplay
            mainPriority = mDispDevices.indexOf(dispDevice);
            Log.d(TAG, "mainDispDevice[" + dispDevice.type + ","
                + mainPriority + "]");
        } else {
            Log.e(TAG, "main: initOutputState maybe failed, fixme!!!");
        }

        dispType = mDm.getDisplayOutputType(Display.TYPE_HDMI);
        dispDevice = getDispDeviceByType(dispType);
        if(mDispDevices.contains(dispDevice)) {
            currentDispNum++;
            Log.d(TAG, "extDispDevice[" + dispDevice.type + ","
                + mDispDevices.indexOf(dispDevice) + "]");
        } else {
            Log.d(TAG, "no external display for the type[" + dispType + "]");
        }

		Log.d(TAG, "currentDispNum=" + currentDispNum + ", mainPriority=" + mainPriority);
        if(1 == currentDispNum) {
            if(1 == mainPriority) {
                mDispOutputState = mainDispToDev1PlugIn; //always plugin
            } else {
                mDispOutputState = mainDispToDev0PlugIn; //always plugin
            }
        } else {
            mDispOutputState = dualDisplayOutput; //always pulgin
        }
    }

    public DispOutputState getMainDispToDev0PlugIn() {
        return mainDispToDev0PlugIn;
    }
    public DispOutputState getMainDispToDev0PlugInExt() {
        return mainDispToDev0PlugInExt;
    }
    public DispOutputState getMainDispToDev0PlugOut() {
        return mainDispToDev0PlugOut;
    }
    public DispOutputState getMainDispToDev1PlugIn() {
        return mainDispToDev1PlugIn;
    }
    public DispOutputState getMainDispToDev1PlugOut() {
        return mainDispToDev1PlugOut;
    }
    public DispOutputState getDualDisplayOutput() {
        return dualDisplayOutput;
    }
    public void setOutputState(DispOutputState state) {
        Log.d(TAG, "setOutputState: " + state);
        this.mDispOutputState = state;
    }
    public int setDisplayOutput(int disp, int dispFormat) {
        Log.d(TAG, "setDispOutput: disp=" + disp + ", dispFormat=" + dispFormat);
        return mDm.setDisplayOutput(disp, dispFormat);
    }

    private DispDevice getDispDeviceByType(int dispType) {
        int priority;
        for(priority = 0; priority < mDispDevices.size(); priority++) {
            DispDevice dispDevice = mDispDevices.get(priority);
            if(dispType == dispDevice.type) {
                return dispDevice;
            }
        }
        return null;
    }
    private int getDispTypeByPriority(int priority) {
        if(mDispDevices.size() <= priority)
            return 0;
        return mDispDevices.get(priority).type;
    }
    private synchronized void dispDevicePlugChanged(DispDevice dispDevice, boolean plugState) {
        int dispFormat = 0;
        int dispType = dispDevice.type;
        int priority = mDispDevices.indexOf(dispDevice);
        if(false == plugState) {
            if(0 == priority) {
                dispType = getDispTypeByPriority(1);
            }
        }
        dispFormat = mDm.makeDisplayFormat(dispType, 0xFF);// 0xFF means adaptive mode
        Log.d(TAG,"plug=" + plugState + ", device[" + priority+ ","
            + dispDevice.type + ", " + dispFormat + "]");
        mDispOutputState.devicePlugChanged(dispFormat, priority, plugState);
    }
    public synchronized void notifyDisplayDevicePlugedChanged(int displayType, boolean pluggedIn) {
        DispDevice dispDevice = getDispDeviceByType(displayType);
        DispDevice dispDevice1;
        hotplugTips(displayType, pluggedIn);
        if(null == dispDevice) {
            return;
        }
        if(false == dispDevice.hotplugSupport) {
            return;
        }
        if(0 != dispDevice.revertPlugStateType && true == pluggedIn) {
            dispDevice1 = getDispDeviceByType(dispDevice.revertPlugStateType);
            dispDevicePlugChanged(dispDevice1, false);
        }
        dispDevicePlugChanged(dispDevice, pluggedIn);
        if(0 != dispDevice.revertPlugStateType && false == pluggedIn) {
            dispDevice1 = getDispDeviceByType(dispDevice.revertPlugStateType);
            dispDevicePlugChanged(dispDevice1, true);
        }
    }

    private int getRevertHotplugType(int displayType) {
		String propertyValue;
        switch(displayType) {
        case DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI:
			propertyValue = SystemProperties.get("persist.sys.hdmi_rvthpd", "0");
            break;
        case DisplayManager.DISPLAY_OUTPUT_TYPE_TV:
			propertyValue = SystemProperties.get("persist.sys.cvbs_rvthpd", "0");
            break;
        case DisplayManager.DISPLAY_OUTPUT_TYPE_VGA:
        case DisplayManager.DISPLAY_OUTPUT_TYPE_LCD:
        default:
			propertyValue = "0";
        }
		return Integer.valueOf(propertyValue, 16);
    }

    private boolean getHotplugSupport(int displayType) {
		String propertyValue;
        int hotplugSupport = 0;
        switch(displayType) {
        case DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI:
			propertyValue = SystemProperties.get("persist.sys.hdmi_hpd", "1");
            break;
        case DisplayManager.DISPLAY_OUTPUT_TYPE_TV:
			propertyValue = SystemProperties.get("persist.sys.cvbs_hpd", "1");
            break;
        case DisplayManager.DISPLAY_OUTPUT_TYPE_VGA:
        case DisplayManager.DISPLAY_OUTPUT_TYPE_LCD:
        default:
            propertyValue = "0";
        }
		hotplugSupport = Integer.valueOf(propertyValue, 16);
		return (1 == hotplugSupport);
    }

    private void hotplugTips(int displayType, boolean pluggedIn) {
        if (mBootCompleted) {
            final String msg;
            int resId = pluggedIn ? com.android.internal.R.string.display_device_plugged_in
                : com.android.internal.R.string.display_device_plugged_out;
            switch(displayType) {
            case DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI:
                msg = String.format(mContext.getString(resId), "HDMI");
                break;
            case DisplayManager.DISPLAY_OUTPUT_TYPE_TV:
                msg = String.format(mContext.getString(resId), "CVBS");
                break;
            case DisplayManager.DISPLAY_OUTPUT_TYPE_VGA:
                msg = String.format(mContext.getString(resId), "VGA");
                break;
            case DisplayManager.DISPLAY_OUTPUT_TYPE_LCD:
                msg = String.format(mContext.getString(resId), "LCD");
                break;
            default:
                msg = String.format(mContext.getString(resId), "unkown");
            }
            mH.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public int getDisplay2EnhanceMode(int disp) {
        if(!canSetDisp2Enhance) {
            return -1;
        }
        FileWriter fw;
        BufferedReader reader;
        int mode = -1;
        try{
            fw = new FileWriter(mDisp2EnhanceIdFile);
            fw.write(String.valueOf(disp));
            fw.close();
            reader = new BufferedReader(new FileReader(mDisp2EnhanceModeFile));
            mode = Integer.parseInt(reader.readLine());
            reader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return mode;
    }
    public int setDisplay2EnhanceMode(int disp, int mode) {
        if(!canSetDisp2Enhance) {
            return -1;
        }
        FileWriter fw;
        try{
            fw = new FileWriter(mDisp2EnhanceIdFile);
            fw.write(String.valueOf(disp));
            fw.close();
            fw = new FileWriter(mDisp2EnhanceModeFile);
            fw.write(String.valueOf(mode));
            fw.close();
            Settings.System.putInt(mContext.getContentResolver(),
            Settings.System.DISPLAY2_ENHANCE_MODE, mode);
        } catch(IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}

