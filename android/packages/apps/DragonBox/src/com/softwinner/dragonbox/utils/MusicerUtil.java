package com.softwinner.dragonbox.utils;

import java.util.ArrayList;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.widget.Toast;

import com.softwinner.dragonbox.R;

public class MusicerUtil {

	private final static String TAG = "MusicerUtil";
	private static MusicerUtil instance;

	SoundPool mSoundPool;
	
	Context mContext;

	public static final int PLAY_LEFT = 1;
	public static final int PLAY_RIGHT = 2;
	public static final int PLAY_NORMAL = 3;

	public static synchronized MusicerUtil getInstance(Context context) {
		if (instance == null) {
			instance = new MusicerUtil(context);
		}
		return instance;
	}

	private MusicerUtil(Context context) {
		mContext = context;
		AudioManager mAM = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		ArrayList<String> audioOutputChannels = mAM
				.getActiveAudioDevices(AudioManager.AUDIO_OUTPUT_ACTIVE);

		for (String stc : audioOutputChannels) {
			Log.e(TAG, "=audioOutputChannels=" + "." + stc + ".");
		}
		audioOutputChannels.clear();
		audioOutputChannels.add(AudioManager.AUDIO_NAME_CODEC);
		audioOutputChannels.add(AudioManager.AUDIO_NAME_HDMI);
		audioOutputChannels.add(AudioManager.AUDIO_NAME_SPDIF);

		mAM.setAudioDeviceActive(audioOutputChannels,
				AudioManager.AUDIO_OUTPUT_ACTIVE);
		int maxVolume = mAM.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mAM.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
	}

	/**
	 * 
	 * @param playType
	 *            PLAY_LEFT or PLAY_RIGHT or PLAY_NORMAL
	 */
    public boolean playMusic(int playType) {
        if (mSoundPool != null) {
            mSoundPool.release();
        }
        mSoundPool = new SoundPool(3, AudioManager.STREAM_NOTIFICATION, 1);
        switch (playType) {
            case PLAY_LEFT:
                final int soundLeftId = mSoundPool.load(mContext, R.raw.left, 1);
                mSoundPool
                    .setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                        @Override
                        public void onLoadComplete(SoundPool soundPool,
                            int sampleId, int status) {
                            mSoundPool.play(soundLeftId, 1.0f, 0.0f, 1,
                                -1, 1.0f);
                        }
                    });

                break;
            case PLAY_RIGHT:
                final int soundRightId = mSoundPool.load(mContext, R.raw.right, 1);
                mSoundPool
                    .setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                        @Override
                        public void onLoadComplete(SoundPool soundPool,
                            int sampleId, int status) {
                            mSoundPool.play(soundRightId, 0.0f, 1.0f,
                                1, -1, 1.0f);
                        }
                    });
                break;
            case PLAY_NORMAL:
                final int soundNormalId = mSoundPool.load(mContext, R.raw.hdmi, 1);
                mSoundPool
                    .setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                        @Override
                        public void onLoadComplete(SoundPool soundPool,
                            int sampleId, int status) {
                            mSoundPool.play(soundNormalId, 1.0f, 1.0f, 1,
                                -1, 1.0f);
                        }
                    });
                break;
            default:
                break;
        }
        return false;
    }

	public void pause() {
		mSoundPool.autoPause();

	}

	public void resume() {
		mSoundPool.autoResume();
	}

	public void release() {
		mSoundPool.release();
		instance = null;
	}
}
