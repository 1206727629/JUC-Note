package thread.pool.excute;

/**
 * @Author yangwentian5
 * @Date 2022/2/23 10:46
 */
public class DiscardRejectPolicy implements RejectPolicy {
    @Override
    public void reject(Runnable task, MyThreadPoolExecutor myThreadPoolExecutor) {
        // do nothing
        System.out.println("discard one task");
    }
}
