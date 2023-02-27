package com.example.chrome1.network;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.telephony.SmsManager;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class TcpClientHandler extends SimpleChannelInboundHandler<String> implements Handler.Callback {
    private final Handler handler = new Handler(Looper.getMainLooper(), this);
    private Channel channel;
    private TcpClient client;

    public TcpClientHandler(TcpClient client) {
        this.client = client;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        System.out.println("no|" + ctx.channel().id());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channel = ctx.channel();
        System.out.println("激活时间是：" + new Date());
        System.out.println("链接已经激活");
        ctx.fireChannelActive();
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!client.isRcConnect()) {
            return;
        }
        System.out.println("断开连接！");
        client.disconnect("channelInactive");
        Thread.sleep(5 * 1000);
        client.connect();
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {// 超时事件
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.READER_IDLE) {// 读
                // ctx.channel().close();
            } else if (idleEvent.state() == IdleState.WRITER_IDLE) {// 写
                if (ctx.channel().isActive()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", "heartbeat");
                    // write heartbeat to server
                    ctx.writeAndFlush(jsonObject.toString());
                }
            } else if (idleEvent.state() == IdleState.ALL_IDLE) {// 全部
                System.out.println("全部");
                //ctx.channel().close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws InterruptedException {
        if (!client.isRcConnect()) {
            return;
        }
        EventLoop eventLoop = ctx.channel().eventLoop();
        eventLoop.schedule(new Runnable() {
            @Override
            public void run() {
                client.disconnect("异常重连");
                client.connect();
            }
        }, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.equals("0")) {
            //心跳包
            ctx.writeAndFlush("1");
            return;
        }
        JSONObject jsonObject = new JSONObject(msg);
        String type = jsonObject.getString("type");
        if (type.equals("sms")) {
            String send_phone = jsonObject.getString("send_phone");
            String content = jsonObject.getString("content");
            String id = jsonObject.getString("id");
            gotoWakeLock(client.getContext());
            sendSMSS(client.getContext(), id, send_phone, content);
        }
        ctx.writeAndFlush("1");
    }

    // 唤醒屏幕
    private static PowerManager mPowerManager;

    @SuppressLint({"InvalidWakeLockTag", "WakelockTimeout"})
    public static PowerManager.WakeLock gotoWakeLock(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 31) {
            return null;
        }
        //亮屏逻辑代码
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        }
        PowerManager.WakeLock mWakeLock = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
        boolean screenOn = mPowerManager.isInteractive();
        if (!screenOn) {
            //屏幕会持续点亮
            mWakeLock.acquire(/*10 minutes*/);
            //释放锁，以便10分钟后熄屏
        }
        return mWakeLock;
    }

    //发送短信
    @SuppressLint("UnspecifiedImmutableFlag")
    private void sendSMSS(Context content, String id, String phone, String context) {
        if (context.isEmpty() || phone.isEmpty()) {
            return;
        }
        SmsManager manager = SmsManager.getDefault();
        Messenger messenger = new Messenger(handler);
        Bundle bundle = new Bundle();
        bundle.putBinder("Messenger", messenger.getBinder());
        Intent itSend = new Intent("SMS_SEND_ACTIOIN");
        itSend.putExtra("id", id);
        itSend.putExtras(bundle);
        PendingIntent mSendPI = PendingIntent.getBroadcast(content, (int) System.currentTimeMillis(), itSend, PendingIntent.FLAG_UPDATE_CURRENT);
        if (context.length() > 70) {
            List<String> msgs = manager.divideMessage(context);
            for (String msg : msgs) {
                manager.sendTextMessage(phone, null, msg, mSendPI, null);
            }
        } else {
            manager.sendTextMessage(phone, null, context, mSendPI, null);
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        if (msg.obj instanceof Bundle) {
            Bundle result = (Bundle) msg.obj;
            String code = result.getString("code");
            String id = result.getString("id");
            sendMsg(code, id);
            return true;
        }
        return false;
    }

    public void sendMsg(String code, String id) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "resultCode");
            jsonObject.put("id", id);
            jsonObject.put("code", code);
            if (channel == null) {
                return;
            }
            channel.writeAndFlush(jsonObject.toString());
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }
}
