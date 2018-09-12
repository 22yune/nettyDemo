package com.demo.netty.tcpClient.async;

import com.demo.netty.tcpClient.TcpClientConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.util.concurrent.Future;

/**
 * 异步 通信客户端
 */
public interface AsyncTcpClient {

    void open(TcpClientConfig config) throws Exception;

    ChannelFuture close();

    /**
     * 消息发送接口  消息接受的处理在channelHandler中。
     * @param msg
     * @return
     */
    Future<Void> send(Object msg);

    /**
     * 底层真正通信的netty的Channel
     * @return
     */
    Channel channel();
}
