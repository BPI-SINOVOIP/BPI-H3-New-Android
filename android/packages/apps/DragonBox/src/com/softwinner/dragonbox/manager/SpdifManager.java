package com.softwinner.dragonbox.manager;

import android.content.Context;

import com.softwinner.dragonbox.utils.MusicerUtil;


public class SpdifManager {
	
	private MusicerUtil mMusicUtil;
	
	private boolean mLeftPlaySuccess = false;
	private boolean mRightPlaySuccess = false;
	
	public SpdifManager(Context context) {
		mMusicUtil = MusicerUtil.getInstance(context);
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
	
}
