package thread.pool.submit;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * @Author yangwentian5
 * @Date 2022/2/28 14:36
 */
public interface FutureExecutor extends Executor {
    <T> Future<T> submit(Callable<T> command);
}
