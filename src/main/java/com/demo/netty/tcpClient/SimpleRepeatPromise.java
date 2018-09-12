package com.demo.netty.tcpClient;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleRepeatPromise<V> implements RepeatPromise<V> {
    private volatile Promise<V> nowPromise;
    private volatile List<GenericFutureListener> listeners;
    private volatile boolean breakFaith = false;

    public boolean isBreakFaith() {
        return breakFaith;
    }

    public synchronized void breakFaith() {
        this.breakFaith = true;
    }

    public Promise<V> nowPromise() {
        return nowPromise;
    }

    protected boolean checkRepeat(Promise<V> nowPromise, Promise<V> newPromise){
        if(!nowPromise.isDone() || nowPromise.isSuccess()){
            return false;
        }
        return true;
    }
    public boolean repeat(Promise<V> promise) {
        if(isBreakFaith()){
            return false;
        }
        //当前许诺未完成或者已成功时，不能重复许诺。
        if(this.nowPromise != null && !checkRepeat(nowPromise,promise)){
            return false;
        }
        //只对breakFaith、listeners和promise做一致性保证。即保证listeners一定设置到promise上，breakFaith后就不能再重复许诺。
        synchronized (this){
            if(isBreakFaith()){
                return false;
            }
            if(!promise.equals(this.nowPromise)){
                this.nowPromise = promise;
                if(listeners != null){
                    for(GenericFutureListener listener : listeners){
                        this.nowPromise.addListener(listener);
                    }
                }
            }
            return true;
        }
    }

    public Promise<V> setSuccess(V result) {
        return nowPromise.setSuccess(result);
    }

    public boolean trySuccess(V result) {
        return nowPromise.trySuccess(result);
    }

    public Promise<V> setFailure(Throwable cause) {
        return nowPromise.setFailure(cause);
    }

    public boolean tryFailure(Throwable cause) {
        return nowPromise.tryFailure(cause);
    }

    public boolean setUncancellable() {
        return nowPromise.setUncancellable();
    }

    public boolean isSuccess() {
        return nowPromise.isSuccess();
    }

    public boolean isCancellable() {
        return nowPromise.isCancellable();
    }

    public Throwable cause() {
        return nowPromise.cause();
    }

    public synchronized Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        if(listeners == null) listeners = new ArrayList<GenericFutureListener>();
        listeners.add(listener);
        return nowPromise.addListener(listener);
    }

    public Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        if (listeners == null) {
            throw new NullPointerException("listeners");
        }

        for (GenericFutureListener<? extends Future<? super V>> l: listeners) {
            if (l == null) {
                break;
            }
            addListener(l);
        }
        return this;
    }

    public synchronized Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        if(listeners != null) listeners.remove(listener);
        return nowPromise.removeListener(listener);
    }

    public Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        if (listeners == null) {
            throw new NullPointerException("listeners");
        }

        for (GenericFutureListener<? extends Future<? super V>> l: listeners) {
            if (l == null) {
                break;
            }
            removeListener(l);
        }
        return this;
    }

    public Promise<V> await() throws InterruptedException {
        return nowPromise.await();
    }

    public Promise<V> awaitUninterruptibly() {
        return nowPromise.awaitUninterruptibly();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return nowPromise.await(timeout,unit);
    }

    public boolean await(long timeoutMillis) throws InterruptedException {
        return nowPromise.await(timeoutMillis);
    }

    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return nowPromise.awaitUninterruptibly(timeout,unit);
    }

    public boolean awaitUninterruptibly(long timeoutMillis) {
        return nowPromise.awaitUninterruptibly(timeoutMillis);
    }

    public V getNow() {
        return nowPromise.getNow();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {//TODO
        return nowPromise.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return nowPromise.isCancelled();
    }

    public boolean isDone() {
        return nowPromise.isDone();
    }

    public V get() throws InterruptedException, ExecutionException {
        return nowPromise.get();
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return nowPromise.get(timeout,unit);
    }

    public Promise<V> sync() throws InterruptedException {
        return nowPromise.sync();
    }

    public Promise<V> syncUninterruptibly() {
        return nowPromise.syncUninterruptibly();
    }
}
