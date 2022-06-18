package aqs.reentrant.condition;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

/**
 * @Author yangwentian5
 * @Date 2022/3/7 16:13
 * 条件锁，是指在获取锁之后发现当前业务场景自己无法处理，而需要等待某个条件的出现才可以继续处理时使用的一种锁。
 * 比如，在阻塞队列中，当队列中没有元素的时候是无法弹出一个元素的，这时候就需要阻塞在条件notEmpty上，
 * 等待其它线程往里面放入一个元素后，唤醒这个条件notEmpty，当前线程才可以继续去做“弹出一个元素”的行为。
 * 注意，这里的条件，必须是在获取锁之后去等待，对应到ReentrantLock的条件锁，就是获取锁之后才能调用condition.await()方法。
 *
 * （1）重入锁是指可重复获取的锁，即一个线程获取锁之后再尝试获取锁时会自动获取锁；
 * （2）在ReentrantLock中重入锁是通过不断累加state变量的值实现的；
 * （3）ReentrantLock的释放要跟获取匹配，即获取了几次也要释放几次；
 * （4）ReentrantLock默认是非公平模式，因为非公平模式效率更高；
 * （5）条件锁是指为了等待某个条件出现而使用的一种锁；
 * （6）条件锁比较经典的使用场景就是队列为空时阻塞在条件notEmpty上；
 * （7）ReentrantLock中的条件锁是通过AQS的ConditionObject内部类实现的；
 * （8）await()和signal()方法都必须在获取锁之后释放锁之前使用；
 * （9）await()方法会新建一个节点放到条件队列中，接着完全释放锁，然后阻塞当前线程并等待条件的出现；
 * （10）signal()方法会寻找条件队列中第一个可用节点移到AQS队列中；
 * （11）在调用signal()方法的线程调用unlock()方法才真正唤醒阻塞在条件上的节点（此时节点已经在AQS队列中）；
 * （12）之后该节点会再次尝试获取锁，后面的逻辑与lock()的逻辑基本一致了。
 */
public class ConditionLock {

    /**
     * 它在构造方法中初始化，决定使用公平锁还是非公平锁的方式获取锁
     */
    private final Sync sync;

    // 默认构造方法使用的是非公平锁
    public ConditionLock() {
        sync = new ConditionLock.NonfairSync();
    }

    // 自己可选择使用公平锁还是非公平锁
    public ConditionLock(boolean fair) {
        sync = fair ? new ConditionLock.FairSync() : new ConditionLock.NonfairSync();
    }

    // ReentrantLock.newCondition()
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 抽象类Sync实现了AQS的部分方法
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {

        // ReentrantLock.Sync.newCondition()
        // 条件锁中也维护了一个队列，为了和AQS的队列区分，我这里称为条件队列，
        // firstWaiter是队列的头节点，lastWaiter是队列的尾节点

        // AQS的队列头节点firstWaiter是不存在任何值的，是一个虚节点；
        // Condition的队列头节点是存储着实实在在的元素值的，是真实节点。

        // AQS中下一个节点是next，上一个节点是prev；
        // Condition中下一个节点是nextWaiter，没有上一个节点
        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // ConditionObject属于AQS内部类
        // 可以看到条件锁中也维护了一个队列，为了和AQS的队列区分，我这里称为条件队列，firstWaiter是队列的头节点，lastWaiter是队列的尾节点
//        public class ConditionObject implements Condition, java.io.Serializable {
//            /** First node of condition queue. */
//            private transient Node firstWaiter;
//            /** Last node of condition queue. */
//            private transient Node lastWaiter;
        // AbstractQueuedSynchronizer.ConditionObject.ConditionObject()
//        public ConditionObject() { }
//        }

    }

    static final class NonfairSync extends Sync {

    }

    static final class FairSync extends Sync {

    }

