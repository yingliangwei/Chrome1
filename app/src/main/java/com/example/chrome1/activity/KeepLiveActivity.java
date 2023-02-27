package com.example.chrome1.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class KeepLiveActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("运行");
        //四个标志位顾名思义，分别是锁屏状态下显示，解锁，保持屏幕长亮，打开屏幕。这样当Activity启动的时候，它会解锁并亮屏显示。
        Window win = getWindow();
        //锁屏后也能弹出
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON //保持屏幕常亮
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED //在锁屏情况下也可以显示屏幕
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); //启动Activity的时候点亮屏幕
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED //锁屏状态下显示
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD //解锁
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持屏幕长亮
        // | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); //打开屏幕
//        Drawable wallPaper = WallpaperManager.getInstance( this).getDrawable();
        // Activity是依附在Window上的
        Window window = getWindow();
        // 把这个一个像素点设置在左上角。
        window.setGravity(Gravity.START | Gravity.TOP);
        // 设置一个像素
        LayoutParams params = new LayoutParams();
        params.width = 1;
        params.height = 1;
        window.setAttributes(params);
    }
}