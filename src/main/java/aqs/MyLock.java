package aqs;

import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * 我们自己是无法修改对象头信息的，那么我们可不可以用一个变量来代替呢？比如，这个变量的值为1的时候就说明已加锁，变量值为0的时候就说明未加锁，我觉得可行。
 *
 * 我们要保证多个线程对上面我们定义的变量的争用是可控的，所谓可控即同时只能有一个线程把它的值修改为1，
 * 且当它的值为1的时候其它线程不能再修改它的值，这种是不是就是典型的CAS操作，所以我们需要使用Unsafe这个类来做CAS操作。
 *
 * 然后，我们知道在多线程的环境下，多个线程对同一个锁的争用肯定只有一个能成功，那么，其它的线程就要排队，所以我们还需要一个队列。
 *
 * 最后，这些线程排队的时候干嘛呢？它们不能再继续执行自己的程序，那就只能阻塞了，阻塞完了当轮到这个线程的时候还要唤醒，所以我们还需要Unsfae这个类来阻塞（park）和唤醒（unpark）线程。
 * 一个变量、一个队列、执行CAS/park/unpark的Unsafe类
 * @Author yangwentian5
 * @Date 2022/3/3 13:54
 */
public class MyLock {

    /**
     * 这个变量只支持同时只有一个线程能把它修改为1，所以它修改完了一定要让其它线程可见，因此，这个变量需要使用volatile来修饰
     */
    private volatile int state;

    private static Unsafe unsafe;

    private static long tailOffset;

    private static long headOffset;

    private static long stateOffset;

    // 链表头
    private volatile Node head;
    // 链表尾
    private volatile Node tail;

    static final Node EMPTY = new Node();

    // 构造方法
    public MyLock() {
        head = tail = EMPTY;
    }

    /**
     * 放元素的时候都是放到尾部，且可能是多个线程一起放，所以对尾部的操作要CAS更新；
     * 唤醒一个元素的时候从头部开始，但同时只有一个线程在操作，即获得了锁的那个线程，所以对头部的操作不需要CAS去更新。
     */
    private static class Node {
        // 存储的元素为线程
        Thread thread;
        // 前一个节点（可以没有，但实现起来很困难）
        Node prev;
        // 后一个节点
        Node next;

        public Node() {
        }

        public Node(Thread thread, Node prev) {
            this.thread = thread;
            this.prev = prev;
        }
    }

    private static int count = 0;


    public static void main(String[] args) throws Exception {
        MyLock lock = new MyLock();

        CountDownLatch countDownLatch = new CountDownLatch(1000);

        IntStream.range(0, 1000).forEach(i -> new Thread(() -> {
            lock.lock();

            try {
                IntStream.range(0, 10000).forEach(j -> {
                    count++;
                });
            } finally {
                lock.unlock();
            }
//            System.out.println(Thread.currentThread().getName());
            countDownLatch.countDown();
        }, "tt-" + i).start());

        countDownLatch.await();

        System.out.println(count);
    }

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
            // count在类中的偏移地址
            tailOffset = unsafe.objectFieldOffset(MyLock.class.getDeclaredField("tail"));
            headOffset = unsafe.objectFieldOffset(MyLock.class.getDeclaredField("head"));
            stateOffset = unsafe.objectFieldOffset(MyLock.class.getDeclaredField("state"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    // 原子更新tail字段
    private boolean compareAndSetTail(Node expect, Node update) {
        // 唤醒一个元素的时候从头部开始，但同时只有一个线程在操作，即获得了锁的那个线程，所以对头部的操作不需要CAS去更新
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    private boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    /**
     * （1）尝试获取锁，成功了就直接返回；
     *
     * （2）未获取到锁，就进入队列排队；
     *
     * （3）入队之后，再次尝试获取锁；
     *
     * （4）如果不成功，就阻塞；
     *
     * （5）如果成功了，就把头节点后移一位，并清空当前节点的内容，且与上一个节点断绝关系；
     *
     * （6）加锁结束；
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void lock() {
        // 尝试更新state字段，更新成功说明占有了锁
        if (compareAndSetState(0, 1)) {
            return;
        }
        // 未更新成功则入队
        Node node = enqueue();
        Node prev = node.prev;
        // 再次尝试获取锁，需要检测上一个节点是不是head，按入队顺序加锁
        while (node.prev != head || !compareAndSetState(0, 1)) {
            // 未获取到锁，阻塞
            unsafe.park(false, 0L);
        }
        // 下面不需要原子更新，因为同时只有一个线程访问到这里
        // 获取到锁了且上一个节点是head
        // head后移一位
        head = node;
        // 清空当前节点的内容，协助GC
        node.thread = null;
        // 将上一个节点从链表中剔除，协助GC
        node.prev = null;
        prev.next = null;
    }

    /**
     * （1）把state改成0，这里不需要CAS更新，因为现在还在加锁中，只有一个线程去更新，在这句之后就释放了锁；
     *
     * （2）如果有下一个节点就唤醒它；
     *
     * （3）唤醒之后就会接着走上面lock()方法的while循环再去尝试获取锁；
     *
     * （4）唤醒的线程不是百分之百能获取到锁的，因为这里state更新成0的时候就解锁了，之后可能就有线程去尝试加锁了。
     */
    // 解锁
    public void unlock() {
        // 把state更新成0，这里不需要原子更新，因为同时只有一个线程访问到这里
        state = 0;
        // 下一个待唤醒的节点
        Node next = head.next;

        // 下一个节点不为空，就唤醒它
        if (next != null) {
            unsafe.unpark(next.thread);
        }
    }

    // 入队
    private Node enqueue() {
        while (true) {
            // 获取尾节点
            Node t = tail;
            // 构造新节点
            Node node = new Node(Thread.currentThread(), t);
            // 不断尝试原子更新尾节点
            if (compareAndSetTail(t, node)) {
                // 更新尾节点成功了，让原尾节点的next指针指向当前节点
                t.next = node;
                return node;
            }
        }
    }
}
