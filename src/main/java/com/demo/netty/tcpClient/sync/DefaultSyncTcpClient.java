package com.demo.netty.tcpClient.sync;

import com.demo.netty.tcpClient.SimpleReusePromise;
import com.demo.netty.tcpClient.TcpClientConfig;
import io.netty.channel.*;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 同步 请求-回复 通信客户端
 * @param <V>
 */
public class DefaultSyncTcpClient<V> implements SyncTcpClient<V> {
    private static final Logger logger = Logger.getLogger(DefaultSyncTcpClient.class.getSimpleName());

    private TcpClientConfig tcpClientConfig;
    private TcpClientPool tcpClientPool;
    private SimpleReusePromise<V> promise;

    private static final int CREATE = 0;
    private static final int OPENED = 1;
    private static final int CLOSED = -1;
    private volatile int state ;

    public DefaultSyncTcpClient(){
        tcpClientPool = new TcpClientPool();
        state = CREATE;
    }
    public DefaultSyncTcpClient(TcpClientConfig config, GenericKeyedObjectPoolConfig poolConfig){
        tcpClientPool = new TcpClientPool(poolConfig);
        state = CREATE;
    }

    public void open(TcpClientConfig config){
        if(state != CREATE) throw new RuntimeException("Channel has opened or closed!");
        tcpClientConfig = config;
        init();
        state = OPENED;
    }
    public V send(Object msg) throws Exception{
        return send(msg,-1);
    }
    public V send(Object msg,long time) throws Exception{
        if(state != OPENED) throw new RuntimeException("Channel have not opened!");
        try {
            final Channel channel = tcpClientPool.borrowObject(tcpClientConfig);
            Promise newPromise  = new DefaultPromise<V>(channel.eventLoop());
            newPromise.addListener(new GenericFutureListener<Future<? super V>>() {
                public void operationComplete(Future<? super V> future) throws Exception {
                    tcpClientPool.returnObject(tcpClientConfig,channel);
                }
            });
            if(!promise.repeat(newPromise)){
                throw new RuntimeException("[code] promise repeat error.");
            }
            ChannelFuture f = channel.writeAndFlush(msg);
            f.addListener(new GenericFutureListener<Future<? super Void>>() {
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if(future.isSuccess()){
                        logger.info(channel.toString() + " : send success");
                    }else {
                        logger.info(channel.toString() + " : send error " + future.cause().toString());
                        promise.tryFailure(future.cause());
                    }
                }
            });
            if(time == -1)
                return promise.get();
            else
                return promise.get(time,TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw e;
        }
    }
    public void close() {
        if(state != OPENED) throw new RuntimeException("Channel have not opened!");
        tcpClientPool.close();
        state = CLOSED;
    }

    private void init(){
        addSyncHandle();
    }

    private void addSyncHandle(){
        promise = new SimpleReusePromise<V>();
        this.tcpClientConfig.getHandlerFactories().add(0, new TcpClientConfig.ChannelHandlerFactory() {
            public ChannelHandler newChannelHandler() {
                return new ChannelHandlerAdapter(){
                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        if(evt instanceof MessageReadEvent){
                            if(((MessageReadEvent) evt).isSuccess())
                                promise.trySuccess((V) ((MessageReadEvent) evt).getMessage());
                            else
                                promise.tryFailure(((MessageReadEvent) evt).getCause());
                        }
                        ctx.fireUserEventTriggered(evt);
                    }
                };
            }
        });
    }

    public MessageReadEvent<V> newReadOkEvent(V message){
        DefaultSyncTcpClient.MessageReadEvent event = new MessageReadEvent<V>();
        event.setMessage(message);
        return event;
    }

    public MessageReadEvent<V> newReadOkEvent(Throwable cause){
        DefaultSyncTcpClient.MessageReadEvent event = new MessageReadEvent<V>();
        event.setCause(cause);
        return event;
    }
    public class MessageReadEvent<V> {
        private Throwable cause;
        private V message;
        private boolean success;

        public boolean isSuccess() {
            return success;
        }

        public V getMessage() {
            return message;
        }

        public void setMessage(V message) {
            success = true;
            this.message = message;
        }

        public Throwable getCause() {
            return cause;
        }

        public void setCause(Throwable cause) {
            success = false;
            this.cause = cause;
        }
    }
}
