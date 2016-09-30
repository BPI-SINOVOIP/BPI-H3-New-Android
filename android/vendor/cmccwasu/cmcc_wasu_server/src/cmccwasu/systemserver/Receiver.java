package cmccwasu.systemserver;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;
import android.os.SystemProperties;
import android.provider.Settings;

public class Receiver extends BroadcastReceiver{
	private static final String TAG = "CMCC-WASU";
	
	private Context mContext = null;
	private class SettingObserver extends ContentObserver{

		public SettingObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		private void observe(){
			ContentResolver resolver = mContext.getContentResolver();
			//这里注册需要监听的数据库改变事件,如:
			//resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, this);
			
			resolver.registerContentObserver(Settings.System.getUriFor(Settings.Secure.DEFAULT_SCREEN_RATIO), false, this);
			resolver.registerContentObserver(Settings.System.getUriFor(Settings.Secure.DEFAULT_PLAYER_QUALITY), false, this);
			
			update();
		}

		@Override
		public void onChange(boolean selfChange) {
			update();
		}
		
		public void update(){
			//这里对以上注册的监听数据库做处理
        	final ContentResolver cr = mContext.getContentResolver();
			
			String val = Settings.Secure.getString(cr, Settings.Secure.DEFAULT_SCREEN_RATIO);
			if (val == null)
				val = "1";
			else if (val.length() == 0)
				val = "1";
			SystemProperties.set("epg.default_screen_ratio", val);
			Log.v(TAG, "set default_screen_ratio = " + val);
			
			val = Settings.Secure.getString(cr, Settings.Secure.DEFAULT_PLAYER_QUALITY);
			if (val == null)
				val = "0";
			else if (val.length() == 0)
				val = "0";
			SystemProperties.set("epg.default_player_quality", val);
			Log.v(TAG, "set default_player_quality = " + val);
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		
		//开始监听数据库改变消息
		SettingObserver observer = new SettingObserver(new Handler());
		observer.observe();
	}
}