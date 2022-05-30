package thread.pool.submit;

import thread.pool.excute.DiscardRejectPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * 我们需要一种新的任务，它既具有旧任务的执行能力（run()方法），又具有新任务的返回值能力（get()方法），
 * 所以我们造一个“将来的任务”对提交的任务进行包装，使其具有返回值的能力
 * @Author yangwentian5
 * @Date 2022/2/28 14:36
 */
public class FutureTask<T> implements Runnable, Future<T> {
    /**
     * 任务执行的状态，0未开始，1正常完成，2异常完成
     * 也可以使用volatile+Unsafe实现CAS操作
     */
    private AtomicInteger state = new AtomicInteger(NEW);
    private static final int NEW = 0;
    private static final int FINISHED = 1;
    private static final int EXCEPTION = 2;

    /**
     * 任务执行的结果【本篇文章由公众号“彤哥读源码”原创】
     * 如果执行正常，返回结果为T
     * 如果执行异常，返回结果为Exception
     */
    private Object result;

    /**
     * 调用者线程
     * 也可以使用volatile+Unsafe实现CAS操作
     */
    private AtomicReference<Thread> caller = new AtomicReference<>();

    /**
     * 真正的任务
     */
    private Callable<T> task;

    public FutureTask(Callable<T> task) {
        this.task = task;
    }

    public static void main(String[] args) {
        FutureExecutor threadPool = new MyThreadPoolFutureExecutor("test", 2, 4, new ArrayBlockingQueue<>(6), new DiscardRejectPolicy());
        List<Future<Integer>> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int num = i;
            Future<Integer> future = threadPool.submit(() -> {
                Thread.sleep(1000);
                System.out.println("running: " + num);
                return num;
            });
            list.add(future);
        }

        for (Future<Integer> future : list) {
            try {
                System.out.println("runned: " + future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 它其实就是先执行真正的任务，然后修改状态为完成，
     * 并保存任务的返回值，如果保存了主线程，还要唤醒它。
     */
    @Override
    public void run() {
        // 如果状态不是NEW，说明执行过了，直接返回
        if (state.get() != NEW) {
            return;
        }
        try {
            // 执行任务【本篇文章由公众号“彤哥读源码”原创】
            T r = task.call();
            // CAS更新state的值为FINISHED
            // 如果更新成功，就把r赋值给result
            // 如果更新失败，说明state的值不为NEW了，也就是任务已经执行过了
            if (state.compareAndSet(NEW, FINISHED)) {
                // 需要一个变量来承载任务执行的返回值
                this.result = r;
                // finish()必须放在修改state里面，见下面的分析
                finish();
            }
        } catch (Exception e) {
            // 如果CAS更新state的值为EXCEPTION成功，就把e赋值给result
            // 如果CAS更新失败，说明state的值不为NEW了，也就是任务已经执行过了
            if (state.compareAndSet(NEW, EXCEPTION)) {
                this.result = e;
                // finish()必须放在修改state里面，见下面的分析
                finish();
            }
        }
    }

    /**
     * 如果get()在run()之前执行，那就需要阻塞等待run()执行完毕才能拿到返回值，
     * 所以需要保存调用者（主线程），get()的时候park阻塞住，run()完成了unpark唤醒它来拿返回值。
     */
    private void finish() {
        // 检查调用者是否为空，如果不为空，唤醒它
        // 调用者在调用get()方法的进入阻塞状态
        for (Thread c; (c = caller.get()) != null;) {
            if (caller.compareAndSet(c, null)) {
                LockSupport.unpark(c);
            }
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    /**
     * 如果任务还未执行，就阻塞等待任务的执行；
     * 如果任务已经执行完毕了，直接拿返回值即可；
     * 但是，还有一种情况，get()方法执行的过程中run()方法也在执行，所以get()方法中的每一步都要检查状态的值有没有变化
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {
        int s = state.get();
        // 如果任务还未执行完成，判断当前线程是否要进入阻塞状态
        if (s == NEW) {
            // 标识调用者线程是否被标记过
            boolean marked = false;
            for (;;) {
                // 重新获取state的值
                s = state.get();
                // 如果state大于NEW说明完成了，跳出循环
                if (s > NEW) {
                    break;
                    // 此处必须把caller的CAS更新和park()方法分成两步处理，不能把park()放在CAS里面
                } else if (!marked) {
                    // get方法在run方法之前调用，尝试更新调用者线程
                    // 试想断点停在此处【本篇文章由公众号“彤哥读源码”原创】
                    // 此时state为NEW，让run()方法执行到底，它不会执行finish()中的unpark()方法
                    // 这时打开断点，这里会更新caller成功，但是循环从头再执行一遍发现state已经变了，
                    // 直接在上面的if(s>NEW)处跳出循环了，因为finish()在修改state内部
                    marked = caller.compareAndSet(null, Thread.currentThread());
                } else {
                    // get方法在run方法之后调用，调用者线程更新之后park当前线程
                    // 试想断点停在此处
                    // 此时state为NEW，让run()方法执行到底，因为上面的caller已经设置值了，
                    // 所以会执行finish()方法中的unpark()方法，
                    // 这时再打开断点，这里不会park
                    // 见unpark()方法的注释，上面写得很清楚：
                    // 如果线程执行了park()方法，那么执行unpark()方法会唤醒这个线程
                    // 如果先执行了unpark()方法，那么线程下一次执行park()方法将不会阻塞
                    LockSupport.park();
                }
            }
        }

        // 如果任务已经执行完毕了，直接拿返回值即可
        if (s == FINISHED) {
            return (T) result;
        }
        throw new RuntimeException((Throwable) result);
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
