package thread.pool.category.submit;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * @Author yangwentian5
 * @Date 2022/3/23 11:32
 */
public class ThreadPool02 {

    /**
     * submit方法，它是提交有返回值任务的一种方式，内部使用未来任务（FutureTask）包装，
     * 再交给execute()去执行，最后返回未来任务本身。
     * @param task
     * @param <T>
     * @return
     */
    public <T> Future<T> submit(Callable<T> task) {
        // 非空检测
        if (task == null) {
            throw new NullPointerException();
        }
        // 包装成FutureTask
        RunnableFuture<T> ftask = newTaskFor(task);
        // 交给execute()方法去执行
//        execute(ftask);
        // 返回futureTask
        return ftask;
    }

    /**
     * 这里的设计很巧妙，实际上这两个方法都是在AbstractExecutorService这个抽象类中完成的，这是模板方法的一种运用。
     *
     * FutureTask实现了RunnableFuture接口，而RunnableFuture接口组合了Runnable接口和Future接口的能力，而Future接口提供了get任务返回值的能力。
     *
     * 问题：submit()方法返回的为什么是Future接口而不是RunnableFuture接口或者FutureTask类呢？
     * 答：这是因为submit()返回的结果，对外部调用者只想暴露其get()的能力（Future接口），而不想暴露其run()的能力（Runaable接口）。
     * @param callable
     * @param <T>
     * @return
     */
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        // 将普通任务包装成FutureTask
        return new FutureTask<T>(callable);
    }
}
