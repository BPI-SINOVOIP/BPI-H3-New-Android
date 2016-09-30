package com.cmcc.media;

/**
 * 扩展系统MediaRecorder的功能
 */
public interface MediaRecorderEx {
	/**
	 * 在演唱过程中用户点击暂停时，通过此方法，让MediaRecorder的AAC混音编码功能暂停
	 */
	public void pause();
	
	/**
	 * 恢复已被暂停的MediaRecorder AAC混音编码功能
	 */
	public void resume();
}
