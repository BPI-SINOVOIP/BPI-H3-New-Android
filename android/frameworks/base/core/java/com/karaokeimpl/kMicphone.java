package com.karaokeimpl;

import android.util.Log;
import android.content.Context;
import com.cmcc.media.Micphone;


/**
 * Called by karaoke media's music apk
 * Control micphone
 */
public class kMicphone implements Micphone{

    static{
        System.loadLibrary("karaokejni");
    }

    private static String TAG = "kMicphone";
    private Context mContext;
	//private MicphoneService mMicphoneService = null;
    private boolean mInited = false;
    private boolean mStarted = false;
    private boolean mPaused = false;
    

    public kMicphone(Context context){
        Log.d(TAG, "kMicphone start");
        mContext = context;
    }

	/**
	 * initialized all the source
	 * 
	 * @return negetive value means failure
     *          bigger than or equals to zero means success
	 */

	public synchronized int initial()
    {
        Log.d(TAG, "initial()");
        if(mInited){
            Log.d(TAG, "already initial");
            return -1;
        }
        int ret = native_init();
        if(ret < 0)
            Log.d(TAG, "native_init failed , return " + ret);
        else
            mInited = true;
        return ret;
    }

	/**
	 * 开启麦克风回放
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public synchronized int start()
    {
        Log.d(TAG, "start()");
        if(!mInited){
            Log.d(TAG, "not initial()");
            return -1;
        }
        if(mStarted){
            Log.d(TAG, "has been started");
            return 0;
        }
        int ret = native_start();
        if(ret < 0)
            Log.d(TAG, "native_start failed , return " + ret);
        else
            mStarted = true;
         
        return ret;
    }

	/**
	 * 暂停麦克风回放
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
    public synchronized int pause()
    {
        Log.d(TAG, "pause()");
        if(!mStarted){
            Log.d(TAG, "not started!");
            return -1;
        }
        if(mPaused){
            Log.d(TAG, "has been pasued");
            return -1;
        }
        int ret = native_pause();
        if(ret < 0)
            Log.d(TAG, "native_pause failed, return "  + ret);
        else
            mPaused = true;
        return ret;
    }

	/**
	 * 恢复麦克风回放
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
    public synchronized int resume()
    {
        Log.d(TAG, "resume()");
        if(!mStarted){
            Log.d(TAG, "not started!");
            return -1;
        }
        if(!mPaused){
            Log.d(TAG, "no need resume");
            return -1;
        }
        int ret = native_resume();
        if(ret < 0)
            Log.d(TAG, "native_resume failed, return " + ret);
        mPaused = false;
        return ret;
    }

	/**
	 * 停止麦克风回放
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public synchronized int stop()
    {
        Log.d(TAG, "stop()");
        if(!mStarted){
            Log.d(TAG, "not started, can not stop");
            return -1;
        }
        int ret = native_stop();
        if(ret < 0)
            Log.d(TAG, "native_stop failed, return " + ret);
        mStarted = false;
        return ret;
    
    }

	/**
	 * 释放系统资源
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public synchronized int release()
    {
        Log.d(TAG, "release");
        if(!mInited){
            Log.d(TAG, "not inited");
            return -1;
        }
        int ret = native_release();
        if(ret < 0)
            Log.d(TAG, "native_release failed, return "+ ret);
        mInited = false;
        return ret;
    }

	/**
	 * 获取麦克风数量
	 * 
	 * @return 实际连接到机顶盒的麦克风数量。对于USB外设方案，如果当前机顶盒没有连接USB设备，则返回-1
	 */
	public synchronized int getMicNumber()
    {
        Log.d(TAG, "getMicNumber()");
        return 2;
    }

	/**
	 * 设置麦克风音量
	 * 
	 * @param volume
	 *            0-100
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public synchronized int setVolume(int volume)
    {
        Log.d(TAG, "setVolume, volue =" + volume);
        if(!mInited){
            Log.d(TAG, "not inited");
            return -1;
        }
        int ret = native_setVolume(volume);
        if(ret < 0)
            Log.d(TAG, "native_setVolume failed, return " + ret);
        return ret;
    
    }

	/**
	 * 获得麦克风音量值
	 * 
	 * @return 麦克风音量值
	 */
	public synchronized int getVolume()
    {
        Log.d(TAG, "getVolume()");
        if(!mInited){
            Log.d(TAG, "not inited");
            return -1;
        }
        int volume = native_getVolume();
        Log.d(TAG, "native_getVolume return " + volume);
        if(volume < 0)
            Log.w(TAG, "get a invalid volume from native");
        return volume;
    }

    /****************************************************native method*************************************************************/
    private native int native_init();
    private native int native_start();
    private native int native_pause();
    private native int native_resume();
    private native int native_stop();
    private native int native_release();
    private native int native_setVolume(int volume);
    private native int native_getVolume();

}
