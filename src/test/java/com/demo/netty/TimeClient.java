package com.demo.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.Date;


public class TimeClient {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8086;
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap(); // (1)
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new TimeClientHandler());
                }
            });
            // 启动客户端
            ChannelFuture f = b.connect(host, port).sync(); // (5)

            // 等待连接关闭
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static class TimeClientHandler extends ChannelHandlerAdapter {
        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            final ByteBuf msg = ctx.alloc().buffer(4);
            msg.writeBytes("aaaaa".getBytes());
            ChannelFuture f = ctx.writeAndFlush(msg);
            f.addListener(new GenericFutureListener<Future<? super Void>>() {
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if(future.isSuccess()){
                        System.out.println("request success");
                    }
                }
            });
        }
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf m = (ByteBuf) msg; // (1)
            try {
                String a = m.toString(io.netty.util.CharsetUtil.US_ASCII);
                System.out.println(a);
                ctx.close();
            } finally {
                m.release();
            }
        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
