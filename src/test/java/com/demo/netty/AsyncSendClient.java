package com.demo.netty;

import com.demo.netty.tcpClient.DefaultTcpClientConfig;
import com.demo.netty.tcpClient.RepeatPromise;
import com.demo.netty.tcpClient.async.ReconnectAsyncTcpClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncSendClient {

    private static ReconnectAsyncTcpClient channel;
    @BeforeClass
    public static void before(){
        DefaultTcpClientConfig channelConfig = new DefaultTcpClientConfig();
        channelConfig.setIp("localhost");
        channelConfig.setPort(8086);
        channelConfig.addHandler(StringEncoder.class);
        channelConfig.addHandler(StringDecoder.class);
        channelConfig.addHandler(AsyncSendClient.SyncClientHandler.class);

        channel = new ReconnectAsyncTcpClient();
        try {
            channel.open(channelConfig);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Test
    public void send1(){
        final CountDownLatch count = new CountDownLatch(1);
        final String send = "send1";
        RepeatPromise<Void> promise = channel.send(send);
        promise.addListener(new GenericFutureListener<Future<? super Void>>() {
            public void operationComplete(Future<? super Void> future) throws Exception {
                if(future.isSuccess()){
                    System.out.println("success test " + send);
                    count.countDown();
                }
            }
        });
        try {
            if(!count.await(10,TimeUnit.SECONDS)){
                promise.breakFaith();
                promise.addListener(new GenericFutureListener<Future<? super Void>>() {
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        if (promise.isBreakFaith()){
                            System.out.println("breakFaith test " + send);
                            count.countDown();
                        }
                    }
                });
            }
        } catch (InterruptedException e) {

        }
        try {
            count.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
   // @Test
    public void send3(){
        final CountDownLatch count = new CountDownLatch(10);
        List<RepeatPromise<Void>> promises = new ArrayList<RepeatPromise<Void>>();
        for(int i = 0; i < 10; i++){
            final String send = "send "+i;
            RepeatPromise<Void> promise = channel.send(send);
            promises.add(promise);
            promise.addListener(new GenericFutureListener<Future<? super Void>>() {
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if(future.isSuccess()){
                        System.out.println("test " + send);
                        count.countDown();
                    } else if (promise.isBreakFaith()){
                        System.out.println("breakFaith test " + send);
                        count.countDown();
                    }
                }
            });
        }
        try {
            if(!count.await(10,TimeUnit.SECONDS)){
                for(RepeatPromise promise : promises){
                    promise.breakFaith();
                }
            }
        } catch (InterruptedException e) {

        }
        try {
            count.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void after(){
        channel.close();
    }


    public static class SyncClientHandler extends ChannelHandlerAdapter {
        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            try {
            //    channel.send("test async");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            System.out.println(msg);
        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
