package aqs.reentrant;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @Author yangwentian5
 * @Date 2022/3/4 11:53
 */
public class ReentrantLock {

    /**
     * 它在构造方法中初始化，决定使用公平锁还是非公平锁的方式获取锁
     */
    private final Sync sync;

    // 默认构造方法使用的是非公平锁
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    // 自己可选择使用公平锁还是非公平锁
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    // ReentrantLock.lock()
    public void lock() {
        // 调用的sync属性的lock()方法
        // 这里的sync是公平锁，所以是FairSync的实例
        sync.lock();
    }

    // ReentrantLock.tryLock()
    public boolean tryLock() {
        // 非公平锁和公平锁的实现在Sync.nonfairTryAcquire中
        return sync.nonfairTryAcquire(1);
    }

    // java.util.concurrent.locks.ReentrantLock.unlock()
    public void unlock() {
        sync.release(1);
    }

    // ReentrantLock.tryLock()
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        // 调用AQS中的方法
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 线程中断，只是在线程上打一个中断标志，并不会对运行中的线程有什么影响，具体需要根据这个中断标志干些什么，用户自己去决定。
     * 比如，如果用户在调用lock()获取锁后，发现线程中断了，就直接返回了，而导致没有释放锁，这也是允许的，但是会导致这个锁一直得不到释放，就出现了死锁。
     */
    public void lockInterruptibly() {
        this.lock();
        if (Thread.currentThread().interrupted()) {
            return ;
        }
        //当然，这里只是举个例子，实际使用肯定是要把lock.lock()后面的代码都放在try...finally...里面的以保证锁始终会释放，
        // 这里主要是为了说明线程中断只是一个标志，至于要做什么完全由用户自己决定。
        this.unlock();
    }

    /**
     * 抽象类Sync实现了AQS的部分方法
     *
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        /**
         * lockInterruptibly()方法
         * 支持线程中断，它与lock()方法的主要区别在于lockInterruptibly()获取锁的时候如果线程中断了，会抛出一个异常，
         * 而lock()不会管线程是否中断都会一直尝试获取锁，获取锁之后把自己标记为已中断，继续执行自己的逻辑，后面也会正常释放锁
         */
        protected abstract void lock();

