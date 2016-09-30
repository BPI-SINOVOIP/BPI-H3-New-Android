package com.clock.pt1.keeptesting;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "SimpleDateFormat", "SdCardPath" })
public class DDRTestActivity extends Activity {

	TextView outputWindow;
	StringBuilder sb = new StringBuilder();

	private boolean isShow;
	private int MAX_INFO_LINE = 500;
	private int currentLine = 0;

	private final static String TAG = "DDRTestor";
	
	private final static int HANDLER_UPDATE_OUTPUT = 0;
	private final static int HANDLER_UPDATE_CMD_INFO = 1;
	
	private final static String CMD_PATH = "/data/data/com.clock.pt1.keeptesting/cache/memtest";
	
	EditText memSizeEdit;
	EditText repeatEdit;
	Button startBtn;
	Button stopBtn;
	TextView testInfoText;
	
	private int memSize;
	private int repeat = 1;
	private boolean flag;
	String cmd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		setRequestedOrientation(ScreenUtil.getScreenOrientation(this));
		setContentView(R.layout.ddr_test_layout);
		//CMD_PATH = this.getCacheDir().getPath() + "/memtest";
		
		outputWindow = (TextView) findViewById(R.id.output_window);
		memSizeEdit = (EditText)findViewById(R.id.mem_size_edit);
		repeatEdit = (EditText)findViewById(R.id.repeat_edit);
		
		startBtn = (Button)findViewById(R.id.ddr_start_button);
		stopBtn = (Button)findViewById(R.id.ddr_stop_button);
		
		testInfoText = (TextView)findViewById(R.id.testing_info);
		
