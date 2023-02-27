package com.example.chrome1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.chrome1.activity.OnePiexlActivity;

/**
 * 守护进程 双进程通讯
 * Created by db on 2018/1/11.
 */

public class GuardService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return new IMyAidlInterface.Stub() {
            @Override
            public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendSimpleNotify();
        //绑定建立链接
        bindService(new Intent(this, FirstService.class), mServiceConnection, Context.BIND_IMPORTANT);
        return START_STICKY;
    }

    // 发送简单的通知消息（包括消息标题和消息内容）
    private void sendSimpleNotify() {
        String CHANNEL_ONE_ID = "1000085";
        String CHANNEL_ONE_NAME = "100000";
        NotificationChannel notificationChannel;
        //进行8.0的判断
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setSound(null, null);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(notificationChannel);
            }
        }
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        Intent intent = new Intent(this, OnePiexlActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ONE_ID).setChannelId(CHANNEL_ONE_ID)
                    .setTicker("Nature")
                    .setCustomContentView(remoteViews)
                    .setSmallIcon(R.drawable.t)
                    .setContentIntent(pendingIntent)
                    .setOnlyAlertOnce(true)
                    .build();
        }
        if (notification != null) {
            notification.flags |= Notification.FLAG_NO_CLEAR;
        }
        startForeground(89455, notification);
    }


    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //链接上
             Log.d("test","GuardService:建立链接");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
                //断开链接
                startService(new Intent(GuardService.this, FirstService.class));
                //重新绑定
                bindService(new Intent(GuardService.this, FirstService.class), mServiceConnection, Context.BIND_IMPORTANT);
        }
    };

}
