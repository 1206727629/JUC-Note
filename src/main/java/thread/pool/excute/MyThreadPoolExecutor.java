package thread.pool.excute;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author yangwentian5
 * @Date 2022/2/22 18:24
 * 创建线程的时候要时刻警惕并发的陷阱
 * 我们定义一个线程池一共需要这么四个变量：核心线程数coreSize、最大线程数maxSize、阻塞队列BlockingQueue、拒绝策略RejectPolicy
 */
public class MyThreadPoolExecutor implements Executor {
    /**
     * 当前正在运行的线程数
     * 需要修改时线程间立即感知，所以使用AtomicInteger
     * 或者也可以使用volatile并结合Unsafe做CAS操作（参考Unsafe篇章讲解）
     */
    private volatile AtomicInteger runningCount = new AtomicInteger(0);

    /**
     * 线程池的名称
     */
    private String name;
    /**
     * 线程序列号
     */
    private AtomicInteger sequence = new AtomicInteger(0);
    /**
     * 核心线程数
     */
    private int coreSize;
    /**
     * 最大线程数
     */
    private int maxSize;
    /**
     * 任务队列
     * 这个队列必须是阻塞队列
     * ConcurrentLinkedQueue不是阻塞队列，不能运用在jdk的线程池中
     */
    private BlockingQueue<Runnable> taskQueue;
    /**
     * 拒绝策略
     * 默认的拒绝策略是抛出异常
     */
    private DiscardRejectPolicy discardRejectPolicy;

    /**
     * 线程保持空闲时间及单位。
     * 默认情况下，此两参数仅当正在运行的线程数大于核心线程数时才有效，即只针对非核心线程。
     * 但是，如果allowCoreThreadTimeOut被设置成了true，针对核心线程也有效。
     * 即当任务队列为空时，线程保持多久才会销毁，内部主要是通过阻塞队列带超时的poll(timeout, unit)方法实现的。
     * keepAliveTime + unit
     */

    /**
     * 线程工厂默认使用的是Executors工具类中的DefaultThreadFactory类，
     * 这个类有个缺点，创建的线程的名称是自动生成的，无法自定义以区分不同的线程池，
     * 且它们都是非守护线程
     * threadFactory
     */

    public MyThreadPoolExecutor(String name, int coreSize, int maxSize, BlockingQueue<Runnable> taskQueue, DiscardRejectPolicy discardRejectPolicy) {
        this.name = name;
        this.coreSize = coreSize;
        this.maxSize = maxSize;
        this.taskQueue = taskQueue;
        this.discardRejectPolicy = discardRejectPolicy;
    }

    public static void main(String[] args) {
        Executor threadPool = new MyThreadPoolExecutor("test", 5, 10, new ArrayBlockingQueue<>(15), new DiscardRejectPolicy());
        AtomicInteger num = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            threadPool.execute(()->{
                try {
                    Thread.sleep(1000);
                    int j = 10 / 0;
                    System.out.println("running: " + System.currentTimeMillis() + ": " + num.incrementAndGet());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void execute(Runnable task) {
        // 正在运行的线程数
        int count = runningCount.get();
        // 如果正在运行的线程数小于核心线程数，直接加一个线程
        if (count < coreSize) {
            // 注意，这里不一定添加成功，addWorker()方法里面还要判断一次是不是确实小
            // 同时addWorker里面通过自旋锁不断地从队列中获取任务
            if (addWorker(task, true)) {
                return;
            }
            // 如果添加核心线程失败，进入下面的逻辑
        }

        // 如果达到了核心线程数，先尝试让任务入队
        // 这里之所以使用offer()，是因为如果队列满了offer()会立即返回false
        if (taskQueue.offer(task)) {
            // do nothing，为了逻辑清晰这里留个空if
            // 【本篇文章由公众号“彤哥读源码”原创】
        } else {
            // 如果入队失败，说明队列满了，那就添加一个非核心线程
            // 非核心线程池false，若是线程池最大线程都运行不来，addWorker则返回false
            if (!addWorker(task, false)) {
                // 如果添加非核心线程失败了，那就执行拒绝策略
                // 直接同步执行
                discardRejectPolicy.reject(task, this);
            }
        }
    }

    /**
     * 不管核心线程池还是非核心线程池，执行任务成功返回true；
     * 若是满了则需要执行拒绝策略
     * @param newTask
     * @param core
     * @return
     */
    private boolean addWorker(Runnable newTask, boolean core) {
        // 自旋判断是不是真的可以创建一个线程
        // 可能CAS判断失败表示其它线程先修改了runningCount的值，那么自旋重试
        for (; ; ) {
            // 正在运行的线程数
            int count = runningCount.get();
            // 入参core表示是否超过了核心线程，核心线程为true，非核心线程为false
            // 核心线程还是非核心线程
            int max = core ? coreSize : maxSize;
            // 不满足创建线程的条件，直接返回false
            if (count >= max) {
                return false;
            }
            // 使用乐观锁比较。修改runningCount成功，可以创建线程
            if (runningCount.compareAndSet(count, count + 1)) {
                // 线程的名字
                String threadName = (core ? "core_" : "") + name + sequence.incrementAndGet();
                // 创建线程并启动
                new Thread(() -> {
                    System.out.println("thread name: " + Thread.currentThread().getName());
                    // 运行的任务
                    Runnable task = newTask;
                    // 不断从任务队列中取任务执行，如果取出来的任务为null，则跳出循环，线程也就结束了
                    // 下面的finally虽然置为null，但若是还是可以从队列里取得任务，那么线程则继续执行
                    while (task != null || (task = getTask()) != null) {
                        try {
                            // 执行任务
                            task.run();
                        } finally {
                            // 任务执行完成，置为空
                            task = null;
                        }
                    }
                }, threadName).start();

                break;
            }
        }

        return true;
    }

    /**
     * 从阻塞队列里获取任务
     * @return
     */
    private Runnable getTask() {
        try {
            // take()方法会一直阻塞直到取到任务为止
            return taskQueue.take();
        } catch (InterruptedException e) {
            // 线程中断了，返回null可以结束当前线程
            // 当前线程都要结束了，理应要把runningCount的数量减一
            runningCount.decrementAndGet();
            return null;
        }
    }
}
