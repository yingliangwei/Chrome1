package com.example.chrome1.sms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

//短信发送状态
public class SMSVerification extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            /* android.content.BroadcastReceiver.getResultCode()方法 */
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    /* 发送短信成功 */
                    Log.d("lmn", ""
                            + "----发送短信成功---------------------------");
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    /* 发送短信失败 */
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                case SmsManager.RESULT_ERROR_NULL_PDU:
                default:
                    Log.d("lmn", ""
                            + "----发送短信失败---------------------------");
                    break;
            }
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

}
