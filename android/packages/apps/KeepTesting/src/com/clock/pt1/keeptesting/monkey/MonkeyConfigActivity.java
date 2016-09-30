package com.clock.pt1.keeptesting.monkey;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.clock.pt1.keeptesting.R;
import com.stericson.RootTools.RootTools;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.DialogInterface;
import android.content.Intent;

@SuppressLint("SdCardPath")
public class MonkeyConfigActivity extends Activity {

	public static final String TAG = "MonkeyTest";
	public static final String BLACK_LIST = "/data/data/com.clock.pt1.keeptesting/cache/black.txt";
	public static final String WHITE_LIST = "/data/data/com.clock.pt1.keeptesting/cache/white.txt";
	public static final String DEFAULT_BLACK_LIST = "com.android.settings;com.clock.pt1.keeptesting;com.android.development";
	public static final int BLACK_LIST_PLUS = 0;
	public static final int BLACK_LIST_MINUS = 1;
	public static final int WHITE_LIST_PLUS = 2;
	public static final int WHITE_LIST_MINUS = 3;
	public static final int BLACK_MODE = 0;
	public static final int WHITE_MODE = 1;

	private CheckBox basicConfigCheckBox;
	private CheckBox fullConfigCheckBox;
	private CheckBox whiteListCheckBox;
	private CheckBox blackListCheckBox;
	private CheckBox ignoreCrashCheckBox;
	private EditText testDuration;
	private EditText operationDelay;
	private Button startButton;
	private Button resetButton;
	private Button whiteListPlusButton;
	private Button whiteListMinusButton;
	private Button blackListPlusButton;
	private Button blackListMinusButton;
	private Spinner debugLevelSpinner;
	private View fullConfigOptions;
	private String packageList;
	
	public static final String DEBUG_LEVEL[] = { 
		"-v 等级1", 
		"-v -v 等级2", 
		"-v -v -v 等级3", };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		setContentView(R.layout.monkey_configuration_layout);

		basicConfigCheckBox = (CheckBox) this
				.findViewById(R.id.basic_config_check_box);
		fullConfigCheckBox = (CheckBox) this
				.findViewById(R.id.full_config_check_box);
		whiteListCheckBox = (CheckBox) this
				.findViewById(R.id.white_list_check_box);
		blackListCheckBox = (CheckBox) this
				.findViewById(R.id.black_list_check_box);
		testDuration = (EditText) this
				.findViewById(R.id.monkey_test_duration_edit);
		operationDelay = (EditText) this
				.findViewById(R.id.monkey_test_operation_delay_edit);
		startButton = (Button) this.findViewById(R.id.monkey_test_start_button);
		resetButton = (Button) this
				.findViewById(R.id.monkey_test_reset_configuration_button);
		debugLevelSpinner = (Spinner) this
				.findViewById(R.id.debug_level_spinner);
		fullConfigOptions = findViewById(R.id.full_options);
		whiteListPlusButton = (Button)findViewById(R.id.monkey_test_white_list_plus_button);
		whiteListMinusButton = (Button)findViewById(R.id.monkey_test_white_list_minus_button);
		blackListPlusButton = (Button)findViewById(R.id.monkey_test_black_list_plus_button);
		blackListMinusButton = (Button)findViewById(R.id.monkey_test_black_list_minus_button);
		ignoreCrashCheckBox = (CheckBox)findViewById(R.id.monkey_test_ignore_crash_check_box);
		