    /**
     * 条件锁阻塞的逻辑
     *
     * （1）如果线程中断了，抛出异常
     * （2）新建一个节点加入到条件队列中去；
     * （3）完全释放当前线程占有的锁；
     * （4）阻塞当前线程，并等待条件的出现；
     * （5）条件已出现（此时节点已经移到AQS的队列中），尝试获取锁；
     * （6）有可能清除条件队列中节点状态waitStatus不是Node.CONDITION（取消状态）的节点并且看是否抛出异常或者给自己设置中断标识
     * 也就是说await()方法内部其实是先释放锁->等待条件->再次获取锁的过程
     */
    // AbstractQueuedSynchronizer.ConditionObject.await()
//    public final void await() throws InterruptedException {
//        // 如果线程中断了，抛出异常
//        if (Thread.interrupted())
//            throw new InterruptedException();
//        // 添加节点到Condition的队列中，并返回该节点
//        Node node = addConditionWaiter();
//        // 完全释放当前线程获取的锁
//        // 因为锁是可重入的，所以这里要把获取的锁全部释放
//        int savedState = fullyRelease(node);
//        int interruptMode = 0;
//        // 是否在同步队列中，不在同步队列进入while逻辑
//        while (!isOnSyncQueue(node)) {
//            // 阻塞当前线程
//            LockSupport.park(this);
//
//            // 上面部分是调用await()时释放自己占有的锁，并阻塞自己等待条件的出现
//            // *************************分界线*************************  //
//            // 下面部分是条件已经出现，线程中断跳出while循环
//
//            if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
//                break;
//        }
//        // 在同步队列的话，会走下面的逻辑
//        // 尝试获取锁，注意第二个参数，这是上一章分析过的方法
//        // 如果没获取到会再次阻塞（这个方法这里就不贴出来了，有兴趣的翻翻上一章的内容）
//        if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
//            interruptMode = REINTERRUPT;
//        // 清除条件队列中节点状态waitStatus不是Node.CONDITION（取消状态）的节点
//        if (node.nextWaiter != null) // clean up if cancelled
//            unlinkCancelledWaiters();
//        // 线程中断，看是否抛出异常或者给自己设置中断标识
//        if (interruptMode != 0)
//            reportInterruptAfterWait(interruptMode);
//    }

    /**
     * 添加新节点至条件队列，这里也是懒加载方式
     *
     * 1. 如果当前尾节点的状态不是Node.CONDITION，调用unlinkCancelledWaiters方法清除条件队列中状态不是Node.CONDITION（取消状态）的节点
     * 2. 首先，在条件队列中，新建节点的初始等待状态是CONDITION（-2）；
     * 3. 尾插法插入并返回新节点
     *
     * 首先，在条件队列中，新建节点的初始等待状态是CONDITION（-2）；
     * 其次，移到AQS的队列中时等待状态会更改为0（AQS队列节点的初始等待状态为0）；
     * 然后，在AQS的队列中如果需要阻塞，会把它上一个节点的等待状态设置为SIGNAL（-1）；
     * 最后，不管在Condition队列还是AQS队列中，已取消的节点的等待状态都会设置为CANCELLED（1）；
     * 另外，后面我们在共享锁的时候还会讲到另外一种等待状态叫PROPAGATE（-3）。
     */
    // AbstractQueuedSynchronizer.ConditionObject.addConditionWaiter
//    private Node addConditionWaiter() {
//        Node t = lastWaiter;
//        // 如果条件队列的尾节点已取消，从头节点开始清除所有已取消的节点
//        if (t != null && t.waitStatus != Node.CONDITION) {
//            unlinkCancelledWaiters();
//            // 重新获取尾节点
//            t = lastWaiter;
//        }
//        // 新建一个节点，它的等待状态是CONDITION
//        Node node = new Node(Thread.currentThread(), Node.CONDITION);
//        // 如果尾节点为空，则把新节点赋值给头节点（相当于初始化队列）
//        // 否则把新节点赋值给尾节点的nextWaiter指针
//        // 这里也是懒加载方式
//        if (t == null)
//            firstWaiter = node;
//        else
//            t.nextWaiter = node;
//        // 尾节点指向新节点
//        lastWaiter = node;
//        // 返回新节点
//        return node;
//    }

    /**
     * 清除条件队列中状态不是Node.CONDITION（取消状态）的节点
     */
//    private void unlinkCancelledWaiters() {
//        AbstractQueuedSynchronizer.Node t = firstWaiter;
//        AbstractQueuedSynchronizer.Node trail = null;
//        while (t != null) {
//            AbstractQueuedSynchronizer.Node next = t.nextWaiter;
//            if (t.waitStatus != AbstractQueuedSynchronizer.Node.CONDITION) {
//                t.nextWaiter = null;
//                if (trail == null)
//                    firstWaiter = next;
//                else
//                    trail.nextWaiter = next;
//                if (next == null)
//                    lastWaiter = trail;
//            }
//            else
//                trail = t;
//            t = next;
//        }
//    }

    /**
     * 一次性释放所有获得的锁，返回获取锁的次数
     */
    // AbstractQueuedSynchronizer.fullyRelease
//    final int fullyRelease(Node node) {
//        boolean failed = true;
//        try {
//            // 获取状态变量的值，重复获取锁，这个值会一直累加
//            // 所以这个值也代表着获取锁的次数
//            int savedState = getState();
//            // 一次性释放所有获得的锁
//            if (release(savedState)) {
//                failed = false;
//                // 返回获取锁的次数
//                return savedState;
//            } else {
//                throw new IllegalMonitorStateException();
//            }
//        } finally {
//            if (failed)
//                node.waitStatus = Node.CANCELLED;
//        }
//    }

