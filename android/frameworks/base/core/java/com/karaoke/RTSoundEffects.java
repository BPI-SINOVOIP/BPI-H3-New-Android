package com.cmcc.media;

/**
 * 用于访问并控制卡拉OK演唱时的混响、变声等特效 此类控制的效果应能够实时起作用
 */
public interface RTSoundEffects {
	@Deprecated
	public static final int MODE_REVERB = 1;

	/**
	 * 录音棚音效
	 */
	public static final int REVERB_MODE_STUDIO = 1;
	/**
	 * KTV音效
	 */
	public static final int REVERB_MODE_KTV = 2;
	/**
	 * 演唱会音效
	 */
	public static final int REVERB_MODE_CONCERT = 3;

	/***
	 * <b>已经废弃，使用setReverbMode(param)替代</b><br>
	 * 设置混音效果相关参数， 例如： //设置KTV混响音效 setParam(MODE_REVERB, REVERB_KTV);
	 * 
	 * @param key
	 *            参数mode
	 * @param param
	 *            参数值
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	@Deprecated
	public int setParam(int mode, int param);

	/**
	 * <b>已经废弃，使用getReverbMode替代</b><br>
	 * 获取音效相关参数，例如：getParam(MODE_REVERB)
	 * 
	 * @param mode 参数mode
	 * @return 参数值
	 */
	@Deprecated
	public int getParam(int mode);

	/***
	 * 设置混响模式
	 * 
	 * @param mode
	 *            混响模式
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public int setReverbMode(int mode);

	/**
	 * 获取当前使用的混响模式
	 * 
	 * @return 混响模式
	 */
	public int getReverbMode();

}
