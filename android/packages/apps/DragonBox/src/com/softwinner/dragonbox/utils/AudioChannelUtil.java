package com.softwinner.dragonbox.utils;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;

public class AudioChannelUtil {

	public AudioChannelUtil() {
		
	}

	/**
	 * set the output channel, when test output ,it always need set channel
	 * @param context
	 * @param channels may use AudioManager.AUDIO_NAME_***
	 * @return the ordinary channels.the values before changed.
	 */
	public static List<String> setOuputChannels(Context context, String ...channels) {
		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		final ArrayList<String> audioOutputChannels = am
				.getActiveAudioDevices(AudioManager.AUDIO_OUTPUT_ACTIVE);

		ArrayList<String> allOutPutchannels = new ArrayList<String>();
		for (String channel : channels) {
			allOutPutchannels.add(channel);
		}

		am.setAudioDeviceActive(allOutPutchannels,
				AudioManager.AUDIO_OUTPUT_ACTIVE);
		return audioOutputChannels;
	}

}
