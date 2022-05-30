package util.atomic.adder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * （1）LongAdder通过base和cells数组来存储值；
 * （2）不同的线程会hash到不同的cell上去更新，减少了竞争；
 * （3）LongAdder的性能非常高，最终会达到一种无竞争的状态；
 *
 * 在longAccumulate()方法中有个条件是n >= NCPU就不会走到扩容逻辑了，而n是2的倍数，那是不是代表cells数组最大只能达到大于等于NCPU的最小2次方？
 *
 * 答案是明确的。因为同一个CPU核心同时只会运行一个线程，而更新失败了说明有两个不同的核心更新了同一个Cell，这时会重新设置更新失败的那个线程的probe值，
 * 这样下一次它所在的Cell很大概率会发生改变，如果运行的时间足够长，最终会出现同一个核心的所有线程都会hash到同一个Cell（大概率，但不一定全在一个Cell上）上去更新，
 * 所以，这里cells数组中长度并不需要太长，达到CPU核心数足够了。
 *
 * 比如，笔者的电脑是8核的，所以这里cells的数组最大只会到8，达到8就不会扩容了。
 *
 * @Author yangwentian5
 * @Date 2022/3/30 16:23
 */
public class LongAdderVSAtomicLongTest {

    /**
     * 运行结果如下：
     *
     * threadCount：1, times：10000000
     * LongAdder elapse：158ms
     * AtomicLong elapse：64ms
     * threadCount：10, times：10000000
     * LongAdder elapse：206ms
     * AtomicLong elapse：2449ms
     * threadCount：20, times：10000000
     * LongAdder elapse：429ms
     * AtomicLong elapse：5142ms
     * threadCount：40, times：10000000
     * LongAdder elapse：840ms
     * AtomicLong elapse：10506ms
     * threadCount：80, times：10000000
     * LongAdder elapse：1369ms
     * AtomicLong elapse：20482ms
     *
     * @param args
     */
    public static void main(String[] args){
        testAtomicLongVSLongAdder(1, 10000000);
        testAtomicLongVSLongAdder(10, 10000000);
        testAtomicLongVSLongAdder(20, 10000000);
        testAtomicLongVSLongAdder(40, 10000000);
        testAtomicLongVSLongAdder(80, 10000000);
    }

    static void testAtomicLongVSLongAdder(final int threadCount, final int times){
        try {
            System.out.println("threadCount：" + threadCount + ", times：" + times);
            long start = System.currentTimeMillis();
            testLongAdder(threadCount, times);
            System.out.println("LongAdder elapse：" + (System.currentTimeMillis() - start) + "ms");

            long start2 = System.currentTimeMillis();
            testAtomicLong(threadCount, times);
            System.out.println("AtomicLong elapse：" + (System.currentTimeMillis() - start2) + "ms");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void testAtomicLong(final int threadCount, final int times) throws InterruptedException {
        AtomicLong atomicLong = new AtomicLong();
        List<Thread> list = new ArrayList<>();
        for (int i=0;i<threadCount;i++){
            list.add(new Thread(() -> {
                for (int j = 0; j<times; j++){
                    atomicLong.incrementAndGet();
                }
            }));
        }

        for (Thread thread : list){
            thread.start();
        }

        for (Thread thread : list){
            thread.join();
        }
    }

    static void testLongAdder(final int threadCount, final int times) throws InterruptedException {
        java.util.concurrent.atomic.LongAdder longAdder = new java.util.concurrent.atomic.LongAdder();
        List<Thread> list = new ArrayList<>();
        for (int i=0;i<threadCount;i++){
            list.add(new Thread(() -> {
                for (int j = 0; j<times; j++){
                    longAdder.add(1);
                }
            }));
        }

        for (Thread thread : list){
            thread.start();
        }

        for (Thread thread : list){
            thread.join();
        }
    }
}
