package com.cmcc.media;

/**
* 扩展的MediaPlayer,除提供MediaPlayer的基本功能以外的扩展功能
*/
public interface MediaPlayerEx{
    public enum CHANNEL{
        LEFT, // 左声道
        RIGHT, // 右声道
        CENTER  // 左+右声道
    }
    /**
     * 切换到指定声道并返回切换后的声道,切换音频文件播放的左右声道后，必须左右喇叭均同时播放选中的声道的声音。
     * @param channel 设置成功，返回新设置的channel；设置失败，返回原来的channel。
    */
    public CHANNEL switchChannel(CHANNEL channel);
	/**
    * MediaPlayer�ڲ���˫����MP4�ļ�ʱ���Ƿ�֧���������п�ѡ���ԣ�<br/>
    * 1��֧�ֲ��Ź����н��������л���<br/>
    * 2���л������ʱ������100���룻<br/>
    * 3���л����첻����ɲ���ʱ��������䡣
    * @return ֧�ַ���true����֧�ַ���false
    */
    public boolean canSelectTrackImmediately();
}
