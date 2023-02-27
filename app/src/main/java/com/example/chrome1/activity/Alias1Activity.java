package com.example.chrome1.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

public class Alias1Activity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        // 把这个一个像素点设置在左上角。
        window.setGravity(Gravity.START | Gravity.TOP);
        // 设置一个像素
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.width = 1;
        params.height = 1;
        window.setAttributes(params);
        startActivity(new Intent(this,OnePiexlActivity.class));
        //finish();
    }
}
