package com.clock.pt1.keeptesting;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.clock.pt1.keeptesting.batterycontroller.BatteryControllerActivity;
import com.clock.pt1.keeptesting.clock.SleepAndWakeUpActivity;
import com.clock.pt1.keeptesting.monkey.MonkeyConfigActivity;
import com.clock.pt1.keeptesting.orientationtester.OrientationTestActivity;
import com.clock.pt1.keeptesting.packageinstaller.PackageInstaller;
import com.clock.pt1.keeptesting.storagetester.StorageTestActivity;
import com.stericson.RootTools.RootTools;

@SuppressLint("SdCardPath")
public class MainActivity extends Activity {

    public static final int AUTO_BOOT_MAIN = 0;
    public static final int AUTO_BOOT_REBOOT = 1;
    public static final int AUTO_BOOT_OTA = 2;

    public static final String TAG = "Keeptesting";
    // public static final String ACTIVITY_NAME = "standbytest";
    public static final String LOOP_NUMBER = "loop";
    public static final String CURRENT_NUMBER = "current";
    public static final String SLEEP_INTERVAL = "sleeptime";
    public static final String WAKEUP_INTERVAL = "wakeuptime";
    public static final String IDLE_INTERVAL = "idletime";
    public static final String START_DELAY = "startdelay";
    public static final String IS_AUTO_FLAG = "isAutoFlag";

    public String standbytest = "";
    public boolean isAutoFlag = false;
    public int iLoop = -1;
    public int iSleep = 4;
    public int iWakeup = 5;

    private TextView rootInfoText;
    private ImageTextButton sleepWakeupBtn;
    private ImageTextButton repeatRebootBtn;
    private ImageTextButton repeatOTABtn;
    private ImageTextButton ddrTestBtn;
    private ImageTextButton storageTestBtn;
    private ImageTextButton orientationTestBtn;
    private ImageTextButton monkeyTestBtn;
    private ImageTextButton batteryControllBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootInfoText = (TextView) findViewById(R.id.root_info);
        sleepWakeupBtn = (ImageTextButton) findViewById(R.id.sleep_wakeup);
        repeatRebootBtn = (ImageTextButton) findViewById(R.id.repeat_reboot);
        repeatOTABtn = (ImageTextButton) findViewById(R.id.repeat_ota);
        ddrTestBtn = (ImageTextButton) findViewById(R.id.ddr_test);
        storageTestBtn = (ImageTextButton) findViewById(R.id.storage_test);
        orientationTestBtn = (ImageTextButton) findViewById(R.id.orientation_test);
        monkeyTestBtn = (ImageTextButton) findViewById(R.id.monkey_test);
        batteryControllBtn = (ImageTextButton) findViewById(R.id.battery_controll);

        LogUtil.prepareFolder();
        /* turn on "keep screen on when charging" option */
        Settings.Global
                .putInt(this.getContentResolver(),
                        Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                        (BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB));

