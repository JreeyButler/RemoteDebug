package cc.dipperx.debug.remote.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cc.dipperx.debug.remote.BuildConfig;

/**
 * @author Dipper
 * @date 2022/7/3 18:59
 */
public class ExecutorServiceBuilder {
    private int poolSize;
    private int maxQueueSize;
    private String threadName;
    private RejectedExecutionHandler executionHandler;
    private boolean allowCoreThreadTimeOut;
    private int keepLiveTime;

    /**
     * 线程池并发线程数
     *
     * @param poolSize 并发线程数
     * @return 线程池Builder
     */
    public ExecutorServiceBuilder poolSize(int poolSize) {
        this.poolSize = poolSize;
        return this;
    }

    /**
     * 线程池等待队列长度
     *
     * @param maxQueueSize 等待队列长度
     * @return 线程池Builder
     */
    public ExecutorServiceBuilder maxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    /**
     * 线程池名称
     *
     * @param threadName 线程池名称
     * @return 线程池Builder
     */
    public ExecutorServiceBuilder threadName(String threadName) {
        this.threadName = threadName;
        return this;
    }

    /**
     * 线程池策略
     *
     * @param executionHandler 线程池策略
     * @return 线程池Builder
     */
    public ExecutorServiceBuilder executionHandler(RejectedExecutionHandler executionHandler) {
        // 默认策略为：放弃队列中最老的任务
        this.executionHandler = executionHandler;
        return this;
    }

    /**
     * 设置核心线程池是否允许超时（影响线程池是否可以自动关闭）
     *
     * @param allowCoreThreadTimeOut true:当corePoolSize小等于0时，线程池将会在keepLiveTime后自动关闭线程池，
     *                               false:默认值，当前设置的corePoolSize最小为1，所以线程池将会一直存活并保持等待任务状态
     * @return 线程池Builder 实例
     */
    public ExecutorServiceBuilder allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
        return this;
    }

    /**
     * 设置线程池保持时间
     *
     * @param seconds 保持时间（单位：s）
     * @return 线程池Builder 实例
     */
    public ExecutorServiceBuilder keepLiveTime(int seconds) {
        this.keepLiveTime = seconds;
        return this;
    }

    public ExecutorService build() {
        poolSize = Math.max(poolSize, 1);
        maxQueueSize = Math.max(maxQueueSize, 1);
        threadName = threadName == null || "".equals(threadName) ? BuildConfig.APPLICATION_ID : threadName;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                Math.max(10, keepLiveTime),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(maxQueueSize),
                new MyThreadFactory(threadName, poolSize));
        executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        executionHandler = executionHandler == null ?
                new ThreadPoolExecutor.DiscardOldestPolicy() : executionHandler;
        executor.setRejectedExecutionHandler(executionHandler);
        return executor;
    }

    public ScheduledExecutorService buildScheduledThreadPool() {
        return new ScheduledThreadPoolExecutor(poolSize, new MyThreadFactory(threadName, poolSize));
    }

    private static class MyThreadFactory implements ThreadFactory {
        private static final String TAG = "MyThreadFactory";
        private final String threadName;
        private final AtomicInteger count;
        private final int maxThreadNumber;

        MyThreadFactory(String threadName, int maxThreadNumber) {
            this.threadName = threadName;
            this.maxThreadNumber = maxThreadNumber;
            count = new AtomicInteger(0);
        }

        @Override
        public Thread newThread(Runnable r) {
            int i = count.incrementAndGet();

            if (i > maxThreadNumber) {
                count.decrementAndGet();
                return null;
            }
            return new Thread(r, threadName + "-" + i);
        }
    }
}
