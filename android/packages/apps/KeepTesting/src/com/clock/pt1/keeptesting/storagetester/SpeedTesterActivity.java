package com.clock.pt1.keeptesting.storagetester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.clock.pt1.keeptesting.LogUtil;
import com.clock.pt1.keeptesting.R;
import com.clock.pt1.keeptesting.storagetester.SwStorageManager.OnVolumeChangeListener;

public class SpeedTesterActivity extends Activity implements View.OnClickListener, OnVolumeChangeListener{
	
	public static final String TAG = "SpeedTester";
	
	public static final String DEFAULT_TEST_PATH = "/mnt/sata";
	public static final String SPEED_TEST_DIR = "speed_test_dir";
	public static final String TEST_FILE_NAME = "speed";
	public static final int MB_SIZE = 1024 * 1024;

	public static final int PROGRESS_MAX = 100;
	public static final int MAX_TEST_ROUND = 500;

	// View
	private Spinner mPath;
	private Spinner mSpinnerSize;
	private Spinner mSpinnerTimes;
	private ProgressBar mProgressWrite;
	private TextView mTextWrite;
	private ProgressBar mProgressRead;
	private TextView mTextRead;
	private Button mStart;
	private TextView mResult;
	private Button mOnlyRead;
	private Button mOnlyWrite;
	private Button mClearDir;
	private EditText mTestRound;

	private String mResultFormat;

	// Data
	private String mSize[] = { /*"64 KB", "256 KB", "1 MB", "4MB",*/ "16 MB",
			"64 MB", "256 MB", "1024 MB" };
	private Integer mISize[] = {
			/*64  * SwFile.SIZE_KB,
			256 * SwFile.SIZE_KB,
			1   * SwFile.SIZE_MB,
			4   * SwFile.SIZE_MB,*/
			16  * SwFile.SIZE_MB,
			64  * SwFile.SIZE_MB,
			256 * SwFile.SIZE_MB,
			1   * SwFile.SIZE_GB};

	private String mTimes[] = { "1","4","16","64","256","1024"};

	private long mGrobleWriteTime;
	private long mGrobleReadTime;
	private double mRoundWriteSpeed[];
	private double mRoundReadSpeed[];
	
	private int mTotalTimes;
	private int mGrobleTimes;
	