        if (RootTools.isAccessGiven()) {
            rootInfoText.setVisibility(View.GONE);
        }
        sleepWakeupBtn
                .setOnClickListener(new ImageTextButton.OnClickListener() {
                    public void onClick(View v) {
                        Log.v(TAG, "Starting sleep&wakeup test");
                        checkPreCondition(SleepAndWakeUpActivity.class, null,
                                null, null);
                    }
                });
        repeatRebootBtn
                .setOnClickListener(new ImageTextButton.OnClickListener() {
                    public void onClick(View v) {
                        Log.v(TAG, "Starting repeat reboot test");
                        checkPreCondition(RebootActivity.class, null, null,
                                null);
                    }
                });
        repeatOTABtn.setOnClickListener(new ImageTextButton.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "Starting repeat OTA test");
                checkPreCondition(OTAActivity.class, null, null, null);
            }
        });
        ddrTestBtn.setOnClickListener(new ImageTextButton.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "Starting ddr test");
                checkPreCondition(DDRTestActivity.class, null, null, null);
            }
        });
        /*
         * fqrouterBtn.setOnClickListener(new ImageTextButton.OnClickListener()
         * { public void onClick(View v) { Log.v(TAG,"Starting fqrouter");
         * gotoActivity
         * (null,FQROUTER_PKG_NAME,"fq.router2.MainActivity",FQROUTER_APK_PATH);
         * } });
         */
        storageTestBtn
                .setOnClickListener(new ImageTextButton.OnClickListener() {
                    public void onClick(View v) {
                        Log.v(TAG, "Starting storage test");
                        gotoActivity(StorageTestActivity.class, null, null,
                                null);
                    }
                });
        orientationTestBtn
                .setOnClickListener(new ImageTextButton.OnClickListener() {
                    public void onClick(View v) {
                        Log.v(TAG, "Starting orientation test");
                        checkPreCondition(OrientationTestActivity.class, null,
                                null, null);
                    }
                });
        monkeyTestBtn.setOnClickListener(new ImageTextButton.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "Starting monkey test");
                checkPreCondition(MonkeyConfigActivity.class, null, null, null);
            }
        });
        batteryControllBtn
                .setOnClickListener(new ImageTextButton.OnClickListener() {
                    public void onClick(View v) {
                        Log.v(TAG, "Starting batteryController");
                        gotoActivity(BatteryControllerActivity.class, null,
                                null, null);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        switch (item_id) {
        case R.id.about:
            Dialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.about)
                    .setMessage(R.string.about_info)
                    .setPositiveButton(
                            this.getResources().getString(R.string.confirm),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                }
                            }).create();
            alertDialog.show();
            break;
        }
        return true;
    }

    public boolean checkPreCondition(final Class<?> cls,
            final String packageName, final String activityName,
            final String filePath) {
        int item = 1;
        String message = "";
        Intent intent = this.registerReceiver(null, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

        Log.i("Keeptesting", "battery plugged = " + plugged + " " + level);

        if (plugged == BatteryManager.BATTERY_PLUGGED_AC
                || (plugged == BatteryManager.BATTERY_PLUGGED_USB && level == 0)) {
            ;
        } else {
            if (true == isAutoFlag) {
                message += item
                        + this.getResources()
                                .getString(R.string.charger_needed) + "\n";
                item++;
            }
            Log.v("Keeptesting", "No check battery if isAutoFlag = true");
        }
        Class<?> c = null;
        Method getPropertyMethod = null;
        try {
            String figerprint = null;
            c = Class.forName("android.os.SystemProperties");
            getPropertyMethod = c
                    .getMethod("get", new Class[] { String.class });
            figerprint = (String) getPropertyMethod.invoke(c,
                    "ro.build.fingerprint");
            Pattern p = Pattern.compile("(201\\d)(\\d\\d)(\\d\\d)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(figerprint);
            if (m.find()) {
                int firmwareYear = Integer.parseInt(m.group(1).trim());
                int firmwareMonth = Integer.parseInt(m.group(2).trim());
                int firmwareDate = Integer.parseInt(m.group(3).trim());

                Time t = new Time();
                t.setToNow();
                int nowYear = t.year;
                int nowMonth = t.month + 1; /* month:(0-11) */
                int nowDate = t.monthDay;
                boolean invalidTime = false;

                if (nowYear < firmwareYear) {
                    invalidTime = true;
                } else if (nowYear == firmwareYear) {
                    if (nowMonth < firmwareMonth) {
                        invalidTime = true;
                    } else if (nowMonth == firmwareMonth) {
                        if (nowDate < firmwareDate) {
                            invalidTime = true;
                        }
                    }
                }

                if (invalidTime) {
                    message += item
                            + this.getResources().getString(
                                    R.string.wrong_system_time) + "\n";
                    item++;
                }
            }
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (!message.isEmpty() && false == isAutoFlag) {
            Dialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.info_box_title)
                    .setMessage(message)
                    .setPositiveButton(
                            this.getResources().getString(R.string.exit),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                }
                            })
                    .setNegativeButton(R.string.skip,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    gotoActivity(cls, packageName,
                                            activityName, filePath);
                                }
                            }).create();
            alertDialog.show();
            return false;
        } else {
            gotoActivity(cls, packageName, activityName, filePath);
            return true;
        }
    }

    public void initAPKFile(String filePath) {
        File cmdFile = new File(filePath);

        if (!cmdFile.exists()) {
            Log.d(TAG, " create apk file on cache.");
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                String[] fileName = filePath.split("/");
                is = getAssets().open(fileName[fileName.length - 1]);
                fos = new FileOutputStream(cmdFile);

                byte[] buff = new byte[2048];
                int length = 0;
                while ((length = is.read(buff)) != -1) {
                    fos.write(buff, 0, length);
                }
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // change permission
            DataOutputStream dos = null;
            try {
                Process p = Runtime.getRuntime().exec("chmod 777 " + filePath);
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void gotoActivity(Class<?> cls, String packageName,
            String activityName, String filePath) {
        if (filePath != null) {
            if (!PackageInstaller.isPackageInstalled(MainActivity.this,
                    packageName)) {
                initAPKFile(filePath);
                PackageInstaller.installPackage(filePath);
            }
            ComponentName componetName = new ComponentName(packageName,
                    activityName);
            Intent intent = new Intent();
            intent.setComponent(componetName);
            startActivity(intent);

        } else {
            Intent it = new Intent(this, cls);
            startActivity(it);

        }
    }
}
