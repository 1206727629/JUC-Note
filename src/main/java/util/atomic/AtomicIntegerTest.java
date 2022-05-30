package util.atomic;

import java.util.stream.IntStream;

/**
 * @Author yangwentian5
 * @Date 2022/3/29 20:58
 */
public class AtomicIntegerTest {
//    private static int count = 0;
//
//    public static void increment() {
//        count++;
//    }

//    public static void main(String[] args) {
//        IntStream.range(0, 100)
//                .forEach(i->
//                        new Thread(()->IntStream.range(0, 1000)
//                                .forEach(j->increment())).start());
//
//        /**
//         * 这里起了100个线程，每个线程对count自增1000次，你会发现每次运行的结果都不一样，但它们有个共同点就是都不到100000次，所以直接使用int是有问题的
//         */
//        // 这里使用2或者1看自己的机器
//        // 我这里是用run跑大于2才会退出循环
//        // 但是用debug跑大于1就会退出循环了
//        while (Thread.activeCount() > 1) {
//            // 让出CPU
//            Thread.yield();
//        }
//
//        System.out.println(count);
//    }


    /**
     * volatile无法解决这个问题
     *  因为volatile仅有两个作用：
     * （1）保证可见性，即一个线程对变量的修改另一个线程立即可见；
     * （2）禁止指令重排序；
     *
     *  count++实际上是两步操作，第一步是获取count的值，第二步是对它的值加1。
     *  使用volatile是无法保证这两步不被其它线程调度打断的，所以无法保证原子性
     */
//    private static volatile int count = 0;
//
//    public static void increment() {
//        count++;
//    }

    private static java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

    public static void increment() {
        count.incrementAndGet();
    }

    public static void main(String[] args) {
        IntStream.range(0, 100)
                .forEach(i->
                        new Thread(()->IntStream.range(0, 1000)
                                .forEach(j->increment())).start());

        // 这里使用2或者1看自己的机器
        // 我这里是用run跑大于2才会退出循环
        // 但是用debug跑大于1就会退出循环了
        while (Thread.activeCount() > 1) {
            // 让出CPU
            Thread.yield();
        }

        System.out.println(count);
    }
}
