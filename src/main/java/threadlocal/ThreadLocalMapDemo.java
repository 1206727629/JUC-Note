package threadlocal;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ywt start
 * @create 2022-06-18 23:27
 */
public class ThreadLocalMapDemo {
    private final int threadLocalHashCode = nextHashCode();

    private static AtomicInteger nextHashCode =
            new AtomicInteger();

    private static final int HASH_INCREMENT = 0x61c88647;

    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    public static void main(String[] args) {
        ThreadLocalMapDemo threadLocalMapDemo = new ThreadLocalMapDemo();
        for(int i=0;i<5;i++){
            new Thread(() -> {
                System.out.println("threadName:"+Thread.currentThread().getName()+":"+ threadLocalMapDemo.nextHashCode);
            }).start();
        }

        for(int i=0;i<5;i++){
            new Thread(() -> {
                System.out.println("threadName2:"+Thread.currentThread().getName()+":"+ new ThreadLocalMapDemo().nextHashCode());
            }).start();
        }
    }
}
