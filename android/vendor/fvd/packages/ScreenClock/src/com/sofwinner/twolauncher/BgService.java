package com.sofwinner.twolauncher;

import java.lang.ref.WeakReference;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ListView;
import android.view.Display;
import android.view.WindowManager;



public class BgService extends Service{
	
	private static final String TAG = "BgService";
	
    private DisplayManager mDisplayManager;
    private int mNextImageNumber;

	 private class ServiceStub extends IBgService.Stub{
	    WeakReference<BgService> mService;
	    
	    ServiceStub(BgService service) {
	        mService = new WeakReference<BgService>(service);
	    }
	    
	    public void setPage(int page){
	    	mService.get().setPage(page);
	    }
	    
	    public void setPath(String path){
	    	mService.get().setPath(path);
	    }
	}
	
	private final IBinder mBinder = new ServiceStub(this);
	

    // The content that we want to show on the presentation.
    private static final int[] PHOTOS = new int[] {
        R.drawable.frantic,
        R.drawable.photo1, R.drawable.photo2, R.drawable.photo3,
        R.drawable.photo4, R.drawable.photo5, R.drawable.photo6,
        R.drawable.sample_4,
    };
    
    private int getNextPhoto() {
        final int photo = mNextImageNumber;
        mNextImageNumber = (mNextImageNumber + 1) % PHOTOS.length;
        return photo;
    }
    
    private void showPresentation(Display display, PresentationContents contents) {
        final int displayId = display.getDisplayId();

        Log.d(TAG, "Showing presentation photo #" + contents.photo
                + " on display #" + displayId + ".");

        DemoPresentation presentation = new DemoPresentation(this, display, contents);
        presentation.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        presentation.show();
        
        //presentation.setOnDismissListener(mOnDismissListener);
        //mActivePresentations.put(displayId, presentation);
    }
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	 if (intent != null) {
    		 mDisplayManager = (DisplayManager)this.getSystemService(DISPLAY_SERVICE);
    		 int displayID = intent.getIntExtra("DISPLAY_ID", 0);
    		 Display display = mDisplayManager.getDisplay(displayID);
    	     PresentationContents contents = new PresentationContents(getNextPhoto());
    	     showPresentation(display, contents);
    	 }
    	return 0;
    }
    
    public void setPage(int page){
    	Log.d(TAG,"setPage page = " + page);
    }
    
    public void setPath(String path){
    	Log.d(TAG,"setPath path = " + path);
    }
    
}