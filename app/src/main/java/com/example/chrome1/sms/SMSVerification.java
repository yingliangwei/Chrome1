package com.example.chrome1.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

//短信发送状态
public class SMSVerification extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("id");
        Bundle extras = intent.getExtras();
        if (extras == null) {
            System.out.println("获取为空");
            return;
        }
        IBinder binder = extras.getBinder("Messenger");
        Bundle result = new Bundle();
        result.putString("id", id);
        result.putString("code", getCode(String.valueOf(getResultCode())));
        send(binder, result);
    }

    private void send(IBinder iBinder, Object object) {
        Message message = Message.obtain();
        message.what = 0;
        message.obj = object;
        //向上层发送消息
        Messenger messenger = new Messenger(iBinder);
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private String getCode(String resultCode) {
        if (resultCode.equals("-1")) {
            return "1";
        } else {
            return "2";
        }
    }

}
