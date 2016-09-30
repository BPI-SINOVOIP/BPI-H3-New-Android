package com.karaokeimpl;

import android.util.Log;
import android.content.Context;
import com.cmcc.media.RTSoundEffects;


/**
 *  Called by karaoke media's music apk
 *  Set Karaoke Sound effects
 */
public class kRTSoundEffects implements RTSoundEffects{
    private static String TAG = "kRTSoundEffects";
    private Context mContext;


    public kRTSoundEffects(Context context){
        Log.d(TAG, "kRTSoundEffects()");
        mContext = context;
    }
	/***
	 * 设置混音效果相关参数， 例如： //设置KTV混响音效 setParam(MODE_REVERB, REVERB_KTV);
	 * 
	 * @param key
	 *            参数mode
	 * @param param
	 *            参数值
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public synchronized int setParam(int mode, int param)
    {
        Log.d(TAG, "setParam, mode = " + mode + ", param = " + param);
        int ret = native_setParam(mode, param);
        if(ret < 0)
            Log.d(TAG, "native_setParam failed , return " + ret);
        return ret;
    }

    public synchronized int setReverbMode(int mode)
    {
        Log.d(TAG, "setReverbMode, mode = " + mode);
        int ret = native_setParam(1, mode);
        if (ret < 0)
            Log.d(TAG, "setReverbMode native_setParam failed, return " + ret);
        return ret;
    }

	/**
	 * 获取音效相关参数，例如：getParam(MODE_REVERB)
	 * 
	 * @param mode 参数mode
	 * @return 参数值
	 */
	public synchronized int getParam(int mode)
    {
        Log.d(TAG, "getParam, mode = " + mode);
        int ret = native_getParam(mode);
        if(ret < 0)
            Log.d(TAG, "native_getParam failed, return " + ret);
        return ret;
    }

    public synchronized int getReverbMode()
    {
        Log.d(TAG, "getReverbMode");
        int ret = native_getParam(1);
        if (ret < 0)
            Log.d(TAG, "getReverbMode native_getParam failed, return" + ret);
        return ret;
    }


    /****************************************************native method*************************************************************/
    private native int native_setParam(int mode, int param);
    private native int native_getParam(int mode);

}
