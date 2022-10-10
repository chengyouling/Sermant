package com.huawei.discovery.factory;

import java.util.concurrent.ThreadFactory;

public class RealmServiceThreadFactory implements ThreadFactory {

    private final String threadName;

    /**
     * 流控线程工厂
     *
     * @param threadName 线程名称
     */
    public RealmServiceThreadFactory(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        return new Thread(runnable, threadName);
    }
}
