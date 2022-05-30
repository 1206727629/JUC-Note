package util.atomic.reference;


import java.util.concurrent.locks.LockSupport;

/**
 * 使用AtomicStampedReference解决开篇那个AtomicInteger带来的ABA问题
 *
 * （1）在多线程环境下使用无锁结构要注意ABA问题；
 * （2）ABA的解决一般使用版本号来控制，并保证数据结构使用元素值来传递，且每次添加元素都新建节点承载元素值；
 * （3）AtomicStampedReference内部使用Pair来存储元素值及其版本号；
 *
 * （1）java中还有哪些类可以解决ABA的问题？
 *  AtomicMarkableReference，它不是维护一个版本号，而是维护一个boolean类型的标记，标记值有修改，了解一下
 * @Author yangwentian5
 * @Date 2022/3/30 11:41
 */
public class ABATest03 {
    /**
     * 运行结果
     *
     * thread 1 read value: 1, stamp: 1
     * thread 2 read value: 1, stamp: 1
     * thread 2 update from 1 to 2
     * thread 2 read value: 2, stamp: 2
     * thread 2 update from 2 to 1
     * thread 1 update fail!
     *
     * 可以看到线程1最后更新1到3时失败了，因为这时版本号也变了，成功解决了ABA的问题
     * @param args
     */
    public static void main(String[] args) {
        testStamp();
    }

    private static void testStamp() {
        java.util.concurrent.atomic.AtomicStampedReference<Integer> atomicStampedReference = new java.util.concurrent.atomic.AtomicStampedReference<>(1, 1);

        new Thread(()->{
            int[] stampHolder = new int[1];
            int value = atomicStampedReference.get(stampHolder);
            int stamp = stampHolder[0];
            System.out.println("thread 1 read value: " + value + ", stamp: " + stamp);

            // 阻塞1s
            LockSupport.parkNanos(1000000000L);

            if (atomicStampedReference.compareAndSet(value, 3, stamp, stamp + 1)) {
                System.out.println("thread 1 update from " + value + " to 3");
            } else {
                System.out.println("thread 1 update fail!");
            }
        }).start();

        new Thread(()->{
            int[] stampHolder = new int[1];
            int value = atomicStampedReference.get(stampHolder);
            int stamp = stampHolder[0];
            System.out.println("thread 2 read value: " + value + ", stamp: " + stamp);
            if (atomicStampedReference.compareAndSet(value, 2, stamp, stamp + 1)) {
                System.out.println("thread 2 update from " + value + " to 2");

                // do sth

                value = atomicStampedReference.get(stampHolder);
                stamp = stampHolder[0];
                System.out.println("thread 2 read value: " + value + ", stamp: " + stamp);
                if (atomicStampedReference.compareAndSet(value, 1, stamp, stamp + 1)) {
                    System.out.println("thread 2 update from " + value + " to 1");
                }
            }
        }).start();
    }
}
