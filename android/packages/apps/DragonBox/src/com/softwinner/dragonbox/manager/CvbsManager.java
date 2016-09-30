package com.softwinner.dragonbox.manager;

import android.content.Context;
import android.media.AudioManager;

import com.softwinner.dragonbox.platform.DisplayManagerPlatform;
import com.softwinner.dragonbox.platform.IDisplayManagerPlatform;
import com.softwinner.dragonbox.utils.AudioChannelUtil;
import com.softwinner.dragonbox.utils.MusicerUtil;

public class CvbsManager {

	private Context mContext;
	private IDisplayManagerPlatform mDisplayManagerPlatform;
	private MusicerUtil mMusicUtil;
	
	private boolean mLeftPlaySuccess = false;
	private boolean mRightPlaySuccess = false;
	
	public CvbsManager(Context context) {
		mContext = context;
		mDisplayManagerPlatform = new DisplayManagerPlatform(mContext);
		mMusicUtil = MusicerUtil.getInstance(context);
	}

	public void changeToCvbs() {
		mDisplayManagerPlatform.changeToCVBS();
		AudioChannelUtil.setOuputChannels(mContext, AudioManager.AUDIO_NAME_CODEC);
		
	}

	public void playLeft(){
		mMusicUtil.playMusic(MusicerUtil.PLAY_LEFT);
	}
	
	public void playRight(){
		mMusicUtil.playMusic(MusicerUtil.PLAY_RIGHT);
	}
	
	public void stopPlaying(){
		mMusicUtil.pause();
	}

	public boolean isLeftPlaySuccess() {
		return mLeftPlaySuccess;
	}

	public void setLeftPlaySuccess(boolean leftPlaySuccess) {
		this.mLeftPlaySuccess = leftPlaySuccess;
	}

	public boolean isRightPlaySuccess() {
		return mRightPlaySuccess;
	}

	public void setRightPlaySuccess(boolean rightPlaySuccess) {
		this.mRightPlaySuccess = rightPlaySuccess;
	}

	public boolean getResult () {
		return isLeftPlaySuccess() && isRightPlaySuccess();
	}
	public boolean isCvbsStatusConn(){
		return mDisplayManagerPlatform.getTvHotPlugStatus();
	}
	
}
