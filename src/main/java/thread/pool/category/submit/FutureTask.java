package thread.pool.category.submit;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * （1）未来任务是通过把普通任务包装成FutureTask来实现的。
 * （2）通过FutureTask不仅能够获取任务执行的结果，还有感知到任务执行的异常，甚至还可以取消任务；
 * （3）AbstractExecutorService中定义了很多模板方法，这是一种很重要的设计模式；
 * （4）FutureTask其实就是典型的异常调用的实现方式，后面我们学习到Netty、Dubbo的时候还会见到这种设计思想的。
 *
 * RPC框架中异步调用是怎么实现的？
 * 答：RPC框架常用的调用方式有同步调用、异步调用，其实它们本质上都是异步调用，它们就是用FutureTask的方式来实现的。
 * 一般地，通过一个线程（我们叫作远程线程）去调用远程接口，如果是同步调用，则直接让调用者线程阻塞着等待远程线程调用的结果，待结果返回了再返回；
 * 如果是异步调用，则先返回一个未来可以获取到远程结果的东西FutureXxx，当然，如果这个FutureXxx在远程结果返回之前调用了get()方法一样会阻塞着调用者线程。
 *
 * @Author yangwentian5
 * @Date 2022/3/23 14:13
 */
public class FutureTask<V> implements RunnableFuture<V> {

    /** The underlying callable; nulled out after running */
    private Callable<V> callable;

    private volatile int state;
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    private volatile Thread runner;

    /** Treiber stack of waiting threads */
    private volatile WaitNode waiters;

    /** The result to return or exception to throw from get() */
    private Object outcome;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }

    public FutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }

    /**
     * 我们知道execute()方法最后调用的是task的run()方法，上面我们传进去的任务，最后被包装成了FutureTask，
     * 也就是说execute()方法最后会调用到FutureTask的run()方法，所以我们直接看这个方法就可以了
     *
     * 先做状态的检测，再执行任务，最后处理结果或异常
     *
     * 整个run()方法总结下来：
     * （1）FutureTask有一个状态state控制任务的运行过程，正常运行结束state从NEW->COMPLETING->NORMAL，异常运行结束state从NEW->COMPLETING->EXCEPTIONAL；
     * （2）FutureTask保存了运行任务的线程runner，它是线程池中的某个线程；
     * （3）调用者线程是保存在waiters队列中的，它是什么时候设置进去的呢？
     * （4）任务执行完毕，除了设置状态state变化之外，还要唤醒调用者线程。
     */
    public void run() {
        // 状态不为NEW，或者修改为当前线程来运行这个任务失败，则直接返回
        if (state != NEW ||
                !UNSAFE.compareAndSwapObject(this, runnerOffset,
                        null, Thread.currentThread())) {
            return;
        }

        try {
            // 真正的任务
            Callable<V> c = callable;
            // state必须为NEW时才运行
            if (c != null && state == NEW) {
                // 运行的结果
                V result;
                boolean ran;
                try {
                    // 任务执行的地方【本文由公从号“彤哥读源码”原创】
                    result = c.call();
                    // 已执行完毕
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    // 处理异常
                    setException(ex);
                }
                if (ran) {
                    // 处理结果
                    set(result);
                }
            }
        } finally {
            // 置空runner
            runner = null;
            // 处理中断
            int s = state;
            if (s >= INTERRUPTING) {
                handlePossibleCancellationInterrupt(s);
            }
        }
    }

    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        if (s == INTERRUPTING) {
            while (state == INTERRUPTING) {
                Thread.yield(); // wait out pending interrupt
            }
        }

        // assert state == INTERRUPTED;

        // We want to clear any interrupt we may have received from
        // cancel(true).  However, it is permissible to use interrupts
        // as an independent mechanism for a task to communicate with
        // its caller, and there is no way to clear only the
        // cancellation interrupt.
        //
        // Thread.interrupted();
    }

    /**
     * 将最终的状态设置为EXCEPTIONAL
     * 返回值置为传进来的异常（outcome为调用get()方法时返回的）
     * @param t
     */
    protected void setException(Throwable t) {
        // 将状态从NEW置为COMPLETING
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 返回值置为传进来的异常（outcome为调用get()方法时返回的）
            outcome = t;
            // 最终的状态设置为EXCEPTIONAL
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            // 调用完成方法
            finishCompletion();
        }
    }

    /**
     * 最终的状态设置为NORMAL
     * 返回值置为传进来的结果（outcome为调用get()方法时返回的）
     * @param v
     */
    protected void set(V v) {
        // 将状态从NEW置为COMPLETING
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 返回值置为传进来的结果（outcome为调用get()方法时返回的）
            outcome = v;
            // 最终的状态设置为NORMAL
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            // 调用完成方法
            finishCompletion();
        }
    }

    /**
     * 上述方法似乎差不多，不同的是出去的结果不一样且状态不一样，最后都调用了finishCompletion()方法
     */
    private void finishCompletion() {
        // 如果队列不为空（这个队列实际上为调用者线程）
        for (WaitNode q; (q = waiters) != null;) {
            // 置空队列
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    // 调用者线程
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        // 如果调用者线程不为空，则唤醒它
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null) {
                        break;
                    }
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }
        // 钩子方法，子类重写
