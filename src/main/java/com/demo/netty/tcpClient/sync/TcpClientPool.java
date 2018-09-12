package com.demo.netty.tcpClient.sync;

import com.demo.netty.tcpClient.TcpClientConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.SystemPropertyUtil;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @see NioSocketChannel 对象池
 */
public class TcpClientPool extends GenericKeyedObjectPool<TcpClientConfig,Channel>{
    private static Logger logger = Logger.getLogger(TcpClientPool.class.getSimpleName());
    private EventLoopGroup workerGroup;
    private static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2));
    }
    public TcpClientPool() {
        super(ChannelFactory.INSTANCE);
        workerGroup = new NioEventLoopGroup();
        ChannelFactory.INSTANCE.workerGroup = workerGroup;
    }

    public TcpClientPool(GenericKeyedObjectPoolConfig config) {
        super(ChannelFactory.INSTANCE, config);
        workerGroup = new NioEventLoopGroup(Math.max(config.getMaxTotal() , DEFAULT_EVENT_LOOP_THREADS ) );
        ChannelFactory.INSTANCE.workerGroup = workerGroup;
    }

    @Override
    public void close() {
        super.close();
        workerGroup.shutdownGracefully();
    }


    private static class ChannelFactory extends BaseKeyedPooledObjectFactory<TcpClientConfig, Channel>{
        private static final ChannelFactory INSTANCE = new ChannelFactory();
        private EventLoopGroup workerGroup;

        public Channel create(TcpClientConfig key) throws Exception {
            try {
                final List<TcpClientConfig.ChannelHandlerFactory> handlerFactories = key.getHandlerFactories();

                Bootstrap b = new Bootstrap(); // (1)
                b.group(workerGroup); // (2)
                b.channel(NioSocketChannel.class); // (3)
                //b.option(ChannelOption.SO_KEEPALIVE, true); // (4)

                Map<ChannelOption<?>, Object> options = key.options();
                synchronized (options) {
                    for (Map.Entry<ChannelOption<?>, Object> e: options.entrySet()) {
                        b.option((ChannelOption<Object>) e.getKey(), e.getValue());
                    }
                }

                final Map<AttributeKey<?>, Object> attrs = key.attrs();
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
                ChannelFuture f = b.connect(key.getIp(), key.getPort()).sync(); // (5)

                f.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        //TODO 需要中断重连？
                    }
                });

                return f.channel();
            } finally {

            }
        }

        public PooledObject<Channel> wrap(Channel value) {
            return  new DefaultPooledObject<Channel>(value);
        }

        @Override
        public void destroyObject(TcpClientConfig key, PooledObject<Channel> p) throws Exception {
            Channel channel = p.getObject();
            if (channel.isOpen()) {
                channel.close();
            }
        }
    }
}
