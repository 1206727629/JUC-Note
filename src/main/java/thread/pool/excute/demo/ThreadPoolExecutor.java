package thread.pool.excute.demo;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author yangwentian5
 * @Date 2022/3/22 21:47
 */
/**
 * （1）线程池的状态和工作线程的数量共同保存在控制变量ctl中，类似于AQS中的state变量，不过这里是直接使用的AtomicInteger，这里换成unsafe+volatile也是可以的；
 * （2）ctl的高三位保存运行状态，低29位保存工作线程的数量，也就是说线程的数量最多只能有(2^29-1)个，也就是上面的CAPACITY；
 * （3）线程池的状态一共有五种，分别是RUNNING、SHUTDOWN、STOP、TIDYING、TERMINATED；
 * （4）RUNNING，表示可接受新任务，且可执行队列中的任务；
 * （5）SHUTDOWN，表示不接受新任务，但可执行队列中的任务；
 * （6）STOP，表示不接受新任务，且不再执行队列中的任务，且中断正在执行的任务；
 * （7）TIDYING，所有任务已经中止，且工作线程数量为0，最后变迁到这个状态的线程将要执行terminated()钩子方法，只会有一个线程执行这个方法；
 * （8）TERMINATED，中止状态，已经执行完terminated()钩子方法；
 */
public class ThreadPoolExecutor {
    /**
     * (1)RUNNING
     * 比较简单，创建线程池的时候就会初始化ctl，而ctl初始化为RUNNING状态，所以线程池的初始状态就为RUNNING状态
     * 初始状态为RUNNING
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3; // =29
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1; // =000 11111...

    // runState is stored in the high-order bits
    private static final int RUNNING    = -1 << COUNT_BITS; // 111 00000...
    private static final int SHUTDOWN   =  0 << COUNT_BITS; // 000 00000...
    private static final int STOP       =  1 << COUNT_BITS; // 001 00000...
    private static final int TIDYING    =  2 << COUNT_BITS; // 010 00000...
    private static final int TERMINATED =  3 << COUNT_BITS; // 011 00000...

    // 线程池的状态
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    // 线程池中工作线程的数量
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    // 计算ctl的值，等于运行状态“加上”线程数量
    private static int ctlOf(int rs, int wc) { return rs | wc; }
    /**
     * --------------------------------------------------------------
     *  （1）新建线程池时，它的初始状态为RUNNING，这个在上面定义ctl的时候可以看到；
         (2）RUNNING->SHUTDOWN，执行shutdown()方法时；
        （3）RUNNING->STOP，执行shutdownNow()方法时；
        （4）SHUTDOWN->STOP，执行shutdownNow()方法时
        （5）STOP->TIDYING，执行了shutdown()或者shutdownNow()后，所有任务已中止，且工作线程数量为0时，此时会执行terminated()方法；
        （6）TIDYING->TERMINATED，执行完terminated()方法后；
     */

    /**
     * （2）SHUTDOWN
     *  执行shutdown()方法时把状态修改为SHUTDOWN，这里肯定会成功，因为advanceRunState()方法中是个自旋，不成功不会退出。
     */
//    public void shutdown() {
//        final ReentrantLock mainLock = this.mainLock;
//        mainLock.lock();
//        try {
//            checkShutdownAccess();
//            // 修改状态为SHUTDOWN
//            advanceRunState(SHUTDOWN);
//            // 标记空闲线程为中断状态
//            interruptIdleWorkers();
//            onShutdown();
//        } finally {
//            mainLock.unlock();
//        }
//        tryTerminate();
//    }
//
//    private void advanceRunState(int targetState) {
//        for (;;) {
//            int c = ctl.get();
//            // 如果状态大于SHUTDOWN，或者修改为SHUTDOWN成功了，才会break跳出自旋
//            if (runStateAtLeast(c, targetState) ||
//                    ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
//                break;
//        }
//    }

