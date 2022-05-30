package thread.pool.category.submit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * （1）如果这里使用普通任务，要怎么写，时间大概是多少？
 * 如果使用普通任务，那么就要把累加操作放到任务里面，而且并不是那么好写（final的问题），
 * 总时间大概是1秒多一点。但是，这样有一个缺点，就是累加操作跟任务本身的内容耦合到一起了，后面如果改成累乘，还要修改任务的内容。
 * （2）如果这里把future.get()放到for循环里面，时间大概是多少？
 * 大概会是5秒多一点，因为每提交一个任务，都要阻塞调用者线程直到任务执行完毕，每个任务执行都是1秒多，所以总时间就是5秒多点。
 *
 * @Author yangwentian5
 * @Date 2022/3/23 11:07
 */
public class ThreadPoolTest02 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 新建一个固定5个线程的线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(5);

        List<Future<Integer>> futureList = new ArrayList<>();
        // 提交5个任务，分别返回0、1、2、3、4
        for (int i = 0; i < 5; i++) {
            int num = i;

            // 任务执行的结果用Future包装
            Future<Integer> future = threadPool.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("return: " + num);
                // 返回值
                return num;
            });

            // 把future添加到list中
            futureList.add(future);
        }

        // 任务全部提交完再从future中get返回值，并做累加
        int sum = 0;
        for (Future<Integer> future : futureList) {
            sum += future.get();
        }

        System.out.println("sum=" + sum);
    }
}
