package util.atomic.reference;

import java.util.concurrent.locks.LockSupport;

/**
 * @Author yangwentian5
 * @Date 2022/3/29 21:14
 *
 * thread 1 read value: 1
 * thread 2 read value: 1
 * thread 2 update from 1 to 2
 * thread 2 read value: 2
 * thread 2 update from 2 to 1
 * thread 1 update from 1 to 3
 */
public class ABATest {

    public static void main(String[] args) {
        java.util.concurrent.atomic.AtomicInteger atomicInteger = new java.util.concurrent.atomic.AtomicInteger(1);

        new Thread(()->{
            int value = atomicInteger.get();
            System.out.println("thread 1 read value: " + value);

            // 阻塞1s
            LockSupport.parkNanos(1000000000L);

            if (atomicInteger.compareAndSet(value, 3)) {
                System.out.println("thread 1 update from " + value + " to 3");
            } else {
                System.out.println("thread 1 update fail!");
            }
        }).start();

        new Thread(()->{
            int value = atomicInteger.get();
            System.out.println("thread 2 read value: " + value);
            if (atomicInteger.compareAndSet(value, 2)) {
                System.out.println("thread 2 update from " + value + " to 2");

                // do sth

                value = atomicInteger.get();
                System.out.println("thread 2 read value: " + value);
                if (atomicInteger.compareAndSet(value, 1)) {
                    System.out.println("thread 2 update from " + value + " to 1");
                }
            }
        }).start();
    }
}