		/* check if the device is rooted */
		if(!ShellUtil.isDeviceRooted()) {
			Dialog alertDialog = new AlertDialog.Builder(this).setTitle(R.string.attention)
					.setMessage(R.string.device_not_rooted)
					.setPositiveButton(this.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							DDRTestActivity.this.finish();
						}
					}).create();
			alertDialog.show();
		}
		
		startBtn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				memSize = Integer.valueOf(memSizeEdit.getEditableText().toString());
				if(memSize <= 0) {
					Toast.makeText(DDRTestActivity.this, getResources().getString(R.string.ddr_test_mem_size_error_text), Toast.LENGTH_SHORT).show();
					return;
				}

				repeat = Integer.valueOf(repeatEdit.getEditableText().toString());
				if(repeat <= 0) {
					Toast.makeText(DDRTestActivity.this, getResources().getString(R.string.ddr_test_repeat_count_error_text), Toast.LENGTH_SHORT).show();
					return;
				}
				
				new Thread(new TestRunnable()).start();
				startBtn.setEnabled(false);
			}
		});
		stopBtn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if(flag) {
					Toast.makeText(DDRTestActivity.this, getResources().getString(R.string.ddr_test_stopped_text), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(DDRTestActivity.this, getResources().getString(R.string.ddr_test_no_test_running_text), Toast.LENGTH_SHORT).show();
				}
				flag = false;
				
				startBtn.setEnabled(true);
			}
		});
		
		refeshOutputWindow();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		isShow = false;
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		isShow = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void refeshOutputWindow() {
		outputWindow.setText(sb.toString());
		if (isShow) {
			mHandler.sendEmptyMessageDelayed(HANDLER_UPDATE_OUTPUT, 1000);
		}
		
		/*if(flag) {
			testInfoText.setText(getResources().getString(R.string.ddr_test_testing_text) + cmd);
			
		} else {
			testInfoText.setText(R.string.ddr_test_no_test_running_text);
		}*/
	}
	
	public void initCMDFile() {
		File cmdFile = new File(CMD_PATH);
		
		if(!cmdFile.exists()) {
			Log.d(TAG, " create memtest file on cache.");
			//copy memtest from assets of app to cache directory.
			InputStream is = null;
			FileOutputStream fos = null;
			try {
				is = getAssets().open("memtest");
				fos = new FileOutputStream(cmdFile);
				
				byte[] buff = new byte[2048];
				int length = 0;
				while((length = is.read(buff)) != -1) {
					fos.write(buff, 0, length);
				}
				fos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(is != null) {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if(fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			//change memtest in cache permission
			
			DataOutputStream dos = null;

			try {	
				Runtime.getRuntime().exec("chmod 777 " + CMD_PATH);
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				if(dos != null) {
					try {
						dos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case HANDLER_UPDATE_OUTPUT:
					refeshOutputWindow();
					break;
				case HANDLER_UPDATE_CMD_INFO:
					Object infoObj = msg.obj;
					if(infoObj != null) {
						testInfoText.setText(getResources().getString(R.string.ddr_test_testing_text) + infoObj.toString());
					} else {
						if(msg.arg1 == 0) {
							testInfoText.setText(R.string.ddr_test_fail_text);
						} else if(msg.arg1 == 1) {
							testInfoText.setText(R.string.ddr_test_success_text);
						} else {
							testInfoText.setText(R.string.ddr_test_no_test_running_text);
						}
						startBtn.setEnabled(true);
					}
					break;
			}
		}
	};
	
	class TestRunnable implements Runnable {
		
		@Override
		public void run() {
			boolean result = false;
			if(memSize <= 0) {
				return;
			}
			initCMDFile();
			flag = true;
			
			sb.setLength(0);
			mHandler.sendEmptyMessage(HANDLER_UPDATE_OUTPUT);
			DataOutputStream dos = null;
			BufferedReader dis = null;
			BufferedReader errReader = null;
			FileOutputStream logOutput = null;
			ErrorRunnable mErrorRunnable = null;

			Process p = null;

			try {
				p = Runtime.getRuntime().exec("/system/xbin/su");
				cmd = String.format("%s %dm %d\n",CMD_PATH, memSize, repeat);
				
				dos = new DataOutputStream(p.getOutputStream());
				dis = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
				String str = format.format(new Date());
				logOutput = new FileOutputStream(LogUtil.PATH+"DDRTest-"+str, true);
					
				Log.i(TAG, "ddr test start........");

				Message msg = new Message();
				msg.what = HANDLER_UPDATE_CMD_INFO;
				msg.obj = cmd;
				mHandler.sendMessage(msg);
				
				//sb.append(dis.readLine());

				dos.writeBytes(cmd);
				dos.flush();
				sb.append("start memtest\n");
				String line = null;
				
				//Record start time of testing to log file.
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				String timeStr = sdf.format(new Date(System.currentTimeMillis()));
				String spliteLine ="------------------------------------------------------------------\n";
				String testInfo = timeStr + "    " + cmd + "\n";
				logOutput.write(spliteLine.getBytes());
				logOutput.write(testInfo.getBytes());
				
				mErrorRunnable = new ErrorRunnable(errReader, logOutput);
				new Thread(mErrorRunnable).start();
				
				while ((line = dis.readLine()) != null && flag) {
					if (currentLine >= MAX_INFO_LINE) {
						currentLine = 0;
						sb.setLength(0);
					}
					
					if (line.length() > 15
							&& "Stuck Address".equals(line.substring(2, 15))) {
						if (line.endsWith("ok")) {
							sb.append("  Stuck Address		: ok");
						} else {
							sb.append("  Stuck Address		: failed");
						}
					} else if (line.length() > 14 && "Random Value".equals(line.substring(2,14))){
						if (line.endsWith("ok")) {
							sb.append("  Random Value		: ok");
						} else {
							sb.append("  Random Value		: failed");
						}
					} else if(line.length() > 14 && "Checkerboard".equals(line.substring(2, 14))) {
						if (line.endsWith("ok")) {
							sb.append("  Checkerboard		: ok");
						} else {
							sb.append("  Checkerboard		: failed");
						}
					}else if (line.length() > 12
							&& "Solid Bits".equals(line.substring(2, 12))) {
						if (line.endsWith("ok")) {
							sb.append("  Solid Bits		: ok");
						} else {
							sb.append("  Solid Bits		: failed");
						}
					} else if (line.length() > 18
							&& "Block Sequential".equals(line.substring(2, 18))) {
						if (line.endsWith("ok")) {
							sb.append("  Block Sequential		: ok");
						} else {
							sb.append("  Block Sequential		: failed");
						}

					} else if(line.length() > 12 && "Bit Spread".equals(line.substring(2, 12))) {
						if (line.endsWith("ok")) {
							sb.append("  Bit Spread		: ok");
						} else {
							sb.append("  Bit Spread		: failed");
						}
					
					}else if (line.length() > 10 && "Bit Flip".equals(line.substring(2, 10))){
						if (line.endsWith("ok")) {
							sb.append("  Bit Flip		: ok");
						} else {
							sb.append("  Bit Flip		: failed");
						}
						
					}else if (line.length() > 14
							&& "Walking Ones".equals(line.substring(2, 14))) {
						if (line.endsWith("ok")) {
							sb.append("  Walking Ones		: ok");
						} else {
							sb.append("  Walking Ones		: failed");
						}
					} else if (line.length() > 16
							&& "Walking Zeroes".equals(line.substring(2, 16))) {
						if (line.endsWith("ok")) {
							sb.append("  Walking Zeroes		: ok");
						} else {
							sb.append("  Walking Zeroes		: failed");
						}
					} else {

						sb.append(line);
					}

					sb.append("\n");
					if (line.startsWith("Done")) {
						result = true;
						break;
					}
					
					//Output to log file;
					logOutput.write(line.getBytes());
					String end = "\n";
					logOutput.write(end.getBytes());
					
					currentLine++;
					
				}

				mErrorRunnable.stop();
				mErrorRunnable = null;
				//errReader.close();
				//errReader = null;
				logOutput.flush();
				Log.i(TAG, "ddr test end.");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				Process pPS = null;
				Process pKiller = null;
				
				DataOutputStream dosKiller = null;
				DataOutputStream dosPS = null;
				BufferedReader disPS = null;
				try {
					pPS = Runtime.getRuntime().exec("/system/xbin/su");

					dosPS = new DataOutputStream(pPS.getOutputStream());
					disPS = new BufferedReader(new InputStreamReader(
							pPS.getInputStream()));
					dosPS.writeBytes("ps\n");
					dosPS.writeBytes("exit\n");
					dosPS.flush();

					Pattern pattern = Pattern.compile("\\S*\\s*(\\d+)");
					Matcher matcher = null;
					String line;

					pKiller = Runtime.getRuntime().exec("/system/xbin/su");
					dosKiller = new DataOutputStream(pKiller.getOutputStream());
					
					while((line = disPS.readLine()) != null) {
						if (line.contains("memtest")) {
							matcher = pattern.matcher(line);
							if (matcher.find())
						    { 
								Log.i(TAG, "killing task: "+matcher.group(1));
								dosKiller.writeBytes("kill " + matcher.group(1)
										+ "\n");
								dosKiller.flush();
								break;
						    }
						}
					}

					dosKiller.writeBytes("exit\n");
					dosKiller.flush();
					
					dos.writeBytes("exit\n");
					dos.flush();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				if (dos != null) {
					try {
						dos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (dis != null) {
					try {
						dis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (p != null) {
					p.destroy();

				}
				if(logOutput != null) {
					try {
						logOutput.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if(mErrorRunnable != null) {
					mErrorRunnable.stop();
					mErrorRunnable = null;
				}

				Message msg = new Message();
				msg.what = HANDLER_UPDATE_CMD_INFO;
				if(flag) {
					if(result) {
						msg.arg1 = 1;
					} else {
						msg.arg1 = 0;
					}
				} else {
					msg.arg1 = 2;
				}
				msg.obj = null;
				mHandler.sendMessage(msg);

				flag = false;
				memSize = 0;
				repeat = 1;
				cmd = null;
			}
		}

	};
	
	class ErrorRunnable implements Runnable {
		
		private boolean isStop;
		
		private BufferedReader mReader;
		private FileOutputStream mOutput;
		
		ErrorRunnable(BufferedReader br, FileOutputStream fos) {
			mReader = br;
		}
		
		public void stop() {
			isStop = true;
		}

		@Override
		public void run() {
			String str = null;
			Log.d("ErrorRunnable", "start catch error info");
			try {
				while(( str = mReader.readLine()) != null && !isStop) {
					sb.append(str);
					sb.append("\n");
					mOutput.write(str.getBytes());
					sb.append("\n");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	};
}
