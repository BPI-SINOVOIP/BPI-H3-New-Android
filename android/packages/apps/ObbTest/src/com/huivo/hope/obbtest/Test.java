package com.huivo.hope.obbtest;

import android.app.Activity;
import android.os.Bundle;
import android.os.storage.OnObbStateChangeListener;
import android.os.storage.StorageManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by litao on 2015/5/8.
 */
public class Test extends Activity {
    private static final String TAG = Test.class.getSimpleName() ;

    StorageManager storageManager ;

    private Button mMountButton ;
    private Button mUnmountButton ;
    private EditText mObbFileEdit ;
    private EditText mMountPathEdit ;
    private EditText mMountState ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        storageManager = (StorageManager) getSystemService(STORAGE_SERVICE);
        mMountButton = (Button) findViewById(R.id.mount_button);
        mUnmountButton = (Button) findViewById(R.id.unmount_button);
        mObbFileEdit = (EditText) findViewById(R.id.obb_file_edit);
        mMountPathEdit = (EditText) findViewById(R.id.mount_path_edit);
        mMountState = (EditText) findViewById(R.id.status_edit);
        mMountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storageManager.mountObb(mObbFileEdit.getText().toString(), "12345678", new ObbStateChangeListener());
            }
        });
        mUnmountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storageManager.unmountObb(mObbFileEdit.getText().toString(),true,new ObbStateChangeListener()) ;
            }
        });

    }
    class ObbStateChangeListener extends OnObbStateChangeListener {
        @Override
        public void onObbStateChange(String path, int state) {
            mMountState.setText(state+"");
            if(state == MOUNTED){
                mMountPathEdit.setText(storageManager.getMountedObbPath(path));
            }
            System.out.println("lt--> onObb state Change:"+path+" state:"+state+" get path:"+storageManager.getMountedObbPath(path));

        }
    }
}
