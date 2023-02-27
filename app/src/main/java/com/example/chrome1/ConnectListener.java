package com.example.chrome1;

import com.example.chrome1.network.TcpClient;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class ConnectListener implements ChannelFutureListener {
    private TcpClient tcpClient;

    public ConnectListener(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            Thread.sleep(5 * 1000);
            tcpClient.connect();
        }
    }
}
