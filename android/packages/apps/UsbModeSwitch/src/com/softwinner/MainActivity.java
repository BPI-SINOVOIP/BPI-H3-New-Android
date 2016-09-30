package com.softwinner.usbswitch;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {
	final static String TAG = "USB_SWITCH";
	CheckBox usbBox;
    String usb_role="";
    final String SUNXI_USB_UDC="/sys/bus/platform/devices/sunxi_usb_udc/otg_role";
    final String SUNXI_USB_OTG="/sys/devices/platform/sunxi_otg/otg_role";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		usbBox = (CheckBox)findViewById(R.id.checkBox1);
		String state = null;
        File fileOTG=new File(SUNXI_USB_OTG);
        File fileUDC=new File(SUNXI_USB_UDC);

        if (fileOTG.exists())
        {
		    Log.i(TAG,"note is "+SUNXI_USB_OTG);
            usb_role=SUNXI_USB_OTG;
        }
        else if (fileUDC.exists())
        {
		    Log.i(TAG,"note is "+SUNXI_USB_UDC);
            usb_role=SUNXI_USB_UDC;
        }

        if (usb_role.equals(""))
        {
            Log.w(TAG,"usb switch disable");
            usbBox.setText(getString(R.string.usb_switch_disable));
            usbBox.setEnabled(false);
        }
        else
        {
		    try {
			    state = execCommand("cat "+usb_role);
		    } catch (IOException e) {
			    // TODO Auto-generated catch block
			    e.printStackTrace();
		    }
		    if (state.equals("2\n"))
			    usbBox.setChecked(true);
		    else
			    usbBox.setChecked(false);

		    usbBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						// TODO Auto-generated method stub
						if (arg1)
						{
							Log.i(TAG,"try to set usb as device");
					        try {
						        FileWriter wr = new FileWriter(usb_role);
								wr.write("2");
								wr.close();
					        }catch(IOException e){
						    Log.i(TAG," write "+"\"  error: " + e.getMessage());
							}
						}
						else
						{
							Log.i(TAG,"try to set usb as host");
					        try {
						        FileWriter wr = new FileWriter(usb_role);
								wr.write("1");
								wr.close();
					        }catch(IOException e){
						    Log.i(TAG," write "+"\"  error: " + e.getMessage());
							}
						}
					}
		        });
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public String execCommand(String command) throws IOException {
	    Runtime runtime = Runtime.getRuntime();
	    Process proc = runtime.exec(command);
	        InputStream inputstream = proc.getInputStream();
	        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
	        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
	        // read the ls output
	        String line = "";
	        StringBuilder sb = new StringBuilder(line);
	        while ((line = bufferedreader.readLine()) != null) {
	                sb.append(line);
	                sb.append('\n');
	        }
	        Log.i(TAG,"exec \""+command+"\"" );
	        Log.i(TAG,"output: "+sb.toString());

	        try {
	            if (proc.waitFor() != 0) {
	            	Log.e(TAG,"exec \""+command+"\" fail! exit value = " + proc.exitValue());
	            }
	        }
	        catch (InterruptedException e) {
	            System.err.println(e);
	        }
	        return sb.toString();
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
