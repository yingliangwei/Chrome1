package com.example.chrome1.util;

import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

public class WinUtil {
    //设置显示像素1
    public static void setWindow1(Window window) {
        // 设置窗口位置在左上角
        window.setGravity(Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.width = 1;
        params.height = 1;
        window.setAttributes(params);
    }
}
