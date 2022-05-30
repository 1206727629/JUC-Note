package util.atomic;

import sun.misc.Unsafe;

/**
 * AtomicInteger是java并发包下面提供的原子类，主要操作的是int类型的整型，通过调用底层Unsafe的CAS等方法实现原子操作
 *
 * 原子操作是指不会被线程调度机制打断的操作，这种操作一旦开始，就一直运行到结束，中间不会有任何线程上下文切换。
 * 原子操作可以是一个步骤，也可以是多个操作步骤，但是其顺序不可以被打乱，也不可以被切割而只执行其中的一部分，将整个操作视作一个整体是原子性的核心特征。
 * 我们这里说的原子操作与数据库ACID中的原子性，笔者认为最大区别在于，数据库中的原子性主要运用在事务中，一个事务之内的所有更新操作要么都成功，
 * 要么都失败，事务是有回滚机制的，而我们这里说的原子操作是没有回滚的，这是最大的区别。
 *
 * （1）AtomicInteger中维护了一个使用volatile修饰的变量value，保证可见性；
 * （2）AtomicInteger中的主要方法最终几乎都会调用到Unsafe的compareAndSwapInt()方法保证对变量修改的原子性。
 * @Author yangwentian5
 * @Date 2022/3/29 20:25
 */
public class AtomicInteger {
    /**
     * （1）使用int类型的value存储值，且使用volatile修饰，volatile主要是保证可见性，即一个线程修改对另一个线程立即可见，主要的实现原理是内存屏障
     * （2）调用Unsafe的objectFieldOffset()方法获取value字段在类中的偏移量，用于后面CAS操作时使用。
     */
    // 获取Unsafe的实例
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // 标识value字段的偏移量
    private static final long valueOffset;
    // 静态代码块，通过unsafe获取value的偏移量
    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                    (AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }
    // 存储int类型值的地方，使用volatile修饰
    private volatile int value;

    /**
     * 调用Unsafe.compareAndSwapInt()方法实现，这个方法有四个参数：
     * （1）操作的对象；
     * （2）对象中字段的偏移量；
     * （3）原来的值，即期望的值；
     * （4）要修改的值；
     * @param expect
     * @param update
     * @return
     */
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    /**
     * 可以看到，这是一个native方法，底层是使用C/C++写的，主要是调用CPU的CAS指令来实现，它能够保证只有当对应偏移量处的字段值是期望值时才更新，即类似下面这样的两步操作：
     * @param var1
     * @param var2
     * @param var4
     * @param var5
     * @return
     */
    // Unsafe中的方法
    public final native boolean compareAndSwapInt(Object var1, long var2, int var4, int var5);
    // 通过CPU的CAS指令可以保证这两步操作是一个整体，也就不会出现多线程环境中可能比较的时候value值是a，而到真正赋值的时候value值可能已经变成b了的问题
//    if(value == expect) {
//        value = newValue;
//    }

    public final int getAndIncrement() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }

    /**
     * getAndIncrement()方法底层是调用的Unsafe的getAndAddInt()方法，这个方法有三个参数：
     * （1）操作的对象；
     * （2）对象中字段的偏移量；
     * （3）要增加的值；
     *
     * 查看Unsafe的getAndAddInt()方法的源码，可以看到它是先获取当前的值，然后再调用compareAndSwapInt()尝试更新对应偏移量处的值，
     * 如果成功了就跳出循环，如果不成功就再重新尝试，直到成功为止，这可不就是（CAS+自旋）的乐观锁机制么^^
     * AtomicInteger中的其它方法几乎都是类似的，最终会调用到Unsafe的compareAndSwapInt()来保证对value值更新的原子性。
     */
    // Unsafe中的方法
//    public final int getAndAddInt(Object var1, long var2, int var4) {
//        int var5;
//        do {
//            var5 = this.getIntVolatile(var1, var2);
//        } while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4));
//
//        return var5;
//    }
}
