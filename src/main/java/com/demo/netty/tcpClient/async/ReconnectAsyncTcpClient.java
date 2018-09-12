package com.demo.netty.tcpClient.async;

import com.demo.netty.tcpClient.RepeatPromise;
import com.demo.netty.tcpClient.SimpleReusePromise;
import com.demo.netty.tcpClient.TcpClientConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 可以自动重连的客户端
 * 提供send接口发送消息，消息在发送失败后会自动重试，只到发送成功或取消重试。
 */

public class ReconnectAsyncTcpClient implements AsyncTcpClient{
    private static final Logger logger = Logger.getLogger(ReconnectAsyncTcpClient.class.getSimpleName());
    private EventLoopGroup workerGroup;
    private volatile Channel channel;
    private static final int CREATE = 0;
    private static final int OPENED = 1;
    private static final int CLOSED = -1;
    private volatile int state ;

    public ReconnectAsyncTcpClient(){
        workerGroup = new NioEventLoopGroup(2);
        state = CREATE;
    }

    public void open(final TcpClientConfig tcpClientConfig) throws Exception{
        if(state == CLOSED) throw new RuntimeException("Channel has closed!");
        synchronized (this) {
            try{
                this.channel = create(tcpClientConfig);
                state = OPENED;
                channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                    public void operationComplete(Future<? super Void> future) throws Exception {

                        if(state == CLOSED){
                            logger.info("channel closed.");
                        }else if(state == OPENED){
                            channel.eventLoop().submit(new Runnable() {
                                public void run() {
                                    try {
                                        open(tcpClientConfig);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                if(state == CLOSED){
                    logger.info("channel closed." + e);
                }else if(state == OPENED){
                    channel.eventLoop().submit(new Runnable() {
                        public void run() {
                            try {
                                open(tcpClientConfig);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }else {
                    throw e;
                }
            }
        }
    }
    public Channel create(final TcpClientConfig tcpClientConfig) throws Exception{
        try {
            final List<TcpClientConfig.ChannelHandlerFactory> handlerFactories = tcpClientConfig.getHandlerFactories();

            Bootstrap b = new Bootstrap(); // (1)
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)

            Map<ChannelOption<?>, Object> options = tcpClientConfig.options();
            synchronized (options) {
                for (Map.Entry<ChannelOption<?>, Object> e: options.entrySet()) {
                    b.option((ChannelOption<Object>) e.getKey(), e.getValue());
                }
            }

            final Map<AttributeKey<?>, Object> attrs = tcpClientConfig.attrs();
            synchronized (attrs) {
                for (Map.Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                    b.attr((AttributeKey<Object>) e.getKey(),e.getValue());
                }
            }

            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    if(handlerFactories != null){
                        for(TcpClientConfig.ChannelHandlerFactory factory : handlerFactories){
                            ch.pipeline().addLast(factory.newChannelHandler());
                        }
                    }
                }
            });
            // 启动客户端
            ChannelFuture f = b.connect(tcpClientConfig.getIp(), tcpClientConfig.getPort()).sync();; // (5)

            return f.channel();
        } finally {

        }
    }

    public ChannelFuture close() {
        if(state != OPENED) throw new RuntimeException("Channel is not opened!");
        state = CLOSED;
        workerGroup.shutdownGracefully();
        return channel.close();
    }

    /**
     * 发送消息，消息在发送失败后会自动重试，只到发送成功或取消重试。
     * @param msg 发送的消息
     * @return 发送成功的许诺，提供取消重试接口。
     */
    public RepeatPromise<Void> send(final Object msg) {
        if(state != OPENED) throw new RuntimeException("Channel is not opened!");

        RepeatPromise<Void> promise = new SimpleReusePromise<Void>();
        new Runnable() {
            private RepeatPromise<Void> promise;
            private Runnable inner;

            public Runnable setPromise(RepeatPromise<Void> promise) {
                this.promise = promise;
                return this;
            }

            public void run() {
                inner = this;
                ChannelPromise newPromise = channel.newPromise();
                if(promise.repeat(newPromise)){
                    newPromise.addListener(new GenericFutureListener<Future<? super Void>>() {
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            if(!future.isSuccess()){
                                channel.eventLoop().submit(inner);
                            }
                        }
                    });
                    channel.writeAndFlush(msg,newPromise);
                }
            }
        }.setPromise(promise).run();

        return promise;
    }

    public Channel channel() {
        return channel;
    }

}
