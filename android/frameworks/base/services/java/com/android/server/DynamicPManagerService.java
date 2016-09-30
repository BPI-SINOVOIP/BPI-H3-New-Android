package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.Settings;
import android.net.Uri;
import android.os.Binder;
import android.os.DynamicPManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.IDynamicPManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.SystemClock;

import android.util.Log;
import android.util.Slog;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DynamicPManagerService extends IDynamicPManager.Stub
{
	private static final String TAG= "DynamicPManagerService";
	private static final boolean DEBUG = true;

	private static final char STRING_QUOT      = '\"';
	private static final char STRING_BLANK      = ' ';

	private ClientList clients = new ClientList();
	private Context mContext;
	private SettingsObserver mSettingsObserver;
	private Handler mHandler;
	private int defFlag= DynamicPManager.CPU_MODE_NORMAL;
	private boolean mMaxPowerEnable;

	private boolean mBootCompleted = false;
	private int mPowerState=-1; // current power state
	private int newPowerState=0; // new power state

    private List<String> performanceStepList = new ArrayList<String>();
    private List<String> normalStepList = new ArrayList<String>();
    private List<String> bootFinishStepList = new ArrayList<String>();

	public DynamicPManagerService(Context context){
		mContext = context;
		mHandler = new Handler();
        nativeInit();
	}

    private native void nativeInit();

    private void readDynamicPManagerConfig()
    {
        String[] performanceStep = mContext.getResources().getStringArray(com.android.internal.R.array.performance_mode);
        String[] normalStep = mContext.getResources().getStringArray(com.android.internal.R.array.normal_mode);
        String[] bootFinishStep = mContext.getResources().getStringArray(com.android.internal.R.array.boot_finish);
        if (DEBUG)
            Log.d(TAG,"boot finish step: ");
        for ( String step : bootFinishStep)
        {
            if (DEBUG)
                Log.d(TAG,step);
            bootFinishStepList.add(step);
        }
        if (DEBUG)
            Log.d(TAG,"performance step: ");
        for ( String step : performanceStep)
        {
            if (DEBUG)
                Log.d(TAG,step);
            performanceStepList.add(step);
        }
        if (DEBUG)
            Log.d(TAG,"normal step: ");
        for ( String step : normalStep)
        {
            if (DEBUG)
                Log.d(TAG,step);
            normalStepList.add(step);
        }
    }

    private void parseAndExcuteOneStep(String step)
    {
        List<String> argv = new ArrayList<String>();
        int start = 0;
        int next = 0;
        while (start<step.length())
        {
            //remove begin space
            while (start<step.length() && step.charAt(start)==STRING_BLANK)
                start++;
            char startChar = 0;
            try {
                startChar = step.charAt(start);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            if (startChar==STRING_QUOT)
            {
                next=step.indexOf(STRING_QUOT,start+1);
                if (next==-1)
                {
                    // has begin quote not have end quote
		            Log.w(TAG," parse mode step maybe not completed : " + step);
                    break;
                }
                else if (next-1==start)
                {
                    //emtpy content , continue
                    start = next+1;
                    continue;
                }
                else
                {
                    try{
                        argv.add(step.substring(start+1,next));
                    }catch (IndexOutOfBoundsException e){
		                Log.i(TAG," parse mode step error : " + e.getMessage());
                        break;
                    }
                    start = next+1;
                }
            }
            else
            {
                next=step.indexOf(STRING_BLANK,start+1);
                if (next==-1)
                    next=step.length();
                try{
                    argv.add(step.substring(start,next));
                }catch (IndexOutOfBoundsException e){
		            Log.i(TAG," parse mode step error : " + e.getMessage());
                }
                start=next;
            }
        }
        for (int i=0; i<argv.size();i++)
		    Log.i(TAG," argv["+i+"] : " + argv.get(i));
        if (argv.size()!=2)
        {
		    Log.e(TAG," step \'" + step +" \' is error" );
            return;
        }

        try {
		        FileWriter wr = new FileWriter(argv.get(0));
				wr.write(argv.get(1));
				wr.close();
		}catch(IOException e){
		    Log.i(TAG," write "+argv.get(0)+" \""+argv.get(1)+"\"  error: " + e.getMessage());
		}
    }

	public void systemReady() {
		Slog.i(TAG,"DynamicPManagerService systemReady");
		mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(Intent.ACTION_BOOT_COMPLETED), null, null);
		mSettingsObserver = new SettingsObserver(mHandler);
		final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.CPU_FAST_ENABLE),
                    false, mSettingsObserver, UserHandle.USER_ALL);
        readDynamicPManagerConfig();
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();

            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                setBootFinishPolicy();
                newPowerState=DynamicPManager.CPU_MODE_NORMAL;
				updateSettingsLocked();
				updateDynamicPower();
				mBootCompleted = true;
	        }
	    }
	};

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
			handleSettingsChangedLocked();
        }
    }

	private void handleSettingsChangedLocked(){
		updateSettingsLocked();
		updateDynamicPower();
	}

	private void updateSettingsLocked(){
		final ContentResolver resolver = mContext.getContentResolver();
        mMaxPowerEnable = (Settings.System.getIntForUser(resolver,
                Settings.System.CPU_FAST_ENABLE,0,UserHandle.USER_CURRENT) != 0)?true:false;
		Slog.d(TAG,"mMaxPowerEnable = " + mMaxPowerEnable);
	}


	private void updateDynamicPower(){

		Slog.d(TAG,"updateDynamicPower");

        if (mPowerState == newPowerState)
            return;
        List<String> stepList;
        switch (newPowerState)
        {
            case DynamicPManager.CPU_MODE_PERFORMENCE:
                stepList=performanceStepList;
                break;
            case DynamicPManager.CPU_MODE_NORMAL:
                stepList=normalStepList;
                break;
            default:
                return;
        }
        if (stepList.size()>0)
        {
            for (int i = 0; i < stepList.size(); i++)
                parseAndExcuteOneStep(stepList.get(i));
        }
        mPowerState = newPowerState;
	}

	private void acquireCpuFreqLockLocked(IBinder b, int flag)
	{
		Client ci = new Client(flag, b);
		clients.addClient(ci);

        newPowerState = clients.gatherState();
        updateDynamicPower();
	}

	public void acquireCpuFreqLock(IBinder b, int flag)
	{
		long ident = Binder.clearCallingIdentity();
		try {
			synchronized(clients){
				acquireCpuFreqLockLocked(b, flag);
			}
		}finally{
			Binder.restoreCallingIdentity(ident);
		}
	}

	private void releaseCpuFreqLockLocked(IBinder b)
	{
		Client ci = clients.rmClient(b);
		if( ci == null)
			return;

		ci.binder.unlinkToDeath(ci, 0);

        newPowerState=clients.gatherState();
        updateDynamicPower();
	}

	public void releaseCpuFreqLock(IBinder b)
	{
		long ident = Binder.clearCallingIdentity();
		try {
			synchronized(clients){
				releaseCpuFreqLockLocked(b);
			}
		}finally{
			Binder.restoreCallingIdentity(ident);
		}
	}
    private void setBootFinishPolicy()
    {
        if (bootFinishStepList.size()>0)
        {
            for (int i = 0; i < bootFinishStepList.size(); i++)
                parseAndExcuteOneStep(bootFinishStepList.get(i));
        }
    }

    private void reset()
    {
        if (DEBUG) Log.d(TAG,"resetFromNative");
        mPowerState = -1;
        newPowerState=clients.gatherState();
        updateDynamicPower();
    }

	private class Client implements IBinder.DeathRecipient
	{
		Client(int flag, IBinder b) {
			this.binder = b;
			this.flag = flag;

			try {
                b.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
		}

		public void binderDied() {	// client be kill, so we will delete record in ArrayList
			synchronized(clients){
				releaseCpuFreqLockLocked(binder);
			}
		}

		final IBinder binder;
		final int flag;	  		// client request flag
 	}

	private class ClientList extends ArrayList<Client>
	{
		void addClient(Client ci)
		{
			int index = getIndex(ci.binder);
			if( index < 0 )
			{
				this.add(ci);
			}
		}
		Client rmClient(IBinder b)
		{
			int index = getIndex(b);
			if( index >= 0 )
			{
				return this.remove(index);
			}else{
				return null;
			}
		}
		int getIndex(IBinder b)
		{
			int N = this.size();
			for(int i=0; i<N; i++)
			{
				if( this.get(i).binder == b)
					return i;
			}
			return -1;
		}
		int gatherState()
		{
		    if( defFlag ==  DynamicPManager.CPU_MODE_PERFORMENCE ){
		        return DynamicPManager.CPU_MODE_PERFORMENCE;
		    }

		    int ret = DynamicPManager.CPU_MODE_NORMAL;
			int N = this.size();
			for( int i=0; i<N; i++){
				Client ci = this.get(i);
				if( (ci.flag & DynamicPManager.CPU_MODE_PERFORMENCE) != 0){
				    ret = DynamicPManager.CPU_MODE_PERFORMENCE;
				    break;
				}
			}
			return ret;
		}
	}

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("DynamicPManagerService:");
        pw.println("  mPowerState:"+mPowerState);
        pw.println("  mBootCompleted:"+(mBootCompleted?"True":"False"));
        pw.println("  client size:" + clients.size());
        for (int i=0;i<clients.size();i++)
            pw.println("  client "+i+" : " + clients.get(i).flag);
        List<String> stepList;
        pw.println("  bootFinishStep:");
        stepList=bootFinishStepList;
        for (int i = 0; i < stepList.size(); i++)
            pw.println("    "+stepList.get(i));

        pw.println("  normalStep:");
        stepList=normalStepList;
        for (int i = 0; i < stepList.size(); i++)
            pw.println("    "+stepList.get(i));

        pw.println("  performanceStep:");
        stepList=performanceStepList;
        for (int i = 0; i < stepList.size(); i++)
            pw.println("    "+stepList.get(i));

    }

}


