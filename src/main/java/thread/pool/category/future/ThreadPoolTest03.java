package thread.pool.category.future;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 定时任务总体分为四种：
 * （1）未来执行一次的任务，无返回值；
 * （2）未来执行一次的任务，有返回值；
 * （3）未来按固定频率重复执行的任务；
 * （4）未来按固定延时重复执行的任务；
 *
 * @Author yangwentian5
 * @Date 2022/3/23 16:02
 */
public class ThreadPoolTest03 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 创建一个定时线程池
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(5);

        System.out.println("start: " + System.currentTimeMillis());

        // 执行一个无返回值任务，5秒后执行，只执行一次
        scheduledThreadPoolExecutor.schedule(() -> {
            System.out.println("spring: " + System.currentTimeMillis());
        }, 5, TimeUnit.SECONDS);

        // 执行一个有返回值任务，5秒后执行，只执行一次
        ScheduledFuture<String> future = scheduledThreadPoolExecutor.schedule(() -> {
            System.out.println("inner summer: " + System.currentTimeMillis());
            return "outer summer: ";
        }, 5, TimeUnit.SECONDS);
        // 获取返回值
        System.out.println(future.get() + System.currentTimeMillis());

        // 按固定频率执行一个任务，每2秒执行一次，1秒后执行
        // 任务开始时的2秒后
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
            System.out.println("autumn: " + System.currentTimeMillis());
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        }, 1, 2, TimeUnit.SECONDS);

        // 按固定延时执行一个任务，每延时2秒执行一次，1秒执行
        // 任务结束时的2秒后
        scheduledThreadPoolExecutor.scheduleWithFixedDelay(() -> {
            System.out.println("winter: " + System.currentTimeMillis());
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        }, 1, 2, TimeUnit.SECONDS);
    }
}
