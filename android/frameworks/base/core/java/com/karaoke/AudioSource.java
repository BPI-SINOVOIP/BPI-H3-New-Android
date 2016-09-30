package com.cmcc.media;

/**
 * 扩展了android.media.MediaRecorder.AudioSource中定义的音频源
 */
public interface AudioSource {
	/**
	 * 卡拉OK应用读取MIC原始PCM数据使用的音频源
	 */
	public static final int CMCC_KARAOK_MIC = 1000;
	/**
	 * 卡拉OK应用读取混音输出数据使用的音频源
	 */
	public static final int CMCC_KARAOK_SPEAKER = 1001;
}
