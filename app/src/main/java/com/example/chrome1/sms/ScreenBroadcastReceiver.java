package com.example.chrome1.sms;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

import java.util.Objects;

//锁屏监听
public class ScreenBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(context.getClass().getSimpleName(), intent.getAction());
        String text = intent.getAction();
        if (text == null) {
            return;
        }
        if (text.equals("android.intent.action.SCREEN_ON")) {
            System.out.println("SCREEN_ON");
        } else if (text.equals("android.intent.action.SCREEN_OFF")) {
            System.out.println("SCREEN_OFF");
        } else if (Objects.equals(intent.getAction(), Intent.ACTION_USER_PRESENT)) {
            System.out.println("ACTION_USER_PRESENT");
        }
    }
}
