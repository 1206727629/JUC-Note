package thread.pool.excute.demo;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * 线程池次级接口，对Executor做了一些扩展，主要增加了关闭线程池、
 * 执行有返回值任务、批量执行任务的方法
 * @Author yangwentian5
 * @Date 2022/2/28 15:59
 */
public interface ExecutorService extends Executor {
    //  TODO 有个线程池的类图，需要记下来

    // 关闭线程池，不再接受新任务，但已经提交的任务会执行完成
    void shutdown();

    // 立即关闭线程池，尝试停止正在运行的任务，未执行的任务将不再执行
    // 被迫停止及未执行的任务将以列表的形式返回
    List<Runnable> shutdownNow();

    // 检查线程池是否已关闭
    boolean isShutdown();

    // 检查线程池是否已终止，只有在shutdown()或shutdownNow()之后调用才有可能为true
    boolean isTerminated();

    // 在指定时间内线程池达到终止状态了才会返回true
    boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException;

    // 执行有返回值的任务，任务的返回值为task.call()的结果
    <T> Future<T> submit(Callable<T> task);

    // 执行有返回值的任务，任务的返回值为这里传入的result
    // 当然只有当任务执行完成了调用get()时才会返回
    <T> Future<T> submit(Runnable task, T result);

    // 执行有返回值的任务，任务的返回值为null
    // 当然只有当任务执行完成了调用get()时才会返回
    Future<?> submit(Runnable task);

    // 批量执行任务，只有当这些任务都完成了这个方法才会返回
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException;

    // 在指定时间内批量执行任务，未执行完成的任务将被取消
    // 这里的timeout是所有任务的总时间，不是单个任务的时间
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
            throws InterruptedException;

    // 返回任意一个已完成任务的执行结果，未执行完成的任务将被取消
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException;

    // 在指定时间内如果有任务已完成，则返回任意一个已完成任务的执行结果，未执行完成的任务将被取消
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;
}