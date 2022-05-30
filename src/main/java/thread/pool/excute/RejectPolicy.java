package thread.pool.excute;

/**
 * @Author yangwentian5
 * @Date 2022/2/23 10:47
 */
public interface RejectPolicy {
    void reject(Runnable task, MyThreadPoolExecutor myThreadPoolExecutor);
}
