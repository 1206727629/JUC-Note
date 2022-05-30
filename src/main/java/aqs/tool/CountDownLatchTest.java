package aqs.tool;

import java.util.concurrent.CountDownLatch;

/**
 * 这段代码分成两段：
 * 第一段，5个辅助线程等待开始的信号，信号由主线程发出，所以5个辅助线程调用startSignal.await()方法等待开始信号，
 *         当主线程的事儿干完了，调用startSignal.countDown()通知辅助线程开始干活。
 * 第二段，主线程等待5个辅助线程完成的信号，信号由5个辅助线程发出，所以主线程调用doneSignal.await()方法等待完成信号，
 *          5个辅助线程干完自己的活儿的时候调用doneSignal.countDown()方法发出自己的完成的信号，当完成信号达到5个的时候，唤醒主线程继续执行后续的逻辑。
 *
 * @Author yangwentian5
 * @Date 2022/3/22 17:00
 */
public class CountDownLatchTest {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            new Thread(()->{
                try {
                    System.out.println("Aid thread is waiting for starting.");
                    startSignal.await();
                    // do sth
                    System.out.println("Aid thread is doing something.");
                    doneSignal.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        // main thread do sth
        Thread.sleep(2000);
        System.out.println("Main thread is doing something.");
        startSignal.countDown();

        // main thread do sth else
        System.out.println("Main thread is waiting for aid threads finishing.");
        doneSignal.await();

        System.out.println("Main thread is doing something after all threads have finished.");
    }
}
