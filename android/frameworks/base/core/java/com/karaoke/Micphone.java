package com.cmcc.media;

/**
 * 麦克风相关控制接口
 */
public interface Micphone {
	/**
	 * 初始化
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public int initial();

	/**
	 * 开启麦克风回放
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public int start();

	/**
	 * 暂停麦克风回放
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
    public int pause();

	/**
	 * 恢复麦克风回放
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
    public int resume();

	/**
	 * 停止麦克风回放
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public int stop();

	/**
	 * 释放系统资源
	 * 
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public int release();

	/**
	 * 获取麦克风数量
	 * 
	 * @return 实际连接到机顶盒的麦克风数量。对于USB外设方案，如果当前机顶盒没有连接USB设备，则返回-1
	 */
	public int getMicNumber();

	/**
	 * 设置麦克风音量
     * <b>注：此设置的应为麦克风输入音量，即如果设置的麦克风音量为0，那么应同时符合下面的要求：<br/>
     * 1、AudioRecord读取到的麦克风PCM数据应为全0的静音数据；<br/>
     * 2、MediaRecorder的混音结果应为纯伴奏
     * </b>
	 * @param volume
	 *            0-100
	 * @return 返回码，大于等于0正常，小于0失败
	 */
	public int setVolume(int volume);

	/**
	 * 获得麦克风音量值
	 * 
	 * @return 麦克风音量值
	 */
	public int getVolume();

}
