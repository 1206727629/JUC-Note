package aqs.tool;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Semaphore实现限流
 * @Author yangwentian5
 * @Date 2022/3/22 13:49
 */
public class SemaphoreTest {
    public static final Semaphore SEMAPHORE = new Semaphore(100);
    public static final AtomicInteger failCount = new AtomicInteger(0);
    public static final AtomicInteger successCount = new AtomicInteger(0);

    public static void main(String[] args) {
        for (int i = 0; i < 1000; i++) {
            new Thread(()->seckill()).start();
        }
    }

    public static boolean seckill() {
        if (!SEMAPHORE.tryAcquire()) {
            System.out.println("no permits, count="+failCount.incrementAndGet());
            return false;
        }

        try {
            // 处理业务逻辑
            Thread.sleep(2000);
            System.out.println("seckill success, count="+successCount.incrementAndGet());
        } catch (InterruptedException e) {
            // todo 处理异常
            e.printStackTrace();
        } finally {
            SEMAPHORE.release();
        }
        return true;
    }
}
