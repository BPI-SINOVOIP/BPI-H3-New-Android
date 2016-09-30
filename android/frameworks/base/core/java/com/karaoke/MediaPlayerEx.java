package com.cmcc.media;

/**
* æ‰©å±•çš„MediaPlayer,é™¤æä¾›MediaPlayerçš„åŸºæœ¬åŠŸèƒ½ä»¥å¤–çš„æ‰©å±•åŠŸèƒ½
*/
public interface MediaPlayerEx{
    public enum CHANNEL{
        LEFT, // å·¦å£°é“
        RIGHT, // å³å£°é“
        CENTER  // å·¦+å³å£°é“
    }
    /**
     * åˆ‡æ¢åˆ°æŒ‡å®šå£°é“å¹¶è¿”å›åˆ‡æ¢åçš„å£°é“,åˆ‡æ¢éŸ³é¢‘æ–‡ä»¶æ’­æ”¾çš„å·¦å³å£°é“åï¼Œå¿…é¡»å·¦å³å–‡å­å‡åŒæ—¶æ’­æ”¾é€‰ä¸­çš„å£°é“çš„å£°éŸ³ã€‚
     * @param channel è®¾ç½®æˆåŠŸï¼Œè¿”å›æ–°è®¾ç½®çš„channelï¼›è®¾ç½®å¤±è´¥ï¼Œè¿”å›åŸæ¥çš„channelã€‚
    */
    public CHANNEL switchChannel(CHANNEL channel);
	/**
    * MediaPlayerÔÚ²¥·ÅË«Òô¹ìMP4ÎÄ¼şÊ±£¬ÊÇ·ñÖ§³ÖÏÂÃæËùÓĞ¿ÉÑ¡ÌØĞÔ£º<br/>
    * 1¡¢Ö§³Ö²¥·Å¹ı³ÌÖĞ½øĞĞÒô¹ìÇĞ»»£»<br/>
    * 2¡¢ÇĞ»»Òô¹ìºÄÊ±²»¸ßÓÚ100ºÁÃë£»<br/>
    * 3¡¢ÇĞ»»Òô¹ì²»»áÔì³É²¥·ÅÊ±¼äÖáµÄÌø±ä¡£
    * @return Ö§³Ö·µ»Øtrue£¬²»Ö§³Ö·µ»Øfalse
    */
    public boolean canSelectTrackImmediately();
}
