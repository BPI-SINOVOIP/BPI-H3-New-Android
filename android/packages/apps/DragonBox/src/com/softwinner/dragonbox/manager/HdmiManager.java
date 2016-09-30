package com.softwinner.dragonbox.manager;

import android.content.Context;
import android.media.AudioManager;

import com.softwinner.dragonbox.platform.DisplayManagerPlatform;
import com.softwinner.dragonbox.platform.IDisplayManagerPlatform;
import com.softwinner.dragonbox.utils.AudioChannelUtil;
import com.softwinner.dragonbox.utils.MusicerUtil;

public class HdmiManager {

	private Context mContext;
	private IDisplayManagerPlatform mDisplayManagerPlatform;

	public HdmiManager(Context context) {
		mContext = context;
		mDisplayManagerPlatform = new DisplayManagerPlatform(context);
	}

	public void changeToHDMI() {
		mDisplayManagerPlatform.changeToHDMI();
		AudioChannelUtil.setOuputChannels(mContext, AudioManager.AUDIO_NAME_HDMI);
		
	}

	public void playMusic(){
		MusicerUtil.getInstance(mContext).playMusic(MusicerUtil.PLAY_NORMAL);
	}
	
	public void stopMusic(){
		MusicerUtil.getInstance(mContext).pause();
	}
	
	public boolean isHDMIStatusConn(){
		return mDisplayManagerPlatform.getHdmiHotPlugStatus();
	}
	
}