    /**
     * 尝试释放锁并唤醒下一个节点
     */
//    public final boolean release(int arg) {
//        if (tryRelease(arg)) {
//            Node h = head;
//            if (h != null && h.waitStatus != 0)
//                unparkSuccessor(h);
//            return true;
//        }
//        return false;
//    }

    /**
     * 看此节点是否在AQS的同步队列中
     */
    // AbstractQueuedSynchronizer.isOnSyncQueue
//    final boolean isOnSyncQueue(Node node) {
//        // 如果等待状态是CONDITION，或者前一个指针为空，返回false
//        // 说明还没有移到AQS的队列中
//        if (node.waitStatus == Node.CONDITION || node.prev == null)
//            return false;
//        // 如果next指针有值，说明已经移到AQS的队列中了
//        if (node.next != null) // If has successor, it must be on queue
//            return true;
//        // 从AQS的尾节点开始往前寻找看是否可以找到当前节点，找到了也说明已经在AQS的队列中了
//        return findNodeFromTail(node);
//    }

    /**
     * 从尾结点向前遍历看是否node节点在同步队列
     */
//    private boolean findNodeFromTail(Node node) {
//        Node t = tail;
//        for (;;) {
//            if (t == node)
//                return true;
//            if (t == null)
//                return false;
//            t = t.prev;
//        }
//    }

    /**
     *  signal()方法的大致流程为：
     * （1）从条件队列的头节点开始寻找一个非取消状态的节点；
     * （2）把它从条件队列移到AQS队列；
     * （3）且只移动一个节点；
     *
     * 1. 如果不是当前线程占有着锁，调用这个方法抛出异常
     * 2. 从条件队列中移除的节点尝试加入至同步队列中
     * 注意，这里调用signal()方法后并不会真正唤醒一个节点，那么，唤醒一个节点是在signal()方法后，最终会执行lock.unlock()方法，
     * 此时才会真正唤醒一个节点，唤醒的这个节点如果曾经是条件节点的话又会继续执行await()方法“分界线”下面的代码。
     */
    // AbstractQueuedSynchronizer.ConditionObject.signal
//    public final void signal() {
//        // 如果不是当前线程占有着锁，调用这个方法抛出异常
//        // 说明signal()也要在获取锁之后执行
//        if (!isHeldExclusively())
//            throw new IllegalMonitorStateException();
//        // 条件队列的头节点
//        Node first = firstWaiter;
//        // 如果有等待条件的节点，则通知它条件已成立
//        if (first != null)
//            doSignal(first);
//    }

    /**
     * 1. 把节点从条件队列中移除
     * 2. 并且尝试把条件队列中移除的节点移动到同步队列中
     * 3. 若是这节点已经取消，transferForSignal直接返回false，重复1、2步骤
     * 4. 若是这节点添加至同步队列成功，则跳出方法
     */
    // AbstractQueuedSynchronizer.ConditionObject.doSignal
//    private void doSignal(Node first) {
//        do {
//            // 移到条件队列的头节点往后一位
//            if ( (firstWaiter = first.nextWaiter) == null)
//                lastWaiter = null;
//            // 相当于把头节点从队列中出队
//            first.nextWaiter = null;
//            // 转移节点到AQS队列中
//        } while (!transferForSignal(first) &&
//                (first = firstWaiter) != null);
//    }

    /**
     * 把条件队列中的节点放到等待队列中
     * 1. 若是这节点已经取消，直接返回false
     * 若是没有取消，尝试将上一个节点的0更改为-1，若是上一个节点已取消了，或者更新状态为SIGNAL失败（也是说明上一个节点已经取消了）则唤醒当前线程
     * 若是尝试成功，返回true跳出循环
     */
    // AbstractQueuedSynchronizer.transferForSignal
//    final boolean transferForSignal(Node node) {
//        // 把节点的状态更改为0，也就是说即将移到AQS队列中
//        // 如果失败了，说明节点已经被改成取消状态了
//        // 返回false，通过上面的循环可知会寻找下一个可用节点
//        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
//            return false;
//
//        // 调用AQS的入队方法把节点移到AQS的队列中
//        // 注意，这里enq()的返回值是node的上一个节点，也就是旧尾节点
//        Node p = enq(node);
//        // 上一个节点的等待状态
//        int ws = p.waitStatus;
//        // 如果上一个节点已取消了，或者更新状态为SIGNAL失败（也是说明上一个节点已经取消了）
//        // 则直接唤醒当前节点对应的线程
//        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
//            LockSupport.unpark(node.thread);
//        // 如果更新上一个节点的等待状态为SIGNAL成功了
//        // 则返回true，这时上面的循环不成立了，退出循环，也就是只通知了一个节点
//        // 此时当前节点还是阻塞状态
//        // 也就是说调用signal()的时候并不会真正唤醒一个节点
//        // 只是把节点从条件队列移到AQS队列中
//        return true;
//    }
}
