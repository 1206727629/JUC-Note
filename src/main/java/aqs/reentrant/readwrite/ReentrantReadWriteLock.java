package aqs.reentrant.readwrite;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author yangwentian5
 * @Date 2022/3/9 14:21
 * （1）ReentrantReadWriteLock本身实现了ReadWriteLock接口，这个接口只提供了两个方法readLock()和writeLock（）；
 * （2）同步器，包含一个继承了AQS的Sync内部类，以及其两个子类FairSync和NonfairSync；
 * （3）ReadLock和WriteLock两个内部类实现了Lock接口，它们具有锁的一些特性。
 *  这里先以非公平模式举例子
 *
 * （1）ReentrantReadWriteLock采用读写锁的思想，能提高并发的吞吐量；
 * （2）读锁使用的是共享锁，多个读锁可以一起获取锁，互相不会影响，即读读不互斥；
 * （3）读写、写读和写写是会互斥的，前者占有着锁，后者需要进入AQS队列中排队；
 * （4）多个连续的读线程是一个接着一个被唤醒的，而不是一次性唤醒所有读线程；
 * （5）只有多个读锁都完全释放了才会唤醒下一个写线程；
 * （6）只有写锁完全释放了才会唤醒下一个等待者，这个等待者有可能是读线程，也可能是写线程；
 */
public class ReentrantReadWriteLock {
    /**
     * 属性中的读锁和写锁是私有属性，通过这两个方法暴露出去
     */
    // 读锁
    private final ReentrantReadWriteLock.ReadLock readerLock;
    // 写锁
    private final ReentrantReadWriteLock.WriteLock writerLock;
    // 同步器
    final Sync sync;

    // 默认构造方法
    public ReentrantReadWriteLock() {
        this(false);
    }

