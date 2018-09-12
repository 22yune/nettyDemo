package com.demo.netty.tcpClient;

import io.netty.util.concurrent.Promise;

/**
 * //比较特殊，重复的许诺不是一个正常的许诺。许诺的接口意义都变了。
 * @param <V>
 */
public interface RepeatPromise<V> extends Promise<V> {
    /**
     *  重复新的许诺
     * @param promise
     * @return 当前许诺已失败或为空，且未失信时，可以重复许诺，返回true；否则返回false。
     */
     boolean repeat(Promise<V> promise);

    /**
     * 返回当前的许诺
     */
     Promise<V> nowPromise();

    /**
     * 失信 失信后不能再重复许诺了
     * @return
     */
     void breakFaith();

    /**
     * 返回是否已失信
     */
    boolean isBreakFaith();
}
