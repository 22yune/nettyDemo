package com.demo.netty.tcpClient;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultTcpClientConfig implements TcpClientConfig {
    private String ip;
    private int port;
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();
    private final Map<AttributeKey<?>, Object> attrs = new LinkedHashMap<AttributeKey<?>, Object>();
    private List<ChannelHandlerFactory> handlerFactories;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public <T> TcpClientConfig option(ChannelOption<T> option, T value) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        if (value == null) {
            synchronized (options) {
                options.remove(option);
            }
        } else {
            synchronized (options) {
                options.put(option, value);
            }
        }
        return this;
    }

    public Map<ChannelOption<?>, Object> options() {
        return options;
    }

    public <T> TcpClientConfig attr(AttributeKey<T> key, T value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            synchronized (attrs) {
                attrs.remove(key);
            }
        } else {
            synchronized (attrs) {
                attrs.put(key, value);
            }
        }
        return this;
    }

    public Map<AttributeKey<?>, Object> attrs() {
        return attrs;
    }


    public void addHandler(Class channelHandlerClass){
        if(handlerFactories == null) handlerFactories = new ArrayList<ChannelHandlerFactory>();
        handlerFactories.add(new ClassChannelHandlerFactory(channelHandlerClass));
    }
    public void addHandler(ChannelHandler channelHandler){
        if(handlerFactories == null) handlerFactories = new ArrayList<ChannelHandlerFactory>();
        handlerFactories.add(new ObjectChannelHandlerFactory(channelHandler));
    }

    public List<ChannelHandlerFactory> getHandlerFactories() {
        return handlerFactories;
    }

    public void setHandlerFactories(List<ChannelHandlerFactory> handlerFactories) {
        this.handlerFactories = handlerFactories;
    }

    private static class ObjectChannelHandlerFactory implements ChannelHandlerFactory{
        private ChannelHandler channelHandler;
        public ObjectChannelHandlerFactory(ChannelHandler channelHandler){
            this.channelHandler = channelHandler;
        }
        public ChannelHandler newChannelHandler() {
            return channelHandler;
        }
    }
    private static class ClassChannelHandlerFactory implements ChannelHandlerFactory{
        private Class channelHandlerClass;
        public ClassChannelHandlerFactory(Class channelHandlerClass){
            this.channelHandlerClass = channelHandlerClass;
        }
        public ChannelHandler newChannelHandler() {
            try {
                return (ChannelHandler) channelHandlerClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
