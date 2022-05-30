package thread.pool.category.excute;

import java.util.concurrent.*;

/**
 * 观察num值的打印信息，先是打印了0~4，再打印了10~14，最后打印了5~9，竟然不是按顺序打印的，为什么呢？
 * 线程池的参数：核心数量5个，最大数量10个，任务队列5个。
 * 答：执行前5个任务执行时，正好还不到核心数量，所以新建核心线程并执行了他们；
 * 执行中间的5个任务时，已达到核心数量，所以他们先入队列；
 * 执行后面5个任务时，已达核心数量且队列已满，所以新建非核心线程并执行了他们；
 * 再执行最后5个任务时，线程池已达到满负荷状态，所以执行了拒绝策略。
 *
 * @Author yangwentian5
 * @Date 2022/3/22 23:02
 */
public class ThreadPoolTest01 {

    public static void main(String[] args) {
        // 新建一个线程池
        // 核心数量为5，最大数量为10，空闲时间为1秒，队列长度为5，拒绝策略打印一句话
        ExecutorService threadPool = new ThreadPoolExecutor(5, 10,
                1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5),
                Executors.defaultThreadFactory(), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                System.out.println(currentThreadName() + ", discard task");
            }
        });

        // 提交20个任务，注意观察num
        for (int i = 0; i < 20; i++) {
            int num = i;
            threadPool.execute(()->{
                try {
                    System.out.println(currentThreadName() + ", "+ num + " running, " + System.currentTimeMillis());
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static String currentThreadName() {
        return Thread.currentThread().getName();
    }
}
