package com.demo.netty;

import com.demo.netty.tcpClient.DefaultTcpClientConfig;
import com.demo.netty.tcpClient.async.ReconnectAsyncTcpClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncClient {

    private static ReconnectAsyncTcpClient channel;
    @BeforeClass
    public static void before(){
        DefaultTcpClientConfig channelConfig = new DefaultTcpClientConfig();
        channelConfig.setIp("localhost");
        channelConfig.setPort(8086);
        channelConfig.addHandler(StringEncoder.class);
        channelConfig.addHandler(StringDecoder.class);
        channelConfig.addHandler(AsyncClient.SyncClientHandler.class);

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
        String send = "send1";
        channel.send(send);
    }
    @Test
    public void send3(){
        final CountDownLatch count = new CountDownLatch(100);
        final ExecutorService executorService = Executors.newFixedThreadPool(8);
        for(int i = 0; i < 100; i++){
            final String send = "send "+i;
            final Runnable runnable = new Runnable() {
                private Runnable inner;
                public void run() {
                    inner = this;
                    ChannelFuture future = channel.channel().writeAndFlush(send);
                    future.addListener(new GenericFutureListener<Future<? super Void>>() {
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            if(!future.isSuccess()){
                                executorService.submit(inner);
                            }else {
                                count.countDown();
                            }
                        }
                    });
                }
            };

            executorService.submit(runnable);
        }
        try {
            count.await();
            executorService.shutdown();
            executorService.awaitTermination(30,TimeUnit.SECONDS);
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
