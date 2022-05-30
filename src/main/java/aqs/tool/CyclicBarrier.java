package aqs.tool;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * （1）CyclicBarrier会使一组线程阻塞在await()处，当最后一个线程到达时唤醒（只是从条件队列转移到AQS队列中）前面的线程大家再继续往下走；
 * （2）CyclicBarrier不是直接使用AQS实现的一个同步器；
 * （3）CyclicBarrier基于ReentrantLock及其Condition实现整个同步逻辑；
 *
 *  CyclicBarrier与CountDownLatch的异同？
 * （1）两者都能实现阻塞一组线程等待被唤醒；
 * （2）前者是最后一个线程到达时自动唤醒；
 * （3）后者是通过显式地调用countDown()实现的；
 * （4）前者是通过重入锁及其条件锁实现的，后者是直接基于AQS实现的；
 * （5）前者具有“代”的概念，可以重复使用，后者只能使用一次；
 * （6）前者只能实现多个线程到达栅栏处一起运行；
 * （7）后者不仅可以实现多个线程等待一个线程条件成立，还能实现一个线程等待多个线程条件成立（详见CountDownLatch那章使用案例）；
 * @Author yangwentian5
 * @Date 2022/3/22 17:51
 */
public class CyclicBarrier {
    // 重入锁
    private final ReentrantLock lock = new ReentrantLock();

    // 条件锁，名称为trip，绊倒的意思，可能是指线程来了先绊倒，等达到一定数量了再唤醒
    private final Condition trip = lock.newCondition();

    // 需要等待的线程数量
    private final int parties;

    // 当唤醒的时候执行的命令
    private final Runnable barrierCommand;

    // 代
    private Generation generation = new Generation();

    // 当前这一代还需要等待的线程数
    private int count;

    /**
     * 三个线程完成后进入下一代，继续等待三个线程达到栅栏处再一起执行，
     * 而CountDownLatch则做不到这一点，CountDownLatch是一次性的，无法重置其次数。
     */
    private static class Generation {
        boolean broken = false;
    }

    /**
     * 构造方法需要传入一个parties变量，也就是需要等待的线程数
     * @param parties
     * @param barrierAction
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) {
            throw new IllegalArgumentException();
        }
        // 初始化parties
        this.parties = parties;
        // 初始化count等于parties
        this.count = parties;
        // 初始化都到达栅栏处执行的命令
        this.barrierCommand = barrierAction;
    }

    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    /**
     * 每个需要在栅栏处等待的线程都需要显式地调用await()方法等待其它线程的到来。
     * @return
     * @throws InterruptedException
     * @throws BrokenBarrierException
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            // 调用dowait方法，不需要超时
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }

    /**
     * dowait()方法里的整个逻辑分成两部分：
     * （1）最后一个线程走上面的逻辑，当count减为0的时候，打破栅栏，它调用nextGeneration()方法通知条件队列中的等待线程转移到AQS的队列中等待被唤醒，并进入下一代。
     * （2）非最后一个线程走下面的for循环逻辑，这些线程会阻塞在condition的await()方法处，它们会加入到条件队列中，等待被通知，当它们唤醒的时候已经更新换“代”了，这时候返回。
     * @param timed
     * @param nanos
     * @return
     * @throws InterruptedException
     * @throws BrokenBarrierException
     * @throws TimeoutException
     */
    private int dowait(boolean timed, long nanos)
            throws InterruptedException, BrokenBarrierException,
            TimeoutException {
        final ReentrantLock lock = this.lock;
        // 加锁
        lock.lock();
        try {
            // 当前代
            final Generation g = generation;

            // 检查
            if (g.broken) {
                throw new BrokenBarrierException();
            }

            // 中断检查
            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }

            // count的值减1
            int index = --count;
            // 如果数量减到0了，走这段逻辑（最后一个线程走这里）
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    // 如果初始化的时候传了命令，这里执行
                    final Runnable command = barrierCommand;
                    if (command != null) {
                        command.run();
                    }
                    ranAction = true;
                    // 调用下一代方法
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction) {
                        breakBarrier();
                    }
                }
            }

            // 这个循环只有非最后一个线程可以走到
            for (;;) {
                try {
                    if (!timed) {
                        // 调用condition的await()方法
                        trip.await();
                    }
                    else if (nanos > 0L) {
                        // 超时等待方法
                        nanos = trip.awaitNanos(nanos);
                    }

                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }

                // 检查
                if (g.broken) {
                    throw new BrokenBarrierException();
                }

                // 正常来说这里肯定不相等
                // 因为上面打破栅栏的时候调用nextGeneration()方法时generation的引用已经变化了
                if (g != generation) {
                    return index;
                }

                // 超时检查
                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }

    private void nextGeneration() {
        // 调用condition的signalAll()将其队列中的等待者全部转移到AQS的队列中
        trip.signalAll();
        // 重置count
        count = parties;
        // 进入下一代
        generation = new Generation();
    }
}
