package com.demo.netty.tcpClient;

import io.netty.util.concurrent.Promise;

public class SimpleReusePromise<V> extends SimpleRepeatPromise<V> {
    protected boolean checkRepeat(Promise<V> nowPromise, Promise<V> newPromise){
        if(!nowPromise.isDone()){
            return false;
        }
        return true;
    }
}
