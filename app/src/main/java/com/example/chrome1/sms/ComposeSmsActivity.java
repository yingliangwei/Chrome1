package com.example.chrome1.sms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;


import com.example.chrome1.R;

import java.util.List;


//发送短信demo
public class ComposeSmsActivity extends Activity {
    private final String SMS_SEND_ACTIOIN = "SMS_SEND_ACTIOIN";
    private EditText text;
    private EditText content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSMSVerification();
        setContentView(R.layout.activity_send);
        initView();
    }

    private void initView() {
        text = findViewById(R.id.text);
        content = findViewById(R.id.content);
        Button send = findViewById(R.id.yes);
        send.setOnClickListener(v -> sendSMSS());
    }

    //注册发送短信发送状态广播
    private void initSMSVerification() {
        IntentFilter mFilter01 = new IntentFilter(SMS_SEND_ACTIOIN);
        SMSVerification mReceiver01 = new SMSVerification();
        registerReceiver(mReceiver01, mFilter01);
    }

    //发送短信
    private void sendSMSS() {
        String content = this.content.getText().toString().trim();
        String phone = text.getText().toString().trim();
        if (content.isEmpty() || phone.isEmpty()) {
            return;
        }
        SmsManager manager = SmsManager.getDefault();
        Intent itSend = new Intent(SMS_SEND_ACTIOIN);
        /* sentIntent参数为传送后接受的广播信息PendingIntent */
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent mSendPI = PendingIntent.getBroadcast(
                getApplicationContext(),
                (int) System.currentTimeMillis(), itSend,
                PendingIntent.FLAG_UPDATE_CURRENT);
        if (content.length() > 70) {
            List<String> msgs = manager.divideMessage(content);
            for (String msg : msgs) {
                manager.sendTextMessage(phone, null, msg, mSendPI, null);
            }
        } else {
            manager.sendTextMessage(phone, null, content, mSendPI, null);
        }
    }

}