//        done();
        // 置空任务
        callable = null;        // to reduce footprint
    }

    /**
     * get()方法调用时如果任务未执行完毕，会阻塞直到任务结束
     * 如果任务状态小于等于COMPLETING，则进入队列等待
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        // 如果状态小于等于COMPLETING，则进入队列等待
        if (s <= COMPLETING) {
            s = awaitDone(false, 0L);
        }
        // 返回结果（异常）
        return report(s);
    }

    /**
     * 这里我们假设调用get()时任务还未执行，也就是其状态为NEW，我们试着按上面标示的1、2、3、4走一遍逻辑：
     * （1）第一次循环，状态为NEW，直接到1处，初始化队列并把调用者线程封装在WaitNode中；
     * （2）第二次循环，状态为NEW，队列不为空，到2处，让包含调用者线程的WaitNode入队；
     * （3）第三次循环，状态为NEW，队列不为空，且已入队，到3处，阻塞调用者线程；
     * （4）假设过了一会任务执行完毕了，根据run()方法的分析最后会unpark调用者线程，也就是3处会被唤醒；
     * （5）第四次循环，状态肯定大于COMPLETING了，退出循环并返回；
     *
     * 问题：为什么要在for循环中控制整个流程呢，把这里的每一步单独拿出来写行不行？
     * 答：因为每一次动作都需要重新检查状态state有没有变化，如果拿出去写也是可以的，只是代码会非常冗长。
     * 这里只分析了get()时状态为NEW，其它的状态也可以自行验证，都是可以保证正确的，甚至两个线程交叉运行（断点的技巧）。
     *
     * 问题：为什么要在for循环中控制整个流程呢，把这里的每一步单独拿出来写行不行？
     * 答：因为每一次动作都需要重新检查状态state有没有变化，如果拿出去写也是可以的，只是代码会非常冗长。
     *    这里只分析了get()时状态为NEW，其它的状态也可以自行验证，都是可以保证正确的，甚至两个线程交叉运行（断点的技巧）。
     *    OK，这里返回之后，再看看是怎么处理最终的结果的。
     * @param timed
     * @param nanos
     * @return
     * @throws InterruptedException
     */
    private int awaitDone(boolean timed, long nanos)
            throws InterruptedException {
        // 我们这里假设不带超时
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        for (;;) {
            // 处理中断
            if (Thread.interrupted()) {
//                removeWaiter(q);
                throw new InterruptedException();
            }
            // 4. 如果状态大于COMPLETING了，则跳出循环并返回
            // 这是自旋的出口
            int s = state;
            if (s > COMPLETING) {
                if (q != null) {
                    q.thread = null;
                }
                return s;
            }
            // 如果状态等于COMPLETING，说明任务快完成了，就差设置状态到NORMAL或EXCEPTIONAL和设置结果了
            // 这时候就让出CPU，优先完成任务
            else if (s == COMPLETING) {
                // cannot time out yet
                Thread.yield();
            }
                // 1. 如果队列为空
            else if (q == null) {
                // 初始化队列（WaitNode中记录了调用者线程）
                q = new WaitNode();
            }
                // 2. 未进入队列
            else if (!queued) {
                // 尝试入队
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                        q.next = waiters, q);
            }
            // 超时处理
            else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
//                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
            }
            // 3. 阻塞当前线程（调用者线程）
            else {
                LockSupport.park(this);
            }
        }
    }

    /**
     *  还记得前面分析run的时候吗，任务执行异常时是把异常放在outcome里面的，这里就用到了。
     * （1）如果正常执行结束，则返回任务的返回值；
     * （2）如果异常结束，则包装成ExecutionException异常抛出；
     *  通过这种方式，线程中出现的异常也可以返回给调用者线程了，不会像执行普通任务那样调用者是不知道任务执行到底有没有成功的。
     * @param s
     * @return
     * @throws ExecutionException
     */
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        // 任务正常结束
        if (s == NORMAL) {
            return (V)x;
        }
        // 被取消了
        if (s >= CANCELLED) {
            throw new CancellationException();
        }
        // 执行异常
        throw new ExecutionException((Throwable)x);
    }

    /**
     * FutureTask除了可以获取任务的返回值以外，还能够取消任务的执行。
     * 这里取消任务是通过中断执行线程来处理的
     * @param mayInterruptIfRunning
     * @return
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW &&
                UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                        mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) {
            return false;
        }

        try {    // in case call to interrupt throws exception
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null) {
                        t.interrupt();
                    }
                } finally { // final state
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
