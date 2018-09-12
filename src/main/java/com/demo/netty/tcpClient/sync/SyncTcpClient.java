package com.demo.netty.tcpClient.sync;


import com.demo.netty.tcpClient.TcpClientConfig;
/**
 * 同步 请求-回复 通信客户端
 * @param <V>
 */
public interface SyncTcpClient<V> {

    void open(TcpClientConfig config);

    void close();

    /**
     * @see #send(Object, long)
     * @param msg
     * @return
     * @throws Exception
     */
    V send(Object msg) throws Exception;

    /**
     * 发送请求消息，并等待回复消息收到后返回。或者超时抛出异常返回。
     * 在ChannelHandler中要调用ctx.pipeline().fireUserEventTriggered(SyncTcpClient.newReadOkEvent(received msg));这里才能识别出回复消息received msg。
     * @param msg 请求消息
     * @param time 超时时间，时间单位为毫秒
     * @return
     * @throws Exception
     */
    V send(Object msg,long time) throws Exception; //client

    /**
     * 使用message 创建新的收到回复事件。
     * @param message 接受到的回复消息
     * @return
     */
    Object newReadOkEvent(V message);

    /**
     * 使用cause 创建新的收到回复事件。在无法接受到需要的回复消息时使用。
     * @param cause 接受异常
     * @return
     */
    Object newReadOkEvent(Throwable cause);
}
