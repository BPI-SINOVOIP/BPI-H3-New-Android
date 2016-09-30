package android.os.display;

import com.softwinner.utils.Config;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

public class DisplayManager{
    public final static int DISPLAY_STANDARD_1080P_60 = 0;
    public final static int DISPLAY_STANDARD_1080P_50 = 1;
    public final static int DISPLAY_STANDARD_1080P_30 = 2;
    public final static int DISPLAY_STANDARD_1080P_25 = 3;
    public final static int DISPLAY_STANDARD_1080P_24 = 4;
    public final static int DISPLAY_STANDARD_1080I_60 = 5;
    public final static int DISPLAY_STANDARD_1080I_50 = 6;
    public final static int DISPLAY_STANDARD_720P_60 = 7;
    public final static int DISPLAY_STANDARD_720P_50 = 8;
    public final static int DISPLAY_STANDARD_576P_50 = 9;
    public final static int DISPLAY_STANDARD_480P_60 = 10;
    public final static int DISPLAY_STANDARD_PAL = 11;
    public final static int DISPLAY_STANDARD_NTSC = 12;
    public final static int DISPLAY_UNKNOWN = 0xFF;
    private android.hardware.display.DisplayManager mDm = null;
    private Context mCtx = null;
    private int[] mSupportList = null;
    /** @hide */
    public DisplayManager(Context context){
        mCtx = context;
        mDm = (android.hardware.display.DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        switch(Config.getTargetPlatform(Config.TARGET_BOARD_PLATFORM)){
        case Config.PLATFORM_JAWS:
        case Config.PLATFORM_FIBER:
        default:
            mSupportList = new int[11];
            mSupportList[0] = DISPLAY_STANDARD_1080P_60;
            mSupportList[1] = DISPLAY_STANDARD_1080P_50;
            mSupportList[2] = DISPLAY_STANDARD_1080P_24;
            mSupportList[3] = DISPLAY_STANDARD_1080I_60;
            mSupportList[4] = DISPLAY_STANDARD_1080I_50;
            mSupportList[5] = DISPLAY_STANDARD_720P_60;
            mSupportList[6] = DISPLAY_STANDARD_720P_50;
            mSupportList[7] = DISPLAY_STANDARD_576P_50;
            mSupportList[8] = DISPLAY_STANDARD_480P_60;
            mSupportList[9] = DISPLAY_STANDARD_PAL;
            mSupportList[10] = DISPLAY_STANDARD_NTSC;
        }

    }

    /**
    * 判断是否支持该制式
    *
    * @param standard
    *    {@link #DISPLAY_STANDARD_1080P_60}
    *    {@link #DISPLAY_STANDARD_1080P_50}
    *    {@link #DISPLAY_STANDARD_1080P_30}
    *    {@link #DISPLAY_STANDARD_1080P_25}
    *    {@link #DISPLAY_STANDARD_1080P_24}
    *    {@link #DISPLAY_STANDARD_1080I_60}
    *    {@link #DISPLAY_STANDARD_1080I_50}
    *    {@link #DISPLAY_STANDARD_720P_60}
    *    {@link #DISPLAY_STANDARD_720P_50}
    *    {@link #DISPLAY_STANDARD_576P_50}
    *    {@link #DISPLAY_STANDARD_480P_60}
    *    {@link #DISPLAY_STANDARD_PAL}
    *    {@link #DISPLAY_STANDARD_NTSC}
    *
    *@return
    */

    public boolean isSupportStandard(int standard) {
        for(int i = 0; i < mSupportList.length; i++){
            if(standard == mSupportList[i]){
                return true;
            }
        }
        return false;
    }

    /**
    * 获取盒子所支持的所有制式
    * @return
    */
    public int[] getAllSupportStandards() {
        return mSupportList.clone();
    }

    /**
    * 设置制式
    *
    * @param standard
    * {@link #DISPLAY_STANDARD_1080P_60}
    * {@link #DISPLAY_STANDARD_1080P_50}
    * {@link #DISPLAY_STANDARD_1080P_30}
    * {@link #DISPLAY_STANDARD_1080P_25}
    * {@link #DISPLAY_STANDARD_1080P_24}
    * {@link #DISPLAY_STANDARD_1080I_60}
    * {@link #DISPLAY_STANDARD_1080I_50}
    * {@link #DISPLAY_STANDARD_720P_60}
    * {@link #DISPLAY_STANDARD_720P_50}
    * {@link #DISPLAY_STANDARD_576P_50}
    * {@link #DISPLAY_STANDARD_480P_60}
    *
    * {@link #DISPLAY_STANDARD_PAL} {@link #DISPLAY_STANDARD_NTSC}
    */
    public void setDisplayStandard(int standard) {
        if(!isSupportStandard(standard)){
            return;
        }
        Log.d("CMCC-WASU", "setDisplayStandard:" + standard + ", outputtype:" + mDm.getDisplayOutputType(Display.TYPE_BUILT_IN));
        int type = 0;
        int mode = 0;
		if(android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI == mDm.getDisplayOutputType(Display.TYPE_BUILT_IN))
		{
	        switch(standard){
	        case DISPLAY_STANDARD_1080P_60:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080P_60HZ;
	            break;
	        case DISPLAY_STANDARD_1080P_50:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080P_50HZ;
	            break;
	        case DISPLAY_STANDARD_1080P_30:
	            break;
	        case DISPLAY_STANDARD_1080P_25:
	            break;
	        case DISPLAY_STANDARD_1080P_24:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080P_24HZ;
	            break;
	        case DISPLAY_STANDARD_1080I_60:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080I_60HZ;
	            break;
	        case DISPLAY_STANDARD_1080I_50:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080I_50HZ;
	            break;
	        case DISPLAY_STANDARD_720P_60:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_720P_60HZ;
	            break;
	        case DISPLAY_STANDARD_720P_50:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_720P_50HZ;
	            break;
	        case DISPLAY_STANDARD_576P_50:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_576P;
	            break;
	        case DISPLAY_STANDARD_480P_60:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_480P;
	            break;
	        case DISPLAY_STANDARD_PAL:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_TV;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_PAL;
	            break;
	        case DISPLAY_STANDARD_NTSC:
	            type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_TV;
	            mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_NTSC;
	            break;
	        default:
	            return;
	        }
		}
		else
		{
			type = android.hardware.display.DisplayManager.DISPLAY_OUTPUT_TYPE_TV;
			mode = android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_PAL;
		}
		Log.d("CMCC-WASU", "set type = " + type + ", mode = " + mode);
		mDm.setDisplayOutputMode(Display.TYPE_BUILT_IN, type, mode);
    }

    /**
    * 获取当前制式
    *
    * @return {@link #DISPLAY_STANDARD_1080P_60}
    * {@link #DISPLAY_STANDARD_1080P_50}
    * {@link #DISPLAY_STANDARD_1080P_30}
    * {@link #DISPLAY_STANDARD_1080P_25}
    * {@link #DISPLAY_STANDARD_1080P_24}
    * {@link #DISPLAY_STANDARD_1080I_60}
    * {@link #DISPLAY_STANDARD_1080I_50}
    * {@link #DISPLAY_STANDARD_720P_60}
    * {@link #DISPLAY_STANDARD_720P_50}
    * {@link #DISPLAY_STANDARD_576P_50}
    * {@link #DISPLAY_STANDARD_480P_60}
    *
    * {@link #DISPLAY_STANDARD_PAL}
    * {@link #DISPLAY_STANDARD_NTSC}
    */
    public int getCurrentStandard() {
        int mode = mDm.getDisplayOutputMode(Display.TYPE_BUILT_IN);
        switch(mode){
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080P_60HZ:
            return DISPLAY_STANDARD_1080P_60;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080P_50HZ:
            return DISPLAY_STANDARD_1080P_50;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080P_24HZ:
            return DISPLAY_STANDARD_1080P_30;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080I_60HZ:
            return DISPLAY_STANDARD_1080I_60;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_1080I_50HZ:
            return DISPLAY_STANDARD_1080I_50;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_720P_60HZ:
            return DISPLAY_STANDARD_720P_60;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_720P_50HZ:
            return DISPLAY_STANDARD_720P_50;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_576P:
            return DISPLAY_STANDARD_576P_50;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_480P:
            return DISPLAY_STANDARD_480P_60;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_PAL:
            return DISPLAY_STANDARD_PAL;
        case android.hardware.display.DisplayManager.DISPLAY_TVFORMAT_NTSC:
            return DISPLAY_STANDARD_NTSC;
        }
        return DISPLAY_UNKNOWN;
    }

    /**
    * 设置屏幕的边距
    *
    * @param left
    *     左边距
    * @param top
    *     上边距
    * @param right
    *     右边距
    * @param bottom
    *     下边距
    */
    public void setScreenMargin(int left, int top, int right, int bottom) {
        int hpercent = changeFromCmcc(left);
        int vpercent = changeFromCmcc(top);
        mDm.setDisplayMargin(Display.TYPE_BUILT_IN, hpercent, vpercent);
        Log.d("CMCC-WASU", String.format("setScreenMargin: left=%d, top=%d, right=%d, bottom=%d",
                left, top, right, bottom));
    }

    /**
    * 获取屏幕的边距
    *
    * @return int[]
    *
    * 数组内容顺序分别为：左边距，上边距，右边距，下边距
    */

    public int[] getScreenMargin() {
        int[] percent = mDm.getDisplayMargin(Display.TYPE_BUILT_IN);
        int[] ret = new int[4];
        ret[0] = changeToCmcc(percent[0]);
        ret[1] = changeToCmcc(percent[1]);
        ret[2] = ret[0];
        ret[3] = ret[1];
        Log.d("CMCC-WASU", "getScreenMargin(" +
            ret[0] + "," + ret[1] + "," +
            ret[2] + "," + ret[3] + ")");
        return ret;
    }

    private int changeFromCmcc(int value){

        if(value < 0){
            return 0;
        }else if(value > 100){
            return 100;
        }
        switch(Config.getTargetPlatform(Config.TARGET_BOARD_PLATFORM)){
        case Config.PLATFORM_JAWS:
		case Config.PLATFORM_DOLPHIN:
            //change 0~100 to 100~50
            //(50 + (value-0)*(100-50)/(100-0))
            return 50 + (100 - value)/2;
        default:
            return value;
        }
    }

    private int changeToCmcc(int value){
        switch(Config.getTargetPlatform(Config.TARGET_BOARD_PLATFORM)){
        case Config.PLATFORM_JAWS:
		case Config.PLATFORM_DOLPHIN:
            //change 50~100 to 100 ~ 0,
            //(0 + (value-50)*(100-0)/(100-50))
            return 100 - (value-50)*2;
        default:
            return value;
        }
    }

    /**
    * 保存参数
    *
    */
    public void saveParams () {
        int type = mDm.getDisplayOutputType(Display.TYPE_BUILT_IN);
        int mode = mDm.getDisplayOutputMode(Display.TYPE_BUILT_IN);
        mDm.saveDisplayResolution(Display.TYPE_BUILT_IN, type, mode);
        int[] percent = mDm.getDisplayMargin(Display.TYPE_BUILT_IN);
        Settings.System.putInt(mCtx.getContentResolver(),
                Settings.System.DISPLAY_AREA_H_PERCENT, percent[0]);
        Settings.System.putInt(mCtx.getContentResolver(),
                Settings.System.DISPLAY_AREA_V_PERCENT, percent[1]);
        Log.d("CMCC-WASU", String.format("saveParams:  type=%d, mode=%d, hpercent=%d, vpercent=%d",
                type, mode, percent[0], percent[1]));
    }
}