        // ReentrantLock.Sync.nonfairTryAcquire()
        public final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                // 如果状态变量的值为0，再次尝试CAS更新状态变量的值
                // 相对于公平锁模式少了!hasQueuedPredecessors()条件
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) {
                    // overflow
                    throw new Error("Maximum lock count exceeded");
                }
                setState(nextc);
                return true;
            }
            return false;
        }

        /**
         * 公平锁和非公平锁底层全都使用此方法解锁
         * 1. state值减一，并设置状态变量的值
         * 2. 如果state为0清空占有线程exclusiveOwnerThread
         * @param releases
         * @return
         */
        @Override
        // java.util.concurrent.locks.ReentrantLock.Sync.tryRelease
        public final boolean tryRelease(int releases) {
            int c = getState() - releases;
            // 如果当前线程不是占有着锁的线程，抛出异常
            if (Thread.currentThread() != getExclusiveOwnerThread()) {
                throw new IllegalMonitorStateException();
            }
            boolean free = false;
            // 如果状态变量的值为0了，说明完全释放了锁
            // 这也就是为什么重入锁调用了多少次lock()就要调用多少次unlock()的原因
            // 如果不这样做，会导致锁不会完全释放，别的线程永远无法获取到锁
            if (c == 0) {
                free = true;
                // 清空占有线程
                setExclusiveOwnerThread(null);
            }
            // 设置状态变量的值
            setState(c);
            return free;
        }
    }

    /**
     * NonfairSync实现了Sync，主要用于非公平锁的获取
     * 相对于公平锁，非公平锁加锁的过程主要有两点不同：
     * （1）一开始就尝试CAS更新状态变量state的值，如果成功了就获取到锁了；
     * （2）在tryAcquire()的时候没有检查是否前面有排队的线程，直接上去获取锁才不管别人有没有排队呢；
     */
    static final class NonfairSync extends Sync {
        // ReentrantLock.NonfairSync.lock()
        // 这个方法在公平锁模式下是直接调用的acquire(1);
        @Override
        public final void lock() {
            // 直接尝试CAS更新状态变量
            if (compareAndSetState(0, 1)) {
                // 如果更新成功，说明获取到锁，把当前线程设为独占线程
                setExclusiveOwnerThread(Thread.currentThread());
            }
            else {
                acquire(1);
            }
        }

        // ReentrantLock.NonfairSync.tryAcquire()
        @Override
        protected final boolean tryAcquire(int acquires) {
            // 调用父类的方法
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * FairSync实现了Sync，主要用于公平锁的获取
     * ReentrantLock#lock()
     * ->ReentrantLock.FairSync#lock() // 公平模式获取锁
     *   ->AbstractQueuedSynchronizer#acquire() // AQS的获取锁方法
     *     ->ReentrantLock.FairSync#tryAcquire() // 尝试获取锁，此方法是自己实现的
     *     ->AbstractQueuedSynchronizer#addWaiter()  // 添加到队列
     * 	  ->AbstractQueuedSynchronizer#enq()  // 入队
     *     ->AbstractQueuedSynchronizer#acquireQueued() // 里面有个for()循环，唤醒后再次尝试获取锁
     *       ->AbstractQueuedSynchronizer#shouldParkAfterFailedAcquire() // 检查是否要阻塞
     *       ->AbstractQueuedSynchronizer#parkAndCheckInterrupt()  // 真正阻塞的地方
     */
    static final class FairSync extends Sync {

        // ReentrantLock.FairSync.lock()
        @Override
        public final void lock() {
            // 调用AQS的acquire()方法获取锁
            // 注意，这里传的值为1
            acquire(1);
        }

        /**
         * lock中的直接调用acquire
         * 1. 先判断是否获锁成功，注意这里基于模板方法模式，公平锁与非公平锁不一样
         * 2. 获锁成功跳出
         * 3. 获锁失败，进入同步队列
         * 4. 同步队列种的每个节点尝试获取锁，前提是自己的前驱节点是head才有可能获得锁
         * 5. 获得锁后更新同步队列节点，使自己变为头节点
         * 6. acquireQueued返回的是同步队列中线程的中断标识位
         */
        // AbstractQueuedSynchronizer.acquire()
        // TODO 这里是从AbstractQueuedSynchronizer粘贴过来的，这是要看的哈~
//        public final void acquire(int arg) {
//            // 尝试获取锁
//            // 如果失败了，就排队
//            if (!tryAcquire(arg) &&
//                    // 注意addWaiter()为排队，addWaiter()这里传入的节点模式为独占模式
                        // 如果获锁成功，acquireQueued返回中断位为false
//                    acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) {
//                selfInterrupt(); // 中断
//            }
//        }

        /**
         * 头节点的下一个节点才会执行此方法
         * 公平锁的前提，若是等待队列中该唤醒（头节点的下一个）的节点正是当前线程并且乐观锁获锁成功，则获取锁
         * 可重入锁state再++的精华也在此方法中
         * @param acquires
         * @return
         */
        @Override
        // ReentrantLock.FairSync.tryAcquire()，tryAcquire()方法需要重写
        protected final boolean tryAcquire(int acquires) {
            // 当前线程
            final Thread current = Thread.currentThread();
            // 查看当前状态变量的值
            int c = getState();
            // 如果状态变量的值为0，说明暂时还没有人占有锁
            if (c == 0) {
                // 如果没有其它线程在排队，那么当前线程尝试更新state的值为1
                // 如果成功了，则说明当前线程获取了锁
                // hasQueuedPredecessors是公平锁的精华所在，只有一个节点或者如果头节点的下一个节点的值不是当前线程的值，则返回false。
                if (!hasQueuedPredecessors() &&
                        compareAndSetState(0, acquires)) {
                    // 当前线程获取了锁，把自己设置到exclusiveOwnerThread变量中
                    // exclusiveOwnerThread是AQS的父类AbstractOwnableSynchronizer（AQS继承于它）中提供的变量
                    setExclusiveOwnerThread(current);
                    // 返回true说明成功获取了锁
                    return true;
                }
            }
            // 如果当前线程本身就占有着锁，现在又尝试获取锁
            // 那么，直接让它获取锁并返回true
            // 这里是可重入锁的实现精华
            else if (current == getExclusiveOwnerThread()) {
                // 状态变量state的值加1
                int nextc = c + acquires;
                // 如果溢出了，则报错
                if (nextc < 0) {
                    throw new Error("Maximum lock count exceeded");
                }
                // 设置到state中
                // 这里不需要CAS更新state
                // 因为当前线程占有着锁，其它线程只会CAS把state从0更新成1，是不会成功的
                // 所以不存在竞争，自然不需要使用CAS来更新
                setState(nextc);
                // 当线程获取锁成功
                return true;
            }
            // 当前线程尝试获取锁失败
            return false;
        }
    }

    /**
     * tryAcquireNanos调用doAcquireNanos
     * 1. 如果线程中断了，抛出异常
     * 2. 先尝试获取一次锁，获取锁失败加入同步队列，然后尝试获取锁
     */
    // AbstractQueuedSynchronizer.tryAcquireNanos()
//        public final boolean tryAcquireNanos(int arg, long nanosTimeout)
//                throws InterruptedException {
//            // 如果线程中断了，抛出异常
//            if (Thread.interrupted())
//                throw new InterruptedException();
//            // 先尝试获取一次锁
//            return tryAcquire(arg) ||
//                    doAcquireNanos(arg, nanosTimeout);
//        }

    /**
     * 也是先尝试获取锁，获取锁失败加入同步队列，然后尝试获取锁
     * 只不过相比未有超时时间的tryLock相比，在阻塞的时候加上阻塞时间，并且会随时检查是否到期，只要到期了没获取到锁就返回false
     * 同时也会有可能中断异常抛出
     */
    // AbstractQueuedSynchronizer.doAcquireNanos()
//    private boolean doAcquireNanos(int arg, long nanosTimeout)
//            throws InterruptedException {
//        // 如果时间已经到期了，直接返回false
//        if (nanosTimeout <= 0L)
//            return false;
//        // 到期时间
//        final long deadline = System.nanoTime() + nanosTimeout;
//        final Node node = addWaiter(Node.EXCLUSIVE);
//        boolean failed = true;
//        try {
//            for (;;) {
//                final Node p = node.predecessor();
//                // 自旋锁不断循环，如果是等待队列中的线程可以获得锁，返回true
//                if (p == head && tryAcquire(arg)) {
//                    setHead(node);
//                    p.next = null; // help GC
//                    failed = false;
//                    return true;
//                }
//                nanosTimeout = deadline - System.nanoTime();
//                // 如果到期了，就直接返回false
//                if (nanosTimeout <= 0L)
//                    return false;
//                // spinForTimeoutThreshold = 1000L;
//                // 只有到期时间大于1000纳秒，才阻塞
//                // 小于等于1000纳秒，直接自旋解决就得了
//                if (shouldParkAfterFailedAcquire(p, node) &&
//                        nanosTimeout > spinForTimeoutThreshold)
//                    // 阻塞一段时间
//                    LockSupport.parkNanos(this, nanosTimeout);
//                if (Thread.interrupted())
//                    throw new InterruptedException();
//            }
//        } finally {
//            if (failed)
//                cancelAcquire(node);
//        }
//    }

    /**
     * 将获取锁失败的线程放入等待队列中
     * 1. 为当前节点创建新Node节点
     * 2. 如果尾结点不为空，尾插法，并返回新建Node节点
     * 3. 如果尾节点为空，调用enq方法
     */
    // AbstractQueuedSynchronizer.addWaiter()
    // 调用这个方法，说明上面尝试获取锁失败了
//        private Node addWaiter(Node mode) {
//            // 新建一个节点
    // Node.EXCLUSIVE为null，Node.nextWaiter为null，Node.thread为Thread.currentThread()
//            Node node = new Node(Thread.currentThread(), mode);
//            // 这里先尝试把新节点加到尾节点后面
//            // 如果成功了就返回新节点
//            // 如果没成功再调用enq()方法不断尝试
//            Node pred = tail;
//            // 如果尾节点不为空
//            if (pred != null) {
//                // 设置新节点的前置节点为现在的尾节点
//                node.prev = pred;
//                // CAS更新尾节点为新节点
//                if (compareAndSetTail(pred, node)) {
//                    // 如果成功了，把旧尾节点的下一个节点指向新节点
//                    pred.next = node;
//                    // 并返回新节点
//                    return node;
//                }
//            }
//            // 如果上面尝试入队新节点没成功，调用enq()处理
//            enq(node);
//            return node;
//        }

    /**
     * 1. 懒加载等待队列的头尾节点，注意头节点算是哨兵节点，不存任何值，最开始头尾指针指向这哨兵节点
     * 2. 初始化完成后或已经初始化则尾插法，返回node
     */
    // AbstractQueuedSynchronizer.enq()
//        private Node enq(final Node node) {
//            // 自旋，不断尝试
//            for (;;) {
//                Node t = tail;
//                // 如果尾节点为空，说明还未初始化
//                if (t == null) { // Must initialize
//                    // 初始化头节点和尾节点，开始用哨兵节点当作头节点和尾节点
//                    if (compareAndSetHead(new Node()))
//                        tail = head;
//                } else {
//                    // 如果尾节点不为空
//                    // 设置新节点的前一个节点为现在的尾节点
//                    node.prev = t;
//                    // CAS更新尾节点为新节点
//                    if (compareAndSetTail(t, node)) {
//                        // 成功了，则设置旧尾节点的下一个节点为新节点
//                        t.next = node;
//                        // 并返回旧尾节点
//                        return t;
//                    }
//                }
//            }
//        }

    /**
     * 1. 等待队列中的每一个节点都尝试去获取锁
     * 2. 获取锁失败，通过shouldParkAfterFailedAcquire判断是否阻塞线程
     * 3. 判断是需要阻塞（即waitStatus属性为Node.SIGNAL），则调用LockSupport.park阻塞线程，并返回该线程是否需要中断标识位
     * 4. 若是节点不需要被阻塞（即waitStatus属性不是Node.SIGNAL），则继续尝试获取锁
     * 5. 若是被唤醒，且前一个节点是头结点，则尝试获取锁。获取锁成功设置自己为头结点
     */
    // AbstractQueuedSynchronizer.acquireQueued()
    // 调用上面的addWaiter()方法使得新节点已经成功入队了
    // 这个方法是尝试让当前节点来获取锁的
//        final boolean acquireQueued(final Node node, int arg) {
//            // 失败标记
//            boolean failed = true;
//            try {
//                // 中断标记
//                boolean interrupted = false;
//                // 自旋
//                for (;;) {
//                    // 当前节点的前一个节点
//                    final Node p = node.predecessor();
//                    // 如果当前节点的前一个节点为head节点，则说明轮到自己获取锁了
//                    // 调用ReentrantLock.FairSync.tryAcquire()方法再次尝试获取锁
//                    if (p == head && tryAcquire(arg)) {
//                        // 尝试获取锁成功
//                        // 这里同时只会有一个线程在执行，所以不需要用CAS更新
//                        // 把当前节点设置为新的头节点
//                        setHead(node);
//                        // 并把上一个节点从链表中删除
//                        p.next = null; // help GC
//                        // 未失败
//                        failed = false;
//                        return interrupted;
//                    }
//                    // 是否需要阻塞
//                    if (shouldParkAfterFailedAcquire(p, node) &&
//                            // 真正阻塞的方法，阻塞当前线程。并返回中断标识
//                            parkAndCheckInterrupt())
//                        // 如果中断了
//                        interrupted = true;
//                }
//            } finally {
//                // 若是上面出现问题自旋跳出，如果失败了
//                if (failed)
//                    // 取消获取锁，即在等待队列中释放该节点
//                    cancelAcquire(node);
//            }
//        }

    /**
     * 判断若是等待队列中的每一个节点是否需要被阻塞
     * 第一次调用会把前一个节点的等待状态设置为SIGNAL，并返回false
     * 第二次调用才会返回true
     */
    // AbstractQueuedSynchronizer.shouldParkAfterFailedAcquire()
    // 这个方法是在上面的for()循环里面调用的
//        private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
//            // 上一个节点的等待状态
//            // 注意Node的waitStatus字段我们在上面创建Node的时候并没有指定
//            // 也就是说使用的是默认值0
//            // 这里把各种等待状态再贴出来
//            //static final int CANCELLED =  1;
//            //static final int SIGNAL    = -1;
//            //static final int CONDITION = -2;
//            //static final int PROPAGATE = -3;

//            int ws = pred.waitStatus;
//            // 如果等待状态为SIGNAL(等待唤醒)，直接返回true
//            if (ws == Node.SIGNAL)
//                return true;
//            // 如果前一个节点的状态大于0，也就是已取消状态
//            if (ws > 0) {
//                // 把前面所有取消状态的节点都从链表中删除
//                do {
//                    node.prev = pred = pred.prev;
//                } while (pred.waitStatus > 0);
//                pred.next = node;
//            } else {
//                // 如果前一个节点的状态小于等于0且不等于-1，则把其状态设置为等待唤醒
//                // 这里可以简单地理解为把初始状态0设置为SIGNAL
//                // CONDITION是条件锁的时候使用的
//                // PROPAGATE是共享锁使用的
//                compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
//            }
//            return false;
//        }

    /**
     * 这里底层调用的是Unsafe的park()方法，
     * 并返回中断位标识
     */
    // AbstractQueuedSynchronizer.parkAndCheckInterrupt()
//        private final boolean parkAndCheckInterrupt() {
//            // 阻塞当前线程
//            // 底层调用的是Unsafe的park()方法
//            LockSupport.park(this);
//            // 返回是否已中断
//            return Thread.interrupted();
//        }

    /**
     * 释放锁的过程大致为：
     * （1）将state的值减1；
     * （2）如果state减到了0，说明已经完全释放锁了，唤醒下一个等待着的节点；
     */
    // java.util.concurrent.locks.AbstractQueuedSynchronizer.release
//    public final boolean release(int arg) {
//        // 调用AQS实现类的tryRelease()方法释放锁
//        if (tryRelease(arg)) {
//            Node h = head;
//            // 如果头节点不为空，且等待状态不是0，就唤醒下一个节点
//            // 还记得waitStatus吗？
//            // 在每个节点阻塞之前会把其上一个节点的等待状态设为SIGNAL（-1）
//            // 所以，SIGNAL的准确理解应该是唤醒下一个等待的线程
//            if (h != null && h.waitStatus != 0)
//                unparkSuccessor(h);
//            return true;
//        }
//        return false;
//    }

    /**
     * 唤醒节点
     * 1. 如果头节点的等待状态小于0，就把它设置为0
     * 2. 从尾节点向前遍历取到队列最前面的那个状态不是已取消状态的节点并唤醒它
     */
//    private void unparkSuccessor(Node node) {
//        // 注意，这里的node是头节点
//
//        // 如果头节点的等待状态小于0，就把它设置为0
//        int ws = node.waitStatus;
//        if (ws < 0)
//            compareAndSetWaitStatus(node, ws, 0);
//
//        // 头节点的下一个节点
//        Node s = node.next;
//        // 如果下一个节点为空，或者其等待状态大于0（实际为已取消）
//        if (s == null || s.waitStatus > 0) {
//            s = null;
//            // 从尾节点向前遍历取到队列最前面的那个状态不是已取消状态的节点
//            for (Node t = tail; t != null && t != node; t = t.prev)
//                if (t.waitStatus <= 0)
//                    s = t;
//        }
//        // 如果s节点不为空，则唤醒它
//        if (s != null)
//            LockSupport.unpark(s.thread);
//    }
}
