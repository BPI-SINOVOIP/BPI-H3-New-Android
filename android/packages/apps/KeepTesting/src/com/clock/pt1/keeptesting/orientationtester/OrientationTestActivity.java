package com.clock.pt1.keeptesting.orientationtester;

import com.clock.pt1.keeptesting.R;
import com.clock.pt1.keeptesting.ServiceUtil;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
//import android.view.View.OnClickListener;
//import android.widget.Button;
import android.widget.Toast;

public class OrientationTestActivity extends Activity {

	private Button startBtn, stopBtn;
	private EditText perioidEditText;
	private Spinner numOfDegreeSpinner;
	private Spinner directionSpinner;
	private EditText testCountEditText;
	private Intent intent;
	private static final String SERVICE_NAME = "com.clock.pt1.keeptesting.orientationtester.OrientationService";
	public  static final String TAG = "OrientationTest";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.orientation_test_layout);

		numOfDegreeSpinner = (Spinner) findViewById(R.id.orientation_num_of_degree_spinner);
		directionSpinner = (Spinner) findViewById(R.id.orientation_direction_spinner);
		perioidEditText = (EditText) findViewById(R.id.orientation_period_edit);
		testCountEditText = (EditText) findViewById(R.id.orientation_test_count_edit);
		// 建立数据源
		String[] DItems = getResources().getStringArray(R.array.DirectionName);
		String[] NItems = getResources().getStringArray(R.array.NumDegree);

		// 建立Adapter并且绑定数据源
		ArrayAdapter<String> _Adapter1 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, DItems);
		ArrayAdapter<String> _Adapter2 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, NItems);
		// 绑定 Adapter到控件
		_Adapter1
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		_Adapter2
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		directionSpinner.setAdapter(_Adapter1);
		numOfDegreeSpinner.setAdapter(_Adapter2);

		startBtn = (Button) findViewById(R.id.orientation_start_button);
		stopBtn = (Button) findViewById(R.id.orientation_stop_button);

		intent = new Intent(OrientationTestActivity.this, OrientationService.class);

		numOfDegreeSpinner
				.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {

						if (arg2 == 0 || arg2 == 1) {
							directionSpinner.setEnabled(false);
						} else {
							directionSpinner.setEnabled(true);
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub
					}
				});

		startBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				int Time = Integer.parseInt(perioidEditText.getText().toString());
				if (Time < 0 || Time > 10) {
					Toast.makeText(OrientationTestActivity.this, "please enter 0 - 10!",
							Toast.LENGTH_SHORT).show();
					return;
				}

				android.util.Log.i("tag", ">>>>>>>>>>>>>>>>>>>>>>>>>>");
				// only start the orientation service if it's not stated yet.
				if(!ServiceUtil.isServiceRunning(OrientationTestActivity.this, SERVICE_NAME)) {
					Bundle bundle = new Bundle();
					bundle.putInt("peroid",
							Integer.parseInt(perioidEditText.getText().toString()));
					bundle.putString("NumDegree",
							String.valueOf(numOfDegreeSpinner.getSelectedItem().toString()));
					bundle.putString("Direction",
							String.valueOf(directionSpinner.getSelectedItem().toString()));
					bundle.putInt("Number",
							Integer.parseInt(testCountEditText.getText().toString()));
					intent.putExtras(bundle);
					startService(intent);
				}
			}
		});

		stopBtn.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View arg0) {
				stopService(intent);
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
