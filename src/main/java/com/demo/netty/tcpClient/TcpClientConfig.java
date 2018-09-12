package com.demo.netty.tcpClient;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;

import java.util.List;
import java.util.Map;

public interface TcpClientConfig {

    String getIp();/* ip地址  */

    int getPort(); /*  端口号  */

    <T> TcpClientConfig option(ChannelOption<T> option, T value);
    Map<ChannelOption<?>, Object> options();

    <T> TcpClientConfig attr(AttributeKey<T> key, T value);
    Map<AttributeKey<?>, Object> attrs();

    /**  编解码 协议相关：握手认证、心跳检测 业务消息*/
    List<ChannelHandlerFactory> getHandlerFactories();

    public interface ChannelHandlerFactory {
        ChannelHandler newChannelHandler();
    }
}
