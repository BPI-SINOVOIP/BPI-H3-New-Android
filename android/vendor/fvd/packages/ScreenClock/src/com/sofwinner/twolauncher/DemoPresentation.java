package com.sofwinner.twolauncher;

import android.app.Presentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sofwinner.twolauncher.widget.LEDView;

/**
 * The presentation to show on the secondary display.
 *
 * Note that the presentation display may have different metrics from the display on which
 * the main activity is showing so we must be careful to use the presentation's
 * own {@link Context} whenever we load resources.
 */
 public class DemoPresentation extends Presentation {

	 
	    // The content that we want to show on the presentation.
	 private static final int[] PHOTOS = new int[] {
	        R.drawable.frantic,
	        R.drawable.photo1, R.drawable.photo2, R.drawable.photo3,
	        R.drawable.photo4, R.drawable.photo5, R.drawable.photo6,
	        R.drawable.sample_4,
	};

	private static final String TAG = "DemoPresentation";
	private static final int  UPDATE_BACKGROUND = 1;

	protected static final int TIMED_UPDATE_BACKGROUND = 2;
	    
    final PresentationContents mContents;
    private LEDView ledView; 
    private ImageView mImage;
    private Context mContext;
    private int count = 0;
    private Handler mHandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		Resources r = getContext().getResources();
    		switch(msg.what){
    		case UPDATE_BACKGROUND:
    			if(msg.arg2==1){
    				
    				int photo = 0;
    				if(msg.arg1==KeyEvent.KEYCODE_0){
    					photo = 0;
    					mImage.setBackgroundDrawable(r.getDrawable(PHOTOS[photo]));
    				}else if(msg.arg1==KeyEvent.KEYCODE_1){
    					photo = 1;
    					mImage.setBackgroundDrawable(r.getDrawable(PHOTOS[photo]));
    				}else if(msg.arg1==KeyEvent.KEYCODE_2){
    					photo = 2;
    					mImage.setBackgroundDrawable(r.getDrawable(PHOTOS[photo]));
    				}else if(msg.arg1==KeyEvent.KEYCODE_3){
    					photo = 3;
    					mImage.setBackgroundDrawable(r.getDrawable(PHOTOS[photo]));
    				}else if(msg.arg1==KeyEvent.KEYCODE_4){
    					photo = 4;
    					mImage.setBackgroundDrawable(r.getDrawable(PHOTOS[photo]));
    				}else if(msg.arg1==KeyEvent.KEYCODE_5){
    					photo = 5;
    					mImage.setBackgroundDrawable(r.getDrawable(PHOTOS[photo]));
    				}else if(msg.arg1==KeyEvent.KEYCODE_INFO){
    					photo = (int)Math.floor((Math.random()*PHOTOS.length));
    					mImage.setBackgroundDrawable(r.getDrawable(PHOTOS[photo]));
    				}else if(msg.arg1==10005){
    					mImage.setBackgroundColor(0x000000);
    				}
    			}
    			break;
    		case TIMED_UPDATE_BACKGROUND:
    			if(++count>=PHOTOS.length){
    				count = 0;
    			}
    			mImage.setBackgroundDrawable(r.getDrawable(PHOTOS[count]));
    			sendEmptyMessageDelayed(TIMED_UPDATE_BACKGROUND,1000*10);
    			break;
    		}
    	}
    };

    public DemoPresentation(Context context, Display display, PresentationContents contents) {
        super(context, display);
        mContents = contents;
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        // Get the resources for the context of the presentation.
        // Notice that we are getting the resources from the context of the presentation.
        Resources r = getContext().getResources();

        // Inflate the layout.
        setContentView(R.layout.presentation_content);
        
        ledView = (LEDView) findViewById(R.id.ledview);  

        final Display display = getDisplay();
        final int displayId = display.getDisplayId();
        final int photo = mContents.photo;

        // Show a caption to describe what's going on.
        //TextView text = (TextView)findViewById(R.id.text);
        /*text.setText(r.getString(R.string.presentation_photo_text,
                photo, displayId, display.getName()));*/

        // Show a n image for visual interest.
        mImage = (ImageView)findViewById(R.id.image);
        mImage.setBackgroundDrawable(r.getDrawable(PHOTOS[0]));

        //GradientDrawable drawable = new GradientDrawable();
       // drawable.setShape(GradientDrawable.RECTANGLE);
        //drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);

        // Set the background to a random gradient.
        Point p = new Point();
        getDisplay().getSize(p);
        //drawable.setGradientRadius(Math.max(p.x, p.y) / 2);
        //drawable.setColors(mContents.colors);
        //findViewById(android.R.id.content).setBackground(drawable);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.KEYEVENT");
        mContext.registerReceiver(new KeyEventReceiver(), filter);
        mHandler.sendEmptyMessageDelayed(TIMED_UPDATE_BACKGROUND,1000*5);

    }
    
    @Override
    protected void onStart(){
    	super.onStart();
    	ledView.start();
    }
    @Override  
    protected void onStop() { 
    	super.onStop();
    	ledView.stop();
    }
    
    
    private class KeyEventReceiver extends BroadcastReceiver{
    	
    	@Override
    	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
    		if(intent.getAction().equals("android.intent.action.KEYEVENT")){
    			int keyCode = intent.getIntExtra("keyCode", 0);
    			boolean down = intent.getBooleanExtra("down", false);
    			Log.d(TAG,"onReceive the broadcast " + " keycode = " + keyCode + " down = " + down);
    			Message msg = new Message();
    			msg.what = UPDATE_BACKGROUND;
    			msg.arg1 = keyCode;
    			msg.arg2 = down?1:0;
    			mHandler.sendMessage(msg);
    		}
    	}
    }

}