    // 是否使用公平锁的构造方法
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }

    abstract static class Sync extends AbstractQueuedSynchronizer {
        /**
         * 读锁尝试获取锁
         * (1)若是因为写锁导致获取读锁失败返回-1
         * (2)readerShouldBlock需要分公平锁和非公平锁的，非公平锁需要看队列中下一个元素是不是共享元素；公平锁要看头节点的下一个节点是不是当前线程
         * (3)获取读锁成功，第一个读者通过firstReaderHoldCount设置重入和首次获取读锁的次数
         * (4)非第一个读者通过cachedHoldCounter和readHolds(本身是一个ThreadLocal)设置获取读锁的次数
         *
         * readHolds记录的是本线程的重入次数，state是总的
         */
        // ReentrantReadWriteLock.Sync.tryAcquireShared()
//        protected final int tryAcquireShared(int unused) {
//            Thread current = Thread.currentThread();
//            // 状态变量的值
//            // 在读写锁模式下，高16位存储的是共享锁（读锁）被获取的次数，低16位存储的是互斥锁（写锁）被获取的次数
//            int c = getState();
//            // exclusiveCount获取低16位的值，表示互斥锁的次数
//            // 如果其它线程获得了写锁，直接返回-1
//            if (exclusiveCount(c) != 0 &&
//                    getExclusiveOwnerThread() != current)
//                return -1;
//            // 读锁被获取的次数
//            int r = sharedCount(c);
//
//            // 下面说明此时还没有写锁，尝试去更新state的值获取读锁
//            // 读者是否需要排队（是否是公平模式）
//            if (!readerShouldBlock() &&
//                    r < MAX_COUNT &&
//                    compareAndSetState(c, c + SHARED_UNIT)) {
//                // 获取读锁成功
//                if (r == 0) {
//                    // 如果之前还没有线程获取读锁
//                    // 记录第一个读者为当前线程
//                    firstReader = current; // 第一个获取读锁的线程(并且其未释放读锁)，以及它持有的读锁数量  提高性能
//                    // 第一个读者重入的次数为1
//                    firstReaderHoldCount = 1;
//                } else if (firstReader == current) {
//                    // 如果有线程获取了读锁且是当前线程是第一个读者
//                    // 则把其重入次数加1
//                    firstReaderHoldCount++;
//                } else {
//                    // 如果有线程获取了读锁且当前线程不是第一个读者
//                    // 则从缓存中获取重入次数保存器
//                    HoldCounter rh = cachedHoldCounter; // 记录最后一个获取读锁的线程的读锁重入次数，用于缓存提高性能
//                    // 如果缓存不属性当前线程
//                    // 再从ThreadLocal中获取
//                    // readHolds本身是一个ThreadLocal，里面存储的是HoldCounter
//                    if (rh == null || rh.tid != getThreadId(current))
//                        // get()的时候会初始化rh
//                        cachedHoldCounter = rh = readHolds.get();
//                    else if (rh.count == 0)
//                        // 如果rh的次数为0，把它放到ThreadLocal中去
//                        readHolds.set(rh);
//                    // 重入的次数加1（初始次数为0）
//                    rh.count++;
//                }
//                // 获取读锁成功，返回1
//                return 1;
//            }
              // 需要排队或者CAS更新失败
//            // 通过这个方法再去尝试获取读锁（如果之前其它线程获取了写锁，一样返回-1表示失败）
//            return fullTryAcquireShared(current);
//        }

        /**
         *
         */
    //        final int fullTryAcquireShared(Thread current) {
//            HoldCounter rh = null;
             // 自旋锁
//            for (;;) {
//                int c = getState();
//                if (exclusiveCount(c) != 0) {
//                    if (getExclusiveOwnerThread() != current)
//                        return -1;
//                } else if (readerShouldBlock()) {
//                    // 如果需要排队的话
//                    if (firstReader == current) {
//                        // assert firstReaderHoldCount > 0;
//                    } else {
                          // 当前线程不是等待队列第一个的话
//                        if (rh == null) {
//                            rh = cachedHoldCounter;
//                            if (rh == null || rh.tid != getThreadId(current)) {
//                                rh = readHolds.get();
                                    // rh为0就可以清除ThreadLocal了
//                                if (rh.count == 0)
//                                    readHolds.remove();
//                            }
//                        }
//                        if (rh.count == 0)
//                            return -1;
//                    }
//                }

                // 下面中的逻辑基本和tryAcquireShared一致
//                if (sharedCount(c) == MAX_COUNT)
//                    throw new Error("Maximum lock count exceeded");
//                if (compareAndSetState(c, c + SHARED_UNIT)) {
//                    if (sharedCount(c) == 0) {
//                        firstReader = current;
//                        firstReaderHoldCount = 1;
//                    } else if (firstReader == current) {
//                        firstReaderHoldCount++;
//                    } else {
//                        if (rh == null)
//                            rh = cachedHoldCounter;
//                        if (rh == null || rh.tid != getThreadId(current))
//                            rh = readHolds.get();
//                        else if (rh.count == 0)
//                            readHolds.set(rh);
//                        rh.count++;
//                        cachedHoldCounter = rh; // cache for release
//                    }
//                    return 1;
//                }
//            }
//        }

        /**
         *
         */
    //        static final int SHARED_SHIFT   = 16;
//        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1; // 1111111111111111
        /**
         * 取低16位的值
         */
    //        static int exclusiveCount(int c) {
//            return c & EXCLUSIVE_MASK;
//        }
        /**
         * 取高16位的值
         */
    //        static int sharedCount(int c)    {
//            return c >>> SHARED_SHIFT;
//        }

        /**
         * （1）将当前线程重入的次数减1；
         * （2）将共享锁总共被获取的次数减1；
         * （3）如果共享锁获取的次数减为0了，说明共享锁完全释放了，那就唤醒下一个节点；
         */
        // java.util.concurrent.locks.ReentrantReadWriteLock.Sync.tryReleaseShared
//        protected final boolean tryReleaseShared(int unused) {
//            Thread current = Thread.currentThread();
//            if (firstReader == current) {
//                // 如果第一个读者（读线程）是当前线程
//                // 就把它重入的次数减1
//                // 如果减到0了就把第一个读者置为空
//                if (firstReaderHoldCount == 1)
//                    firstReader = null;
//                else
//                    firstReaderHoldCount--;
//            } else {
//                // 如果第一个读者不是当前线程
//                // 一样地，把它重入的次数减1
//                HoldCounter rh = cachedHoldCounter;
//                if (rh == null || rh.tid != getThreadId(current))
//                    rh = readHolds.get();
//                int count = rh.count;
//                if (count <= 1) {
//                    readHolds.remove();
//                    if (count <= 0)
//                        throw unmatchedUnlockException();
//                }
//                --rh.count;
//            }
//            for (;;) {
//                // 共享锁获取的次数减1
//                // 如果减为0了说明完全释放了，才返回true
//                int c = getState();
//                int nextc = c - SHARED_UNIT;
//                if (compareAndSetState(c, nextc))
//                    return nextc == 0;
//            }
//        }

        /**
         * (1) 先获取state的值，如果不等于0且
         */
        // java.util.concurrent.locks.ReentrantReadWriteLock.Sync.tryAcquire()
//        protected final boolean tryAcquire(int acquires) {
//            Thread current = Thread.currentThread();
//            // 状态变量state的值
//            int c = getState();
//            // 互斥锁被获取的次数
//            int w = exclusiveCount(c);
//            if (c != 0) {
//                // 如果c!=0且w==0，说明共享锁被获取的次数不为0
//                // 这句话整个的意思就是
//                // 如果共享锁被获取的次数不为0，或者被其它线程获取了互斥锁（写锁）
//                // 那么就返回false，获取写锁失败
//                if (w == 0 || current != getExclusiveOwnerThread())
//                    return false;
//                // 溢出检测
//                if (w + exclusiveCount(acquires) > MAX_COUNT)
//                    throw new Error("Maximum lock count exceeded");
//                // 到这里说明当前线程已经获取过写锁，这里是重入了，直接把state加1即可
//                setState(c + acquires);
//                // 获取写锁成功
//                return true;
//            }
//            // 如果c等于0，就尝试更新state的值（非公平模式writerShouldBlock()返回false）
//            // 如果失败了，说明获取写锁失败，返回false
//            // 如果成功了，说明获取写锁成功，把自己设置为占有者，并返回true
//            if (writerShouldBlock() ||
//                    !compareAndSetState(c, c + acquires))
//                return false;
//            setExclusiveOwnerThread(current);
//            return true;
//        }
        // 获取写锁失败了后面的逻辑跟ReentrantLock是一致的，进入队列排队，这里就不列源码了
    }

    static final class NonfairSync extends Sync {
        /**
         * AQS模板方法，读者是否需要排队
         * nextWaiter != SHARED 需要排队
         */
//        final boolean readerShouldBlock() {
//            return apparentlyFirstQueuedIsExclusive();
//        }

        /**
         * 非公平模式永远返回false
         */
//        final boolean writerShouldBlock() {
//            return false; // writers can always barge
//        }
    }

    static final class FairSync extends Sync {
        /**
         * AQS模板方法，读者是否需要排队
         * 直接比对队列中的下一个节点是不是当前线程
         */
//        final boolean readerShouldBlock() {
//            return hasQueuedPredecessors();
//        }

        /**
         * AQS模板方法，写者是否需要排队
         * 直接比对队列中的下一个节点是不是当前线程
         */
//        final boolean writerShouldBlock() {
//            return hasQueuedPredecessors();
//        }
    }

    public static class ReadLock implements Lock, java.io.Serializable {
        private final Sync sync;

        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        @Override
        // ReentrantReadWriteLock.ReadLock.lock()
        public void lock() {
            sync.acquireShared(1);
        }

        @Override
        // java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock.unlock
        public void unlock() {
            sync.releaseShared(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {

        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }

    /**
     * （1）先尝试获取读锁；
     * （2）如果成功了直接结束；
     * （3）如果失败了，进入doAcquireShared()方法；
     *
     *  在整个逻辑中是在哪里连续唤醒读节点的呢？
     *  答案是在doAcquireShared()方法中，在这里一个节点A获取了读锁后，会唤醒下一个读节点B，这时候B也会获取读锁，然后B继续唤醒C，依次往复，
     *  也就是说这里的节点是一个唤醒一个这样的形式，而不是一个节点获取了读锁后一次性唤醒后面所有的读节点。
     */
    // AbstractQueuedSynchronizer.acquireShared()
//    public final void acquireShared(int arg) {
//        // 尝试获取共享锁（返回1表示成功，返回-1表示失败）
//        if (tryAcquireShared(arg) < 0)
//            // 失败了就可能要排队
//            doAcquireShared(arg);
//    }

    /**
     * （1）doAcquireShared()方法中首先会生成一个新节点并进入AQS队列中；
     * （2）如果头节点正好是当前节点的上一个节点，再次尝试获取锁；
     * （3）如果成功了，则设置头节点为新节点，并传播；
     * （4）传播即唤醒下一个读节点（如果下一个节点是读节点的话）；
     * （5）如果头节点不是当前节点的上一个节点或者（5）失败，则阻塞当前线程等待被唤醒；
     * （6）唤醒之后继续走（5）的逻辑；
     */
    // AbstractQueuedSynchronizer.doAcquireShared()
//    private void doAcquireShared(int arg) {
//        // 进入AQS的队列中
//        final Node node = addWaiter(Node.SHARED);
//        boolean failed = true;
//        try {
//            boolean interrupted = false;
//            for (;;) {
//                // 当前节点的前一个节点
//                final Node p = node.predecessor();
//                // 如果前一个节点是头节点（说明是第一个排队的节点）
//                if (p == head) {
//                    // 再次尝试获取读锁
//                    int r = tryAcquireShared(arg);
//                    // 如果成功了
//                    if (r >= 0) {
//                        // 头节点后移并传播
//                        // 传播即唤醒后面连续的读节点
//                        setHeadAndPropagate(node, r);
//                        p.next = null; // help GC
//                        if (interrupted)
//                            selfInterrupt();
//                        failed = false;
//                        return;
//                    }
//                }
//                // 没获取到读锁，阻塞并等待被唤醒
//                if (shouldParkAfterFailedAcquire(p, node) &&
//                        parkAndCheckInterrupt())
//                    interrupted = true;
//            }
//        } finally {
//            if (failed)
//                cancelAcquire(node);
//        }
//    }

    /**
     * 设置当前节点为新头节点，并唤醒下一个节点
     */
    // AbstractQueuedSynchronizer.setHeadAndPropagate()
//    private void setHeadAndPropagate(Node node, int propagate) {
//        // h为旧的头节点
//        Node h = head;
//        // 设置当前节点为新头节点
//        setHead(node);
//
//        // 如果旧的头节点或新的头节点为空或者其等待状态小于0（表示状态为SIGNAL/PROPAGATE）
//        if (propagate > 0 || h == null || h.waitStatus < 0 ||
//                (h = head) == null || h.waitStatus < 0) {
//            // 需要传播
//            // 取下一个节点
//            Node s = node.next;
//            // 如果下一个节点为空，或者是需要获取读锁的节点
//            if (s == null || s.isShared())
//                // 唤醒下一个节点
//                doReleaseShared();
//        }
//    }

    /**
     * （1）尝试唤醒下一个节点，如果头节点状态为SIGNAL，说明要唤醒下一个节点
     * （2）如果头节点状态是默认0，则把头节点的状态改为PROPAGATE成功才会跳到下面的if即唤醒后head没变，则跳出循环
     */
    // AbstractQueuedSynchronizer.doReleaseShared()
// 这个方法只会唤醒一个节点
//    private void doReleaseShared() {
//        for (;;) {
//            Node h = head;
//            if (h != null && h != tail) {
//                int ws = h.waitStatus;
//                // 如果头节点状态为SIGNAL，说明要唤醒下一个节点
//                if (ws == Node.SIGNAL) {
//                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
//                        continue;            // loop to recheck cases
//                    // 唤醒下一个节点
//                    unparkSuccessor(h);
//                }
//                else if (ws == 0 &&
//                        // 把头节点的状态改为PROPAGATE成功才会跳到下面的if
//                        !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
//                    continue;                // loop on failed CAS
//            }
//            // 如果唤醒后head没变，则跳出循环
//            if (h == head)                   // loop if head changed
//                break;
//        }
//    }

    /**
     *
      */
    // java.util.concurrent.locks.AbstractQueuedSynchronizer.releaseShared
//    public final boolean releaseShared(int arg) {
//        // 如果尝试释放成功了，就唤醒下一个节点
//        if (tryReleaseShared(arg)) {
//            // 这个方法实际是唤醒下一个节点
//            doReleaseShared();
//            return true;
//        }
//        return false;
//    }

    public static class WriteLock implements Lock, java.io.Serializable {

        private final Sync sync;

        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        @Override
        // java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock.lock()
        public void lock() {
            sync.acquire(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {

        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void unlock() {

        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }

    // java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire()
//    public final void acquire(int arg) {
//        // 先尝试获取锁
//        // 如果失败，则会进入队列中排队，后面的逻辑跟ReentrantLock一模一样了
//        if (!tryAcquire(arg) &&
//                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
//            selfInterrupt();
//    }
}
