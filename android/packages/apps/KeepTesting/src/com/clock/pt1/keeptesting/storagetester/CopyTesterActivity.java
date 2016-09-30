package com.clock.pt1.keeptesting.storagetester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.clock.pt1.keeptesting.LogUtil;
import com.clock.pt1.keeptesting.R;
import com.clock.pt1.keeptesting.storagetester.SwStorageManager.OnVolumeChangeListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class CopyTesterActivity extends Activity implements RadioGroup.OnCheckedChangeListener,View.OnClickListener, OnVolumeChangeListener{

	public static final String TAG = "CopyTester";
	
	public static final String FROM_DIR = "from7612dfdsfsd";
	
	public static final String TO_DIR = "to45dfadfdfdadaa";
	
	public static final String NORMAL_FILE = "normal154645";

	//Fragment size
	public static final String[] FRAGMENT_TOTAL_SIZE = { "64 MB", "128 MB",
			"256 MB", "512 MB", "1024 MB", "2048 MB", "4096 MB", };
	
	public static final long[] FRAGMENT_TOTAL_SIZE_BYTE = {
		64l * SwFile.SIZE_MB,
		128l * SwFile.SIZE_MB,
		256l * SwFile.SIZE_MB,
		512l * SwFile.SIZE_MB,
		1024l * SwFile.SIZE_MB,
		2048l * SwFile.SIZE_MB,
		4096l * SwFile.SIZE_MB
	};

	public static final String[] FRAGMENT_PER_SIZE = { "128KB", "256 KB",
			"512 KB", "1 MB", "2 MB", "4 MB", "8 MB", "16 MB" };

	public static final int[] FRAGMENT_PER_SIZE_BYTE = { 
		128 * SwFile.SIZE_KB,
		256 * SwFile.SIZE_KB, 
		512 * SwFile.SIZE_KB, 
		1 * SwFile.SIZE_MB,
		2 * SwFile.SIZE_MB, 
		4 * SwFile.SIZE_MB, 
		8 * SwFile.SIZE_MB,
		16 * SwFile.SIZE_MB, };

	// normal size
	public static final String NORMAL_SIZE[] = { 
		"64 MB", 
		"128 MB", 
		"256 MB", 
		"512 MB",
		"1024 MB", 
		"2048 MB", 
		"4096 MB", };
	
	public static final long NORMAL_SIZE_BYTE[] = { 
		64l * SwFile.SIZE_MB, 
		128l * SwFile.SIZE_MB,
		256l * SwFile.SIZE_MB, 
		512l * SwFile.SIZE_MB, 
		1024l * SwFile.SIZE_MB,
		2048l * SwFile.SIZE_MB, 
		4096l * SwFile.SIZE_MB };
	
	public static final String NORMAL_TIMES[] = { "1", "2", "3", "4", "5", "6", "7", "8", "10", };
	public static final int MAX_TEST_ROUND = 500;
	
	//View
	private Spinner mFrom;
	//private TextView mFromError;
	private Spinner mTo;
	//private TextView mToError;
	private RadioGroup mRadioGroup;
	private RadioButton mRadioNormal;
	private RadioButton mRadioFragment;
	private Spinner mFragmentTotal;
	private Spinner mFragmentSize;
	private Spinner mNormalSize;
	private Spinner mNormalTime;
	private Button mStart;
	private ProgressBar mProgressCreate;
	private TextView mTextCreate;
	private ProgressBar mProgressCopy;
	private TextView mTextCopy;
	private TextView mResult;
	private View mFragmentArea;
	private View mNormalArea;
	private CheckBox mCBCopyAll;
	private EditText mTestRound;
	
	private String ResultFormat;
	
	private static final int COPY_MODE_NORMAL = 0;
	private static final int COPY_MODE_FRAGMENT = 1;
	private int COPY_MODE = COPY_MODE_NORMAL; 
	
	private ArrayList<Object> mFromVolume;
	private ArrayList<Object> mToVolume;
	
	private int mTotalTime;
	private int mGrobleTime;
	
	private int mTotalTimeCp;
	private int mGrobleTimeCp;
	
	private String mRecentFromDir;
	private String mRecentToDir;
	
	private boolean mCopyAll = false;
	
	private ArrayList<CopyResult> mCpResultList = new ArrayList<CopyResult>();
	private class CopyResult{
		String from;
		String to;
		long fileSize;
		int  fileCnt;
		long costTime;
	}
	
	private Object lock = new Object();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.copy_tester);
		initView();
		ResultFormat = getString(R.string.test_copy_result_format);
		SwStorageManager.getInstance(this).registerListener(this);
	}
	
	private void initView() {
		mFrom = (Spinner) findViewById(R.id.spinner_from);
		//mFromError = (TextView) mRoot.findViewById(R.id.from_error);
		mTo = (Spinner) findViewById(R.id.spinner_to);
		//mToError = (TextView) mRoot.findViewById(R.id.to_error);
		mRadioGroup = (RadioGroup)findViewById(R.id.copy_mode_select);
		mRadioNormal = (RadioButton) findViewById(R.id.copy_normal);
		mRadioFragment = (RadioButton) findViewById(R.id.copy_fragment);
		mFragmentTotal = (Spinner) findViewById(R.id.spinner_fragment_total);
		mFragmentSize = (Spinner) findViewById(R.id.spinner_fragment_size);
		mNormalSize = (Spinner) findViewById(R.id.spinner_normal_size);
		mNormalTime = (Spinner) findViewById(R.id.spinner_normal_times);
		mStart = (Button) findViewById(R.id.start);
		mProgressCreate = (ProgressBar) findViewById(R.id.write_progress);
		mProgressCopy = (ProgressBar) findViewById(R.id.read_progress);
		mTextCreate = (TextView) findViewById(R.id.write_text);
		mTextCopy = (TextView) findViewById(R.id.read_text);
		mResult = (TextView) findViewById(R.id.result);
		mFragmentArea = findViewById(R.id.fragment_area);
		mNormalArea = findViewById(R.id.normal_area);
		mCBCopyAll = (CheckBox)findViewById(R.id.copy_all);
		mTestRound = (EditText)findViewById(R.id.copy_test_round_edit);
		
		mCBCopyAll.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean check) {
				boolean enableBtn = !check;
				mFrom.setEnabled(enableBtn);
				mTo.setEnabled(enableBtn);
				mCopyAll = check;
			}			
		});

		// -----------------------------------init spinner
		// init the fragment total spinner
		ArrayList<String> listSize = new ArrayList<String>();
		listSize.addAll(Arrays.asList(FRAGMENT_TOTAL_SIZE));
		ArrayAdapter<String> adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mFragmentTotal.setAdapter(adapterSize);
		mFragmentTotal.setSelection(3);

		// init the fragment size spinner
		listSize = new ArrayList<String>();
		listSize.addAll(Arrays.asList(FRAGMENT_PER_SIZE));
		adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mFragmentSize.setAdapter(adapterSize);
		mFragmentSize.setSelection(5);

		// init the normal size spinner
		listSize = new ArrayList<String>();
		listSize.addAll(Arrays.asList(NORMAL_SIZE));
		adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mNormalSize.setAdapter(adapterSize);
		mNormalSize.setSelection(0);

		// init the normal times spinner
		listSize = new ArrayList<String>();
		listSize.addAll(Arrays.asList(NORMAL_TIMES));
		adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mNormalTime.setAdapter(adapterSize);
		mNormalTime.setSelection(0);
		
		//init from & to spinner
		SwStorageManager swStorageManager = SwStorageManager.getInstance(this);
		mFromVolume = swStorageManager.getMountedVolume();
		mToVolume = swStorageManager.getMountedVolume();
		ArrayList<String> device = new ArrayList<String>();
		for(Object v : mFromVolume){
			device.add(Volume.getPath(v));
		}
		listSize = device;
		adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mFrom.setAdapter(adapterSize);
		mFrom.setSelection(0);
		mTo.setAdapter(adapterSize);
		mTo.setSelection(0);
		
		//init radio button
		mRadioGroup.setOnCheckedChangeListener(this);
		switchCopyMode(COPY_MODE);
		
		//init the start button
		mStart.setOnClickListener(this);
		
		//init the progress bar
		mProgressCreate.setMax(100);
		mProgressCopy.setMax(100);
	}
	
	private void switchCopyMode(int mode){
		switch(mode){
		case COPY_MODE_NORMAL:
			mRadioNormal.setChecked(true);
			mFragmentArea.setVisibility(View.GONE);
			mNormalArea.setVisibility(View.VISIBLE);
			break;
		case COPY_MODE_FRAGMENT:
			mRadioFragment.setChecked(true);
			mFragmentArea.setVisibility(View.VISIBLE);
			mNormalArea.setVisibility(View.GONE);
			break;
		}
		COPY_MODE = mode;
	}
	
	private void clearDir() {
		synchronized (lock) {
			File fromDir = new File(mRecentFromDir);
			File toDir = new File(mRecentToDir);
			SwFile.deleteDir(fromDir);
			SwFile.deleteDir(toDir);
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup arg0, int id) {
		if(id == mRadioNormal.getId()){
			switchCopyMode(COPY_MODE_NORMAL);
		}else if(id == mRadioFragment.getId()){
			switchCopyMode(COPY_MODE_FRAGMENT);
		}
	}

	@Override
	public void onClick(View view) {
		mCpResultList.clear();
		
		switch (COPY_MODE) {
		case COPY_MODE_NORMAL:
			final int sizePos = mNormalSize.getSelectedItemPosition();
			final long size = NORMAL_SIZE_BYTE[sizePos];
			final int timePos = mNormalTime.getSelectedItemPosition();
			final int times = timePos + 1;
			final int testRound = (Integer.parseInt(mTestRound.getText().toString()) > MAX_TEST_ROUND)?MAX_TEST_ROUND:Integer.parseInt(mTestRound.getText().toString());
			mStart.setEnabled(false);
			new Thread(new Runnable(){
				@Override
				public void run() {
					if(!mCopyAll){	
						int fromPos = mFrom.getSelectedItemPosition();
						String tmpFromPath = Volume.getPath(mFromVolume.get(fromPos));
						int toPos = mTo.getSelectedItemPosition();
						String tmpToPath = Volume.getPath(mToVolume.get(toPos));
						long totalCost = 0;
						for(int i = 0; i < testRound; i++) {
							copyNormal(tmpFromPath, tmpToPath,size,times);
							CopyResult last = mCpResultList.get(i);
							totalCost += last.costTime;
						}
						/* add the average result here */
						CopyResult cpResult = new CopyResult();
						cpResult.from = tmpFromPath;
						cpResult.to = tmpToPath;
						cpResult.fileSize = size;
						cpResult.fileCnt = times;
						cpResult.costTime = totalCost/testRound;
						mCpResultList.add(cpResult);
						final long c = cpResult.costTime;
						((Activity) CopyTesterActivity.this).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								String total = "" + (c / 1000.f / times) + " s/个";
								String per = "" + (c / 1000.f / times) + " s/个";
								String speed = ""
										+ SwFile.byteToSize((long) (size / (c / 1000.f / times)))
										+ "/s";
								String result = String.format(ResultFormat, "1",
										SwFile.byteToSize(size), total, per, speed);
								mResult.setText(result);
								mStart.setEnabled(true);
							}
						});
					}else{
						int fromSize = mFromVolume.size();
						int toSize = mToVolume.size();
		
						for(int i=0;i<fromSize;i++){
							for(int j=0;j<toSize;j++){
								String fromPath = Volume.getPath(mFromVolume.get(i));
								String toPath = Volume.getPath(mToVolume.get(j));
								long totalCost = 0;
								for(int k = 0; k < testRound; k++) {
									Log.v(TAG,"from path=" + fromPath + "|" + "to path=" + toPath);
									copyNormal(fromPath, toPath,size,times);
									CopyResult last = mCpResultList.get(i*toSize*testRound+j*testRound+k);
									totalCost += last.costTime;
								}
							
								/* add the average result here */
								CopyResult cpResult = new CopyResult();
								cpResult.from = fromPath;
								cpResult.to = toPath;
								cpResult.fileSize = size;
								cpResult.fileCnt = times;
								cpResult.costTime = totalCost/testRound;
								mCpResultList.add(cpResult);
							}
						}
						((Activity) CopyTesterActivity.this).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mResult.setText(CopyTesterActivity.this.getResources().getString(R.string.storage_test_go_to_test_result));
								mStart.setEnabled(true);
							}
						});
					}
					buildHTMLLog("Normal Copy test\n"+"Test File Size:"+mNormalSize.getSelectedItem().toString()+"\n"+
							"Test File Count:"+mNormalTime.getSelectedItem().toString()+"\n"+
							"Test times:"+testRound);
					clearDir();
				}
			}).start();	
			break;
		case COPY_MODE_FRAGMENT:
			final int totalPos = mFragmentTotal.getSelectedItemPosition();
			final long totalSize = FRAGMENT_TOTAL_SIZE_BYTE[totalPos];
			final int fragmentSizePos = mFragmentSize.getSelectedItemPosition();
			final int fragmentSize = FRAGMENT_PER_SIZE_BYTE[fragmentSizePos];
			final int fragmentTestRound = (Integer.parseInt(mTestRound.getText().toString()) > MAX_TEST_ROUND)?MAX_TEST_ROUND:Integer.parseInt(mTestRound.getText().toString());
			mStart.setEnabled(false);
			new Thread(new Runnable(){
				@Override
				public void run() {
					if(!mCopyAll){
						int fromPos = mFrom.getSelectedItemPosition();
						String tmpFromPath = Volume.getPath(mFromVolume.get(fromPos));
						int toPos = mTo.getSelectedItemPosition();
						String tmpToPath = Volume.getPath(mToVolume.get(toPos));
						long totalCost = 0;
						for(int i = 0; i < fragmentTestRound; i++) {
							copyFragment(tmpFromPath, tmpToPath,totalSize,fragmentSize);
							CopyResult last = mCpResultList.get(i);
							totalCost += last.costTime;
						}
						/* add the average result here */
						final int createTimes = (int) (totalSize / fragmentSize);
						CopyResult cpResult = new CopyResult();
						cpResult.from = tmpFromPath;
						cpResult.to = tmpToPath;
						cpResult.fileSize = fragmentSize;
						cpResult.fileCnt = createTimes;
						cpResult.costTime = totalCost/fragmentTestRound;
						mCpResultList.add(cpResult);
						
						final long c = cpResult.costTime;
						((Activity) CopyTesterActivity.this).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								String total = "" + (c / 1000.f) + " s/个";
								String per = "" + (c / 1000.f / createTimes) + " s/个";
								String speed = ""
										+ SwFile.byteToSize((long) (fragmentSize / (c / 1000.f / createTimes)))
										+ "/s";
								String result = String.format(ResultFormat, "" + createTimes,
										SwFile.byteToSize(fragmentSize), total, per, speed);
								mResult.setText(result);
								mStart.setEnabled(true);
							}
						});
					}else{
						int fromSize = mFromVolume.size();
						int toSize = mToVolume.size();
						Log.v(TAG,"fromSize=" + fromSize + "|" + "toSize=" + toSize);
						for(int i=0;i<fromSize;i++){
							for(int j=0;j<toSize;j++){
								long totalCost = 0;
								String fromPath = Volume.getPath(mFromVolume.get(i));
								String toPath = Volume.getPath(mToVolume.get(j));
								Log.v(TAG,"from path=" + fromPath + "|" + "to path=" + toPath);
								for(int k = 0; k < fragmentTestRound; k++) {
									copyFragment(fromPath, toPath,totalSize,fragmentSize);
									CopyResult last = mCpResultList.get(i*toSize*fragmentTestRound+j*fragmentTestRound+k);
									totalCost += last.costTime;
								}
								/* add the average result here */
								final int createTimes = (int) (totalSize / fragmentSize);
								CopyResult cpResult = new CopyResult();
								cpResult.from = fromPath;
								cpResult.to = toPath;
								cpResult.fileSize = fragmentSize;
								cpResult.fileCnt = createTimes;
								cpResult.costTime = totalCost/fragmentTestRound;
								mCpResultList.add(cpResult);
							}
						}
						((Activity) CopyTesterActivity.this).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mResult.setText(CopyTesterActivity.this.getResources().getString(R.string.storage_test_go_to_test_result));
								mStart.setEnabled(true);
							}
						});
					}
					buildHTMLLog("Fragment Copy test\n"+"Total Fragment Size:"+mFragmentTotal.getSelectedItem().toString()+"\n"+
							"Each Fragment Size:"+mFragmentSize.getSelectedItem().toString()+"\n"+
							"Test times:"+fragmentTestRound);
					clearDir();
				}
			}).start();	
			break;
		}
	}
	
	private void copyFragment(String from ,String to,long totalSize,final int fragmentSize){

		final String fromPath = from;
		final String toPath = to;

		mRecentFromDir = fromPath + "/" + FROM_DIR;
		mRecentToDir = toPath + "/" + TO_DIR;
		File fromDir = new File(mRecentFromDir);
		if (!fromDir.exists()) {
			fromDir.mkdirs();
		}
		File toDir = new File(mRecentToDir);
		if (!toDir.exists()) {
			toDir.mkdirs();
		}
		final int createTimes = (int) (totalSize / fragmentSize);
		String fromDirPath = fromDir.getPath();
		String toDirPath = toDir.getPath();
		((Activity) CopyTesterActivity.this).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String txt = getString(R.string.test_copy_progress_create_file);
				txt += "\n" + mRecentFromDir;
				mResult.setText(txt);
			}
		});

		SwFile.OnProgressListener createListener = new SwFile.OnProgressListener() {
			@Override
			public void onProgressChange(final long progress) {
				((Activity) CopyTesterActivity.this)
						.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mProgressCreate.setProgress((int) progress);
								String pstr = new StringBuilder()
										.append(mGrobleTime).append("/")
										.append(mTotalTime).append("  ")
										.append((progress * fragmentSize / 100))
										.append(" byte / ").append(fragmentSize)
										.append(" byte").toString();
								mTextCreate.setText(pstr);
							}
						});
			}
		};
		try {
			for (int i = 0; i < createTimes; i++) {
				File file = new File(fromDirPath + "/" + i);
				mGrobleTime = i + 1;
				mTotalTime = createTimes;
				if (!(file.exists() && file.length() == fragmentSize)) {
					SwFile.createZeroFile(file, fragmentSize, createListener, 100);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		((Activity) CopyTesterActivity.this).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String txt = CopyTesterActivity.this
						.getString(R.string.test_copy_progress_copy_file);
				txt += "\n" + mRecentFromDir + " to " + mRecentToDir;
				mResult.setText(txt);
			}
		});
		SwFile.OnProgressListener copyListener = new SwFile.OnProgressListener() {
			@Override
			public void onProgressChange(final long progress) {
				((Activity) CopyTesterActivity.this)
						.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mProgressCopy.setProgress((int) progress);
								String pstr = new StringBuilder()
										.append(mGrobleTimeCp).append("/")
										.append(mTotalTimeCp).append("  ")
										.append((progress * fragmentSize / 100))
										.append(" byte / ").append(fragmentSize)
										.append(" byte").toString();
								mTextCopy.setText(pstr);
							}
						});
			}
		};
		long t = 0;
		long cost = 0;
		Volume.cleanPageCache();
		try {
			t = System.currentTimeMillis();
			for (int i = 0; i < createTimes; i++) {
				File fromF = new File(fromDirPath + "/" + i);
				File toF = new File(toDirPath + "/" + i);
				mGrobleTimeCp = i + 1;
				mTotalTimeCp = createTimes;
				SwFile.copyFileTo(fromF, toF, 0, copyListener, 100);
			}
			cost = System.currentTimeMillis() - t;
		} catch (IOException e) {
			e.printStackTrace();
		}

		CopyResult cpResult = new CopyResult();
		cpResult.from = fromPath;
		cpResult.to = toPath;
		cpResult.fileSize = fragmentSize;
		cpResult.fileCnt = createTimes;
		cpResult.costTime = cost;
		mCpResultList.add(cpResult);
	}
	
	private void copyNormal(String from, String to,final long size,final int times){
		String tmpFromPath = from;
		String tmpToPath = to;
		
		final String fromPath = getMatchPartition(tmpFromPath);
		final String toPath = getMatchPartition(tmpToPath);
		

		mRecentFromDir = fromPath + "/" + FROM_DIR;
		mRecentToDir = toPath + "/" + TO_DIR;
		File fromDir = new File(mRecentFromDir);
		if (!fromDir.exists()) {
			fromDir.mkdirs();
		}
		File toDir = new File(mRecentToDir);
		if (!toDir.exists()) {
			toDir.mkdirs();
		}
		File fromF = new File(fromDir.getPath() + "/" + NORMAL_FILE);
		File toF = new File(toDir.getPath() + "/" + NORMAL_FILE);
		SwFile.OnProgressListener createListener = new SwFile.OnProgressListener() {
			@Override
			public void onProgressChange(final long progress) {
				((Activity) CopyTesterActivity.this)
						.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mProgressCreate.setProgress((int) progress);
								String pstr = new StringBuilder()
										.append((progress * size / 100))
										.append(" byte / ").append(size)
										.append(" byte").toString();
								mTextCreate.setText(pstr);
							}
						});
			}
		};

		SwFile.OnProgressListener copyListener = new SwFile.OnProgressListener() {
			@Override
			public void onProgressChange(final long progress) {
				((Activity) CopyTesterActivity.this)
						.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mProgressCopy.setProgress((int) progress);
								String pstr = new StringBuilder()
										.append(mGrobleTimeCp).append("/")
										.append(mTotalTimeCp).append("  ")
										.append((progress * size / 100))
										.append(" byte / ").append(size)
										.append(" byte").toString();
								mTextCopy.setText(pstr);
							}
						});
			}
		};
		long cost = 0;
		try {

			((Activity) CopyTesterActivity.this).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String txt = CopyTesterActivity.this
							.getString(R.string.test_copy_progress_create_file);
					txt += "\n" + mRecentFromDir;
					mResult.setText(txt);
				}
			});
			Log.e("dfdfdfsfsadf","----exits "+fromF.exists()+" from length:"+fromF.length()+" size:"+size);
			if (!(fromF.exists() && fromF.length() == size)) {
				SwFile.createZeroFile(fromF, size, createListener, 100);
			}
			
			Volume.cleanPageCache();
			((Activity) CopyTesterActivity.this).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String txt = CopyTesterActivity.this
							.getString(R.string.test_copy_progress_copy_file);
					txt += "\n" + mRecentFromDir + " to " + mRecentToDir;
					mResult.setText(txt);
				}
			});

			for (int i = 0; i < times; i++) {
				mGrobleTimeCp = i + 1;
				mTotalTimeCp = times;
				cost += SwFile.copyFileTo(fromF, toF, 0, copyListener, 100);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		CopyResult cpResult = new CopyResult();
		cpResult.from = fromPath;
		cpResult.to = toPath;
		cpResult.fileSize = size;
		cpResult.fileCnt = times;
		cpResult.costTime = cost;
		mCpResultList.add(cpResult);
	}
	
	private String getMatchPartition(String dir){
		String ret = dir;
		ArrayList<String> partition = SwStorageManager.getMountedPartitionList(dir);
		if(partition != null){
			ret = dir + "/" + partition.get(0);
		}
		return ret;
	}
	
	@Override
	public void onVolumeAdd(Object volume) {
		SwStorageManager swStorageManager = SwStorageManager.getInstance(this);
		mFromVolume = swStorageManager.getMountedVolume();
		mToVolume = swStorageManager.getMountedVolume();
		ArrayList<String> device = new ArrayList<String>();
		for(Object v : mFromVolume){
			device.add(Volume.getPath(v));
		}
		ArrayList<String> listSize = device;
		ArrayAdapter<String> adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mFrom.setAdapter(adapterSize);
		mFrom.setSelection(0);
		mTo.setAdapter(adapterSize);
		mTo.setSelection(0);
	}

	@Override
	public void onVolumeDel(Object volume) {
		SwStorageManager swStorageManager = SwStorageManager.getInstance(this);
		mFromVolume = swStorageManager.getMountedVolume();
		mToVolume = swStorageManager.getMountedVolume();
		ArrayList<String> device = new ArrayList<String>();
		for(Object v : mFromVolume){
			device.add(Volume.getPath(v));
		}
		ArrayList<String> listSize = device;
		ArrayAdapter<String> adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mFrom.setAdapter(adapterSize);
		mFrom.setSelection(0);
		mTo.setAdapter(adapterSize);
		mTo.setSelection(0);
	}

	private void buildHTMLLog(String testInfo){
		int testRound = (Integer.parseInt(mTestRound.getText().toString()) > MAX_TEST_ROUND)?MAX_TEST_ROUND:Integer.parseInt(mTestRound.getText().toString());
		
		try {
			FileWriter result = null;

			result = new FileWriter(new File(LogUtil.PATH
					+ StorageTestActivity.LOG_FILE_NAME), false);

			/* build the HTML header */
			result.append("<html>");
			String array[] = testInfo.split("\n");
			for(String item:array) {
				result.append("<H3>"+item+"</H3>");
			}
			result.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"0\" bordercolor=\"#000000\" bordercolordark=\"#FFFFFF\" bgcolor=\"#FFFFFF\">\n<tr>\n<th>Test Round</th>\n<th>from path</th>\n<th>to path</th>\n<th>copy speed(MB/s)</th>\n</tr>\n");
			/* write round speed data */
			int i = 0;
			String label;
			for(CopyResult eachResult:mCpResultList) {
				if(i == testRound) {
					i = 0;
					label = "Average";
				} else {
					label = "#"+(i+1);
				}
				result.append("<tr><td>"+label+"</td>\n<td>"+eachResult.from+"</td>\n<td>"+eachResult.to+"</td>\n<td>"+SwFile.byteToSize((long)(eachResult.fileSize/(eachResult.costTime/1000.f/eachResult.fileCnt)))+"/s"+"</td></tr>\n");
				i++;
			}
			/* write HTML tail */
			result.append("</table>\n");
			result.append("</html>");

			result.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