		ArrayList<String> debugList = new ArrayList<String>();
		debugList = new ArrayList<String>();
		debugList.addAll(Arrays.asList(DEBUG_LEVEL));
		ArrayAdapter<String> debugLevelAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, debugList);
		debugLevelAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		debugLevelSpinner.setAdapter(debugLevelAdapter);
		debugLevelSpinner.setSelection(2);
		
		fullConfigOptions.setVisibility(View.GONE);
		whiteListPlusButton.setEnabled(false);
		whiteListMinusButton.setEnabled(false);
		
		packageList = DEFAULT_BLACK_LIST;

		startButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if(whiteListCheckBox.isChecked()) {
					if(packageList.isEmpty()) {
						Toast.makeText(MonkeyConfigActivity.this, getResources().getString(R.string.monkey_test_empty_white_list_error), Toast.LENGTH_SHORT).show();
						return;
					}
					createWhiteBlackList(WHITE_LIST);
				} else {
					createWhiteBlackList(BLACK_LIST);
				}
				
				
				Intent it = new Intent(MonkeyConfigActivity.this,MonkeyService.class);
				Bundle bundle = new Bundle();
				if(whiteListCheckBox.isChecked()) {
					bundle.putInt("mode", WHITE_MODE);
				} else {
					bundle.putInt("mode", BLACK_MODE);
				}
				bundle.putInt("duration", Integer.parseInt(testDuration.getText().toString()));
				bundle.putInt("delay", Integer.parseInt(operationDelay.getText().toString()));
				bundle.putBoolean("ignoreCrash", ignoreCrashCheckBox.isChecked());
				if(debugLevelSpinner.getSelectedItemPosition() == 0) {
					bundle.putString("debugLevel", "-v");
				} else if(debugLevelSpinner.getSelectedItemPosition() == 1) {
					bundle.putString("debugLevel", "-v -v");
				} else {
					bundle.putString("debugLevel", "-v -v -v");
				}

				it.putExtras(bundle);
				
				startService(it);
			}
		});
		
		resetButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {		
				basicConfigCheckBox.setChecked(true);
				fullConfigCheckBox.setChecked(false);
				blackListCheckBox.setChecked(true);
				whiteListCheckBox.setChecked(false);
				packageList = DEFAULT_BLACK_LIST;
				testDuration.setText(MonkeyConfigActivity.this.getResources().getString(R.string.monkey_test_duration_count));
				operationDelay.setText(MonkeyConfigActivity.this.getResources().getString(R.string.monkey_test_operation_delay_count));
				debugLevelSpinner.setSelection(2);
				ignoreCrashCheckBox.setChecked(false);
			}
		});
		
		basicConfigCheckBox
				.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							fullConfigCheckBox.setChecked(false);
							fullConfigOptions.setVisibility(View.GONE);
						} else {
							fullConfigCheckBox.setChecked(true);
							fullConfigOptions.setVisibility(View.VISIBLE);
						}
					}
				});
		fullConfigCheckBox
				.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							basicConfigCheckBox.setChecked(false);
							fullConfigOptions.setVisibility(View.VISIBLE);
						} else {
							basicConfigCheckBox.setChecked(true);
							fullConfigOptions.setVisibility(View.GONE);
						}
					}
				});
		whiteListCheckBox
				.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							packageList = "";
							blackListCheckBox.setChecked(false);
							whiteListPlusButton.setEnabled(true);
							whiteListMinusButton.setEnabled(true);
							blackListPlusButton.setEnabled(false);
							blackListMinusButton.setEnabled(false);
						} else {
							packageList = DEFAULT_BLACK_LIST;
							blackListCheckBox.setChecked(true);
							whiteListPlusButton.setEnabled(false);
							whiteListMinusButton.setEnabled(false);
							blackListPlusButton.setEnabled(true);
							blackListMinusButton.setEnabled(true);
						}
					}
				});
		blackListCheckBox
				.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							packageList = DEFAULT_BLACK_LIST;
							whiteListCheckBox.setChecked(false);
							whiteListPlusButton.setEnabled(false);
							whiteListMinusButton.setEnabled(false);
							blackListPlusButton.setEnabled(true);
							blackListMinusButton.setEnabled(true);
						} else {
							packageList = "";
							whiteListCheckBox.setChecked(true);
							whiteListPlusButton.setEnabled(true);
							whiteListMinusButton.setEnabled(true);
							blackListPlusButton.setEnabled(false);
							blackListMinusButton.setEnabled(false);
						}
					}
				});
		blackListPlusButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent it = new Intent(MonkeyConfigActivity.this,PackageListActivity.class);
				Bundle bundle = new Bundle();
				bundle.putInt("action", BLACK_LIST_PLUS);
				bundle.putString("list",packageList);
				it.putExtras(bundle);
				
				startActivityForResult(it, 100);
			}
		});
		blackListMinusButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent it = new Intent(MonkeyConfigActivity.this,PackageListActivity.class);
				Bundle bundle = new Bundle();
				bundle.putInt("action", BLACK_LIST_MINUS);
				bundle.putString("list",packageList);
				it.putExtras(bundle);
				
				startActivityForResult(it, 100);
			}
		});
		whiteListPlusButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent it = new Intent(MonkeyConfigActivity.this,PackageListActivity.class);
				Bundle bundle = new Bundle();
				bundle.putInt("action", WHITE_LIST_PLUS);
				bundle.putString("list",packageList);
				it.putExtras(bundle);
				
				startActivityForResult(it, 100);
			}
		});
		whiteListMinusButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent it = new Intent(MonkeyConfigActivity.this,PackageListActivity.class);
				Bundle bundle = new Bundle();
				bundle.putInt("action", WHITE_LIST_MINUS);
				bundle.putString("list",packageList);
				it.putExtras(bundle);
				
				startActivityForResult(it, 100);
			}
		});
		
		if(!RootTools.isAccessGiven()) {
			Dialog alertDialog = new AlertDialog.Builder(this).setTitle(R.string.attention)
					.setMessage(R.string.device_not_rooted)
					.setPositiveButton(this.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							MonkeyConfigActivity.this.finish();
						}
					}).create();
			alertDialog.show();
		}
	}
	
    @Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  
    {  
        if(PackageListActivity.RETURN_CODE_OK_BUTTON==resultCode) {  
    		Bundle bundle = data.getExtras();
    		packageList = bundle.getString("list");

    		if(packageList.equals("empty")) {
    			packageList = "";
    		}

    		Log.v(TAG,"---"+packageList+"---");
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void createWhiteBlackList(String path) {
		FileWriter listFile = null;
		try {
			listFile = new FileWriter(new File(path), false);
			String[] sArray = packageList.split(";");
			for (String p : sArray) {
				if(!p.isEmpty()) {
					listFile.append(p + "\n");
				}
			}
			listFile.flush();
			listFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