	private ArrayList<Object> mVolumes;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speed_tester);
		initView();
		
		SwStorageManager.getInstance(this).registerListener(this);
		mRoundWriteSpeed = new double[MAX_TEST_ROUND+1];
		mRoundReadSpeed = new double[MAX_TEST_ROUND+1];
		
		mResultFormat = this.getResources().getString(R.string.result_format_str);

		// init the size spinner
		ArrayList<String> listSize = new ArrayList<String>();
		listSize.addAll(Arrays.asList(mSize));
		ArrayAdapter<String> adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerSize.setAdapter(adapterSize);
		mSpinnerSize.setSelection(0);

		// init the times spinner
		ArrayList<String> listTimes = new ArrayList<String>();
		listTimes.addAll(Arrays.asList(mTimes));
		ArrayAdapter<String> adapterTimes = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listTimes);
		adapterTimes
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerTimes.setAdapter(adapterTimes);
		mSpinnerTimes.setSelection(2);
		
		// init the times spinner
		SwStorageManager swStorageManager = SwStorageManager
				.getInstance(this);
		mVolumes = swStorageManager.getMountedVolume();
		ArrayList<String> device = new ArrayList<String>();
		for (Object v : mVolumes) {
			device.add(Volume.getPath(v));
		}
		listSize = device;
		adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPath.setAdapter(adapterSize);
		mPath.setSelection(0);
	}
	
	private void initView() {
		mPath = (Spinner) findViewById(R.id.edit_path);
		mSpinnerSize = (Spinner) findViewById(R.id.spinner_size);
		mSpinnerTimes = (Spinner) findViewById(R.id.spinner_times);
		mProgressWrite = (ProgressBar) findViewById(R.id.write_progress);
		mProgressRead = (ProgressBar) findViewById(R.id.read_progress);
		mTextWrite = (TextView) findViewById(R.id.write_text);
		mTextRead = (TextView) findViewById(R.id.read_text);
		mResult = (TextView) findViewById(R.id.result);
		mStart = (Button) findViewById(R.id.start);
		mOnlyRead = (Button)findViewById(R.id.only_read);
		mOnlyWrite = (Button)findViewById(R.id.only_write);
		mClearDir = (Button)findViewById(R.id.clear_dir);
		mTestRound = (EditText)findViewById(R.id.test_round_edit);
				
		mProgressWrite.setMax(100);
		mProgressRead.setMax(100);
		
		mStart.setOnClickListener(this);
		mOnlyRead.setOnClickListener(this);
		mOnlyWrite.setOnClickListener(this);
		mClearDir.setOnClickListener(this);
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
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.start:
			writeRead();
			break;
		case R.id.only_read:
			readOnly();
			break;
		case R.id.only_write:
			writeOnly();
			break;
		case R.id.clear_dir:
			clearDir();
			break;
		}
	}
	
	private void writeRead(){
		int sizePos = mSpinnerSize.getSelectedItemPosition();
		final int size = mISize[sizePos];
		int timesPos = mSpinnerTimes.getSelectedItemPosition();
		final int times = (int) Math.pow(4, timesPos);
		final int pathPos = mPath.getSelectedItemPosition();
		final int testRound = (Integer.parseInt(mTestRound.getText().toString()) > MAX_TEST_ROUND)?MAX_TEST_ROUND:Integer.parseInt(mTestRound.getText().toString());
		String dir = Volume.getPath(mVolumes.get(pathPos));

		dir = getMatchPartition(dir);
		dir = dir + "/" + SPEED_TEST_DIR;
		File dirf = new File(dir);
		if(!dirf.exists()){
			dirf.mkdirs();
		}
		final String filePath = dir + "/" + TEST_FILE_NAME;
		mGrobleTimes = 0;
		mGrobleWriteTime = 0;
		mGrobleReadTime = 0;
		new Thread(new Runnable() {
			@Override
			public void run() {
				mTotalTimes = times;
				String path = filePath;
				long wTime = 0;
				long rTime = 0;
				long totalWTime = 0;
				long totalRTime = 0;
				for(int j = 0; j < testRound; j++) {
					wTime = 0;
					long t = System.currentTimeMillis();
					for (int i = 0; i < times; i++) {
						mGrobleTimes = i + 1;
						path = filePath + i;
						wTime += writeSpeed(path, size);
					}
					long cost = System.currentTimeMillis() - t;
					wTime = cost;
					totalWTime += wTime;
					mRoundWriteSpeed[j] = ((double)(size*1000.0f*times)/ (1024*1024*wTime));
				}
				mGrobleWriteTime = totalWTime;

				for(int j = 0; j < testRound; j++) {
					rTime = 0;
					Volume.cleanPageCache();
					long t = System.currentTimeMillis();
					for (int i = 0; i < times; i++) {
						mGrobleTimes = i + 1;
						path = filePath + i;
						rTime += readSpeed(path);
					}
					long cost = System.currentTimeMillis() - t;
					rTime = cost;
					totalRTime += rTime;
					mRoundReadSpeed[j] = ((double)(size*1000.0f*times) / (1024*1024*rTime));
				}
				mGrobleReadTime = totalRTime;
				
				long wirtePerSec = mGrobleWriteTime == 0 ? 0 :(long) ((size*times*testRound*1000.0f) / mGrobleWriteTime);
				long readPerSec = mGrobleReadTime == 0 ? 0 : (long) ((size*times*testRound*1000.0f) / mGrobleReadTime);
				mRoundWriteSpeed[testRound] = (double)wirtePerSec/(1024*1024);
				mRoundReadSpeed[testRound]  = (double)readPerSec/(1024*1024);
				
				((Activity)SpeedTesterActivity.this).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						
						long wirtePerSec = mGrobleWriteTime == 0 ? 0 :(long) ((size*times*testRound*1000.0f) / mGrobleWriteTime);
						long readPerSec = mGrobleReadTime == 0 ? 0 : (long) ((size*times*testRound*1000.0f) / mGrobleReadTime);

						String wps = SwFile.byteToSize(wirtePerSec) + "/s";
						String rps = SwFile.byteToSize(readPerSec) + "/s";
						String result = String.format(mResultFormat, mStart.getText(),wps, rps);
						mResult.setText(result);
						Log.v(TAG, "wirte per sec:" + wps);
						Log.v(TAG,"read per sec:" + SwFile.byteToSize(readPerSec));
						mStart.setEnabled(true);
						mOnlyWrite.setEnabled(true);
						mOnlyRead.setEnabled(true);
						mClearDir.setEnabled(true);
					}
				});

				buildHTMLLog("Read/Write test\n"+"Test File Size:"+mSpinnerSize.getSelectedItem().toString()+"\n"+
							"Test File Count:"+mSpinnerTimes.getSelectedItem().toString()+"\n"+
							"Test Path:"+mPath.getSelectedItem().toString()+"\n"+
							"Test times:"+testRound);
			}
		}).start();
		mStart.setEnabled(false);
		mOnlyWrite.setEnabled(false);
		mOnlyRead.setEnabled(false);
		mClearDir.setEnabled(false);
	}
	
	private void writeOnly(){
		int sizePos = mSpinnerSize.getSelectedItemPosition();
		final int size = mISize[sizePos];
		int timesPos = mSpinnerTimes.getSelectedItemPosition();
		final int times = (int) Math.pow(4, timesPos);
		final int pathPos = mPath.getSelectedItemPosition();
		final int testRound = (Integer.parseInt(mTestRound.getText().toString()) > MAX_TEST_ROUND)?MAX_TEST_ROUND:Integer.parseInt(mTestRound.getText().toString());
		
		String dir = Volume.getPath(mVolumes.get(pathPos));
		dir = getMatchPartition(dir);
		dir = dir + "/" + SPEED_TEST_DIR;
		final File dirf = new File(dir);
		final String filePath = dir + "/" + TEST_FILE_NAME;
		mGrobleTimes = 0;
		mGrobleWriteTime = 0;
		mGrobleReadTime = 0;
		new Thread(new Runnable() {
			@Override
			public void run() {
				((Activity) SpeedTesterActivity.this).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mResult.setText(R.string.storage_test_clean_folder_message);
					}
				});
				SwFile.deleteDir(dirf);
				if(!dirf.exists()){
					dirf.mkdirs();
				}
				
				long wTime = 0;
				long totalWTime = 0;
				mTotalTimes = times;
				String path = filePath;	
				for(int j = 0; j < testRound; j++) {
					wTime = 0;
					long t = System.currentTimeMillis();
					for (int i = 0; i < times; i++) {
						mGrobleTimes = i + 1;
						path = filePath + i;
						wTime += writeSpeed(path, size);
					}
					long cost = System.currentTimeMillis() - t;
					wTime = cost;
					totalWTime += wTime;
					mRoundWriteSpeed[j] = ((double)(size*1000.0f*times)/ (1024*1024*wTime));
					mRoundReadSpeed[j]  = 0;
				}
				mGrobleWriteTime = totalWTime;
				
				long wirtePerSec = mGrobleWriteTime == 0 ? 0 :(long) ((size*times*testRound*1000.0f) / mGrobleWriteTime);
				long readPerSec = mGrobleReadTime == 0 ? 0 : (long) ((size*times*testRound*1000.0f) / mGrobleReadTime);
				mRoundWriteSpeed[testRound] = (double)wirtePerSec/(1024*1024);
				mRoundReadSpeed[testRound]  = (double)readPerSec/(1024*1024);
				
				((Activity)SpeedTesterActivity.this).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						long wirtePerSec = mGrobleWriteTime == 0 ? 0 :(long) ((size*times*testRound*1000.0f) / mGrobleWriteTime);
						long readPerSec = mGrobleReadTime == 0 ? 0 : (long) ((size*times*testRound*1000.0f) / mGrobleReadTime);

						String wps = SwFile.byteToSize(wirtePerSec) + "/s";
						String rps = SwFile.byteToSize(readPerSec) + "/s";
						String result = String.format(mResultFormat, mOnlyWrite.getText(),wps, rps);
						mResult.setText(result);
						Log.v(TAG, "wirte per sec:" + wps);
						Log.v(TAG,"read per sec:" + SwFile.byteToSize(readPerSec));
						mStart.setEnabled(true);
						mOnlyWrite.setEnabled(true);
						mOnlyRead.setEnabled(true);
						mClearDir.setEnabled(true);
					}
				});

				buildHTMLLog("Write Only test\n"+"Test File Size:"+mSpinnerSize.getSelectedItem().toString()+"\n"+
						"Test File Count:"+mSpinnerTimes.getSelectedItem().toString()+"\n"+
						"Test Path:"+mPath.getSelectedItem().toString()+"\n"+
						"Test times:"+testRound);
			}
		}).start();
		mStart.setEnabled(false);
		mOnlyWrite.setEnabled(false);
		mOnlyRead.setEnabled(false);
		mClearDir.setEnabled(false);
	}
	
	private void readOnly(){
		final int testRound = (Integer.parseInt(mTestRound.getText().toString()) > MAX_TEST_ROUND)?MAX_TEST_ROUND:Integer.parseInt(mTestRound.getText().toString());
		final int pathPos = mPath.getSelectedItemPosition();
		String dir = Volume.getPath(mVolumes.get(pathPos));
		dir = getMatchPartition(dir);
		dir = dir + "/" + SPEED_TEST_DIR;
		final File dirf = new File(dir);
		if(!dirf.exists()){
			dirf.mkdirs();
		}
		final String filePath = dir + "/" + TEST_FILE_NAME;
		mGrobleTimes = 0;
		mGrobleWriteTime = 0;
		mGrobleReadTime = 0;
		new Thread(new Runnable() {
			@Override
			public void run() {				
				String path = filePath;
				String filelist[] = dirf.list();
				final long size = filelist != null && filelist.length > 0 ?
						new File(dirf,filelist[0]).length() : 0;
				final int fileCnt = filelist == null ? 0 : filelist.length;
				mTotalTimes = fileCnt;

				((Activity) SpeedTesterActivity.this).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mResult.setText(R.string.storage_test_reading_file_message);
					}
				});
				

				long rTime = 0;
				long totalRTime = 0;
				for(int j = 0; j < testRound; j++) {
					rTime = 0;
					Volume.cleanPageCache();
					long t = System.currentTimeMillis();
					for (int i = 0; i < fileCnt; i++) {
						mGrobleTimes = i + 1;
						path = filePath + i;
						rTime += readSpeed(path);
					}
					long cost = System.currentTimeMillis() - t;
					rTime = cost;
					totalRTime += rTime;
					mRoundReadSpeed[j] = ((double)(size*1000.0f*fileCnt) / (1024*1024*rTime));
					mRoundWriteSpeed[j] = 0;
				}
				mGrobleReadTime = totalRTime;
				
				long wirtePerSec = mGrobleWriteTime == 0 ? 0 :(long) ((size*fileCnt*testRound*1000.0f) / mGrobleWriteTime);
				long readPerSec = mGrobleReadTime == 0 ? 0 : (long) ((size*fileCnt*testRound*1000.0f) / mGrobleReadTime);
				mRoundWriteSpeed[testRound] = (double)wirtePerSec/(1024*1024);
				mRoundReadSpeed[testRound]  = (double)readPerSec/(1024*1024);
				
				((Activity)SpeedTesterActivity.this).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						long wirtePerSec = mGrobleWriteTime == 0 ? 0 :(long) ((size*fileCnt*testRound*1000.0f) / mGrobleWriteTime);
						long readPerSec = mGrobleReadTime == 0 ? 0 : (long) ((size*fileCnt*testRound*1000.0f) / mGrobleReadTime);

						String wps = SwFile.byteToSize(wirtePerSec) + "/s";
						String rps = SwFile.byteToSize(readPerSec) + "/s";
						String result = String.format(mResultFormat, mOnlyRead.getText(),wps, rps);
						mResult.setText(result);
						Log.v(TAG, "wirte per sec:" + wps);
						Log.v(TAG, "read per sec:" + SwFile.byteToSize(readPerSec));
						mStart.setEnabled(true);
						mOnlyWrite.setEnabled(true);
						mOnlyRead.setEnabled(true);
						mClearDir.setEnabled(true);
					}
				});

				buildHTMLLog("Read Only test\n"+"Test File Size:"+mSpinnerSize.getSelectedItem().toString()+"\n"+
						"Test File Count:"+mSpinnerTimes.getSelectedItem().toString()+"\n"+
						"Test Path:"+mPath.getSelectedItem().toString()+"\n"+
						"Test times:"+testRound);
			}
		}).start();
		mStart.setEnabled(false);
		mOnlyWrite.setEnabled(false);
		mOnlyRead.setEnabled(false);
		mClearDir.setEnabled(false);
	}
	
	private void clearDir(){
		new Thread(new Runnable(){
			@Override
			public void run() {
				final int pathPos = mPath.getSelectedItemPosition();
				String dir = Volume.getPath(mVolumes.get(pathPos));
				dir = getMatchPartition(dir);
				String dirPath = dir + "/" + SPEED_TEST_DIR;
				final boolean del = SwFile.deleteDir(new File(dirPath));
				((Activity)SpeedTesterActivity.this).runOnUiThread(new Runnable(){
					@Override
					public void run() {
						Toast.makeText(SpeedTesterActivity.this, del ? SpeedTesterActivity.this.getResources().getString(R.string.storage_test_delete_success) : SpeedTesterActivity.this.getResources().getString(R.string.storage_test_delete_fail), Toast.LENGTH_SHORT).show();
					}});				
			}			
		}).start();		
	}

	private long writeSpeed(String path, final int size) {
		long result = 0;
		try {
			result = SwFile.writeSpeed(new File(path), size,
					new SwFile.OnProgressListener() {
						@Override
						public void onProgressChange(final long progress) {
							((Activity)SpeedTesterActivity.this).runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mProgressWrite.setProgress((int) progress);
									String pstr = new StringBuilder()
											.append(mGrobleTimes)
											.append("/")
											.append(mTotalTimes)
											.append("  ")
											.append((progress * size / 100))
											.append(" byte / ").append(size)
											.append(" byte").toString();
									mTextWrite.setText(pstr);
								}
							});
						}
					}, PROGRESS_MAX);
			mGrobleWriteTime += result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private long readSpeed(String path) {
		long result = 0;
		try {
			File file = new File(path);
			final long size = file.length();
			result = SwFile.readSpeed(file,
					new SwFile.OnProgressListener() {
						@Override
						public void onProgressChange(final long progress) {
							((Activity)SpeedTesterActivity.this).runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mProgressRead.setProgress((int) progress);
									String pstr = new StringBuilder()
											.append(mGrobleTimes)
											.append("/")
											.append(mTotalTimes)
											.append("  ")
											.append((progress * size / 100))
											.append(" byte / ").append(size)
											.append(" byte").toString();
									mTextRead.setText(pstr);
								}

							});
						}
					}, PROGRESS_MAX);
			mGrobleReadTime += result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public void onVolumeAdd(Object volume) {
		SwStorageManager swStorageManager = SwStorageManager
				.getInstance(this);
		mVolumes = swStorageManager.getMountedVolume();
		ArrayList<String> device = new ArrayList<String>();
		for (Object v : mVolumes) {
			device.add(Volume.getPath(v));
		}
		ArrayList<String> listSize = device;
		ArrayAdapter<String >adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPath.setAdapter(adapterSize);
		mPath.setSelection(0);
	}

	@Override
	public void onVolumeDel(Object volume) {
		SwStorageManager swStorageManager = SwStorageManager
				.getInstance(this);
		mVolumes = swStorageManager.getMountedVolume();
		ArrayList<String> device = new ArrayList<String>();
		for (Object v : mVolumes) {
			device.add(Volume.getPath(v));
		}
		ArrayList<String> listSize = device;
		ArrayAdapter<String >adapterSize = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, listSize);
		adapterSize
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPath.setAdapter(adapterSize);
		mPath.setSelection(0);
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
			result.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"0\" bordercolor=\"#000000\" bordercolordark=\"#FFFFFF\" bgcolor=\"#FFFFFF\">\n<tr>\n<th>Test Round</th>\n<th>write speed(MB/s)</th>\n<th>read speed(MB/s)</th>\n</tr>\n");
			/* write round speed data */
			for(int i = 0; i < testRound; i++) {
				result.append("<tr><td>#"+(i+1)+"</td>\n<td>"+String.format("%.2f", mRoundWriteSpeed[i])+"</td>\n<td>"+String.format("%.2f", mRoundReadSpeed[i])+"</td></tr>\n");
			}
			/* write average speed data */
			result.append("<tr><td>Average</td>\n<td>"+String.format("%.2f", mRoundWriteSpeed[testRound])+"</td>\n<td>"+String.format("%.2f", mRoundReadSpeed[testRound])+"</td></tr>\n");
			Log.v(TAG,"testRound:"+testRound);
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