    /**
     * （3）STOP
     *  执行shutdownNow()方法时，会把线程池状态修改为STOP状态，同时标记所有线程为中断状态。
     */
//    public List<Runnable> shutdownNow() {
//        List<Runnable> tasks;
//        final ReentrantLock mainLock = this.mainLock;
//        mainLock.lock();
//        try {
//            checkShutdownAccess();
//            // 修改为STOP状态
//            advanceRunState(STOP);
//            // 标记所有线程为中断状态
//            interruptWorkers();
//            tasks = drainQueue();
//        } finally {
//            // 【本文由公从号“彤哥读源码”原创】
//            mainLock.unlock();
//        }
//        tryTerminate();
//        return tasks;
//    }

    /**
     * 至于线程是否响应中断其实是在队列的take()或poll()方法中响应的，最后会到AQS中，
     * 它们检测到线程中断了会抛出一个InterruptedException异常，然后getTask()中捕获这个异常，并且在下一次的自旋时退出当前线程并减少工作线程的数量。
     *
     *  这里有一个问题，就是已经通过getTask()取出来且返回的任务怎么办？
     *  实际上它们会正常执行完毕，有兴趣的同学可以自己看看runWorker()这个方法，我们下一节会分析这个方法。
     * @return
     */
//    private Runnable getTask() {
//        boolean timedOut = false; // Did the last poll() time out?
//
//        for (;;) {
//            int c = ctl.get();
//            int rs = runStateOf(c);
//
//            // 如果状态为STOP了，这里会直接退出循环，且减少工作线程数量
//            // 退出循环了也就相当于这个线程的生命周期结束了
//            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
//                decrementWorkerCount();
//                return null;
//            }
//
//            int wc = workerCountOf(c);
//
//            // Are workers subject to culling?
//            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
//
//            if ((wc > maximumPoolSize || (timed && timedOut))
//                    && (wc > 1 || workQueue.isEmpty())) {
//                if (compareAndDecrementWorkerCount(c))
//                    return null;
//                continue;
//            }
//            已经通过getTask()取出来且返回的，实际上它们会正常执行完毕
//            try {
//                // 真正响应中断是在poll()方法或者take()方法中
//                Runnable r = timed ?
//                        workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
//                        workQueue.take();
//                if (r != null)
//                    return r;
//                timedOut = true;
//            } catch (InterruptedException retry) {
//                // 这里捕获中断异常
//                timedOut = false;
//            }
//        }
//    }


    /**
     * （4）TIDYING
     *  当执行shutdown()或shutdownNow()之后，如果所有任务已中止，且工作线程数量为0，就会进入这个状态。
     *
     *  实际更新状态为TIDYING和TERMINATED状态的代码都在tryTerminate()方法中，实际上tryTerminated()方法在很多地方都有调用，
     *  比如shutdown()、shutdownNow()、线程退出时，所以说几乎每个线程最后消亡的时候都会调用tryTerminate()方法，但最后只会有一个线程真正执行到修改状态为TIDYING的地方。
     *  修改状态为TIDYING后执行terminated()方法，最后修改状态为TERMINATED，标志着线程池真正消亡了。
     */
//    final void tryTerminate() {
//        for (;;) {
//            int c = ctl.get();
//            // 下面几种情况不会执行后续代码
//            // 1. 运行中
//            // 2. 状态的值比TIDYING还大，也就是TERMINATED
//            // 3. SHUTDOWN状态且任务队列不为空
//            if (isRunning(c) ||
//                    runStateAtLeast(c, TIDYING) ||
//                    (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
//                return;
//            // 工作线程数量不为0，也不会执行后续代码
//            if (workerCountOf(c) != 0) {
//                // 尝试中断空闲的线程
//                interruptIdleWorkers(ONLY_ONE);
//                return;
//            }
//
//            final ReentrantLock mainLock = this.mainLock;
//            mainLock.lock();
//            try {
//                // CAS修改状态为TIDYING状态
//                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
//                    try {
//                        // 更新成功，执行terminated钩子方法
//                        terminated();
//                    } finally {
//                        // 强制更新状态为TERMINATED，这里不需要CAS了
//                        ctl.set(ctlOf(TERMINATED, 0));
//                        termination.signalAll();
//                    }
//                    return;
//                }
//            } finally {
//                mainLock.unlock();
//            }
//            // else retry on failed CAS
//        }
//    }

    /**
     * （5）TERMINATED
     */
}
