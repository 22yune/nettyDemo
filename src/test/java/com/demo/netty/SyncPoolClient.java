package com.demo.netty;

import com.demo.netty.tcpClient.sync.DefaultSyncTcpClient;
import com.demo.netty.tcpClient.DefaultTcpClientConfig;
import io.netty.channel.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SyncPoolClient {

    private static DefaultSyncTcpClient<String> channel;
    @BeforeClass
    public static void before(){
        DefaultTcpClientConfig channelConfig = new DefaultTcpClientConfig();
        channelConfig.setIp("localhost");
        channelConfig.setPort(8086);
        channelConfig.addHandler(StringEncoder.class);
        channelConfig.addHandler(StringDecoder.class);
        channelConfig.addHandler(SyncClientHandler.class);

        channel = new DefaultSyncTcpClient<String>();
        channel.open(channelConfig);
    }
    @Test
    public void send1(){
        String send = "send1";
        String a = null;
        try {
            a = channel.send(send);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertTrue(send.equals(a));
    }
    @Test
    public void send2(){
        String send = "send2";
        String a = null;
        try {
            a = channel.send(send);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertTrue(send.equals(a));
    }
    @Test
    public void send3(){
        for(int i = 0; i < 1000000; i++){
            final String send = "send3"+i;
            ExecutorService  executorService = Executors.newFixedThreadPool(8);
            executorService.submit(new Runnable() {
                public void run() {
                    String a = null;
                    try {
                        a = channel.send(send);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Assert.assertTrue(send.equals(a));
                }
            });
            executorService.shutdown();
            try {
                executorService.awaitTermination(30,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @AfterClass
    public static void after(){
        channel.close();
    }


    public static class SyncClientHandler extends ChannelHandlerAdapter {
        @Override
        public void channelActive(final ChannelHandlerContext ctx) {

        }
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            System.out.println(msg);

            ctx.pipeline().fireUserEventTriggered(channel.newReadOkEvent((String) msg));

        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.pipeline().fireUserEventTriggered(channel.newReadOkEvent(cause));
            ctx.close();
        }
    }
}
