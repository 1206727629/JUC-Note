package queue;

/**
 * @Author yangwentian5
 * @Date 2022/3/31 17:07
 */
public class TestSynchronousQueue {
    public static void main(String[] args) throws InterruptedException {
        java.util.concurrent.SynchronousQueue<Integer> queue = new java.util.concurrent.SynchronousQueue<>(false);

        new Thread(()->{
            try {
                queue.put(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();


        Thread.sleep(500);
        System.out.println(queue.take());
    }
}
