package unsafe;

import sun.misc.Unsafe;
import util.atomic.AtomicInteger;

import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * @Author yangwentian5
 * @Date 2022/3/3 14:44
 */
public class Counter {
    /**
     * park/unpark
     *
     * JVM在上下文切换的时候使用了Unsafe中的两个非常牛逼的方法park()和unpark()。
     *
     * 当一个线程正在等待某个操作时，JVM调用Unsafe的park()方法来阻塞此线程。
     *
     * 当阻塞中的线程需要再次运行时，JVM调用Unsafe的unpark()方法来唤醒此线程。
     *
     * 我们之前在分析java中的集合时看到了大量的LockSupport.park()/unpark()，它们底层都是调用的Unsafe的这两个方法。
     */

    private volatile int count = 0;

    private static long offset;
    private static Unsafe unsafe;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
            // count在类中的偏移地址
            offset = unsafe.objectFieldOffset(Counter.class.getDeclaredField("count"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Counter counter = new Counter();
        ExecutorService threadPool = Executors.newFixedThreadPool(100);

        // 起100个线程，每个线程自增10000次
        IntStream.range(0, 100)
                .forEach(i->threadPool.submit(()-> IntStream.range(0, 10000)
                        .forEach(j->counter.increment())));

        threadPool.shutdown();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 打印1000000
        System.out.println(counter.getCount());

        // 1. 构造方法
        User user1 = new User();
        // 2. Class，里面实际也是反射
        User user2 = User.class.newInstance();
        // 3. 反射
        User user3 = User.class.getConstructor().newInstance();
        // 4. 克隆
        User user4 = (User) user1.clone();
        // 5. 反序列化
        User user5 = unserialize(user1);
        // 6. Unsafe
        User user6 = (User) unsafe.allocateInstance(User.class);

        System.out.println(user1.age);
        System.out.println(user2.age);
        System.out.println(user3.age);
        System.out.println(user4.age);
        System.out.println(user5.age);
        System.out.println(user6.age);
    }

    /**
     * 论实例化一个类的方式？
     *
     * （1）通过构造方法实例化一个类；
     * （2）通过Class实例化一个类；
     * （3）通过反射实例化一个类；
     * （4）通过克隆实例化一个类；
     * （5）通过反序列化实例化一个类；
     * （6）通过Unsafe实例化一个类；
     * @param user1
     * @return
     * @throws Exception
     */
    private static User unserialize(User user1) throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("D://object.txt"));
        oos.writeObject(user1);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("D://object.txt"));
        // 反序列化
        User user5 = (User) ois.readObject();
        ois.close();
        return user5;
    }

    static class User implements Cloneable, Serializable {
        private int age;

        public User() {
            this.age = 10;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    /**
     * 我们通过调用Unsafe的compareAndSwapInt()方法来尝试更新之前获取到的count的值，
     * 如果它没有被其它线程更新过，则更新成功，否则不断重试直到成功为止
     */
    public void increment() {
        int before = count;
        // 失败了就重试直到成功为止
        // 当前对象，count在类中的偏移地址，旧值，新值
        while (!unsafe.compareAndSwapInt(this, offset, before, before + 1)) {
            // 更新失败了，将老值赋给before也未尝不可
            before = count;
        }
    }

    public int getCount() {
        return count;
    }

    // 使用Unsafe抛出异常不需要定义在方法签名上往外抛
    public static void readFileUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        // 通过Unsafe我们可以抛出一个checked异常，同时却不用捕获或在方法签名上定义它
        unsafe.throwException(new IOException());
    }
}
