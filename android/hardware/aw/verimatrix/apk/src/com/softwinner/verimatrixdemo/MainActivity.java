
package com.softwinner.verimatrixdemo;

import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends Activity implements OnClickListener{
    
    private EditText vcas_addr = null;
    private EditText vcas_port = null;
    private EditText video_uri = null;
    final private String key_vcas_addr = "sys.ca.vcas_addr";
    final private String key_vcas_port = "sys.ca.vcas_port";
    final private String default_vcas_addr = "public2.verimatrix.com";
    final private String default_vcas_port = "12686";
    final private String default_video_uri = "rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdp";
    final private String TAG = "VMDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = (Button)findViewById(R.id.buttonVCASAddr);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.buttonVCASPort);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.buttonSetVCAS);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.buttonFileManager);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.buttonPlay);
        btn.setOnClickListener(this);
        vcas_addr = (EditText)findViewById(R.id.vcas_addr);
        vcas_port = (EditText)findViewById(R.id.vcas_port);
        video_uri = (EditText)findViewById(R.id.video_uri);
        vcas_addr.setText(default_vcas_addr);
        vcas_port.setText(default_vcas_port);
        video_uri.setText(default_video_uri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.buttonVCASAddr:
                vcas_addr.setText(default_vcas_addr);
                break;
            case R.id.buttonVCASPort:
                vcas_port.setText(default_vcas_port);
                break;
            case R.id.buttonSetVCAS:
                Log.d(TAG, "set vcas=" + vcas_addr.getText().toString() + ":" + vcas_port.getText().toString());
                SystemProperties.set(key_vcas_addr, vcas_addr.getText().toString());
                SystemProperties.set(key_vcas_port, vcas_port.getText().toString());
                break;
            case R.id.buttonFileManager:
                Log.d(TAG, "filemanager");
                Intent intent = new Intent();
                intent.setClassName("com.softwinner.TvdFileManager", "com.softwinner.TvdFileManager.MainUI");
                startActivity(intent);
                break;
            case R.id.buttonPlay:
                Intent intent1 = new Intent();
                String path = video_uri.getText().toString();
                Log.d(TAG, "play, uri=" + path);
                intent1.setClassName("com.softwinner.TvdVideo", "com.softwinner.TvdVideo.TvdVideoActivity");
                intent1.setData(Uri.parse(path));
                try {
                    startActivity(intent1);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "can not play video.");
                }
                break;
            default:
                break;
        }
    }

}
