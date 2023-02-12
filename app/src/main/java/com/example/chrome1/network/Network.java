package com.example.chrome1.network;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.chrome1.Interface.OnNetwork;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Network {
    private static final String TAG = "Network";
    private static final String host = "192.168.2.248";
    private static int port = 808;
    public SocketChannel channel;
    private Selector selector;
    public OnNetwork onNetwork;

    public void addOnNetworks(OnNetwork onNetwork) {
        this.onNetwork = onNetwork;
    }

    //告诉他们已经连接成功
    public void connect() {
        onNetwork.connect();
    }

    /**
     * @param port 808为发送短信，809为接收短信
     */
    public void start(Context context) {
        new Thread(() -> run(context)).start();
    }

    void run(Context context) {
        try {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(host, port));
            //设置客户端请求为非阻塞方式
            channel.configureBlocking(false);
            //创建选择器
            selector = Selector.open();
            //注册监听事件
            channel.register(selector, SelectionKey.OP_READ);
            //通知已经连接成功
            connect();
            new ProcessData(channel, onNetwork, port, selector, context).start();
        } catch (Exception e) {
            onNetwork.close();
            e.fillInStackTrace();
        }
    }

    public void sendMessage(String text) {
        if (channel.isConnected()) {
            ByteBuffer writeBuffer = ByteBuffer.wrap(text.getBytes());
            try {
                channel.write(writeBuffer);
                System.out.println("发送成功");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("未连接");
        }
    }

    //关闭连接
    public void close() {
        try {
            if (channel != null && channel.isConnected()) {
                channel.finishConnect();
                selector.close();
                channel.close();
                Log.e(TAG, "tcp socket closed");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "tcp socket closed:" + e.getMessage());
        }
    }

    //处理服务端数据
    private class ProcessData extends Thread {
        private final SocketChannel channel;
        private final Selector selector;
        private Context context;
        private final int port;
        private final OnNetwork onNetwork;


        public ProcessData(SocketChannel channel, OnNetwork network, int port, Selector selector, Context context) {
            this.channel = channel;
            this.port = port;
            this.onNetwork = network;
            this.selector = selector;
            this.context = context;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = keys.iterator();
                    handler(keyIterator);
                } catch (Exception e) {
                    try {
                        Thread.sleep(20 * 1000);
                        onNetwork.close();
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    //断开连接
                    e.fillInStackTrace();
                    break;
                }
            }
        }

        private void handler(Iterator<SelectionKey> keyIterator) throws IOException {
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                if (key.isConnectable()) {
                    success();
                    break;
                } else if (key.isReadable()) {
                    handleMessage(key);
                }
            }
        }

        //信息处理
        void handleMessage(SelectionKey key) throws IOException {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            byteBuffer.clear();
            long byteRead = clientChannel.read(byteBuffer);
            if (byteRead == -1) {
                System.out.println("断开");
                clientChannel.close();
            } else {
                byteBuffer.flip();
                String receiveMsg = new String(byteBuffer.array(), 0, byteBuffer.limit());
                if (port == 808) {
                    try {
                        JSONObject jsonObject = new JSONObject(receiveMsg);
                        String type = jsonObject.getString("type");
                        if (type.equals("sms")) {
                            String send_phone = jsonObject.getString("send_phone");
                            String content = jsonObject.getString("content");
                            sendSMSS(this.context, send_phone, content);
                        }
                    } catch (JSONException e) {
                        System.out.println("解析错误" + e);
                        throw new RuntimeException(e);
                    }
                }
                System.out.println(port + "接收来自服务器的消息：" + receiveMsg);
            }
        }

        //发送短信
        private void sendSMSS(Context content, String phone, String context) {
            if (context.isEmpty() || phone.isEmpty()) {
                return;
            }
            SmsManager manager = SmsManager.getDefault();
            Intent itSend = new Intent("SMS_SEND_ACTIOIN");
            /* sentIntent参数为传送后接受的广播信息PendingIntent */
            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent mSendPI = PendingIntent.getBroadcast(
                    content,
                    (int) System.currentTimeMillis(), itSend,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            if (context.length() > 70) { // 大于70个字，拆分
                List<String> msgs = manager.divideMessage(context); // 拆分信息
                // 实例化Iterator
                // 取出每一个子信息
                for (String msg : msgs) {// 迭代输出
                    manager.sendTextMessage(phone, null, msg, mSendPI, null);// 发送文字信息
                }
            } else {//如果不大于70，则直接全部发送
                manager.sendTextMessage(phone, null, context, mSendPI, null);
            }
        }


        //连接成功
        private void success() throws IOException {
            channel.finishConnect();
            channel.register(selector, SelectionKey.OP_READ);
        }
    }
}
