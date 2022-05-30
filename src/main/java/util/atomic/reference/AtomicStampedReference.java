package util.atomic.reference;

/**
 * AtomicStampedReference是java并发包下提供的一个原子类，它能解决其它原子类无法解决的ABA问题
 *
 * @Author yangwentian5
 * @Date 2022/3/29 21:10
 */
public class AtomicStampedReference {

    /**
     * 声明一个Pair类型的变量并使用Unsfae获取其偏移量，存储到pairOffset中
     */
//    private volatile Pair<V> pair;
//    private static final sun.misc.Unsafe UNSAFE = sun.misc.Unsafe.getUnsafe();
//    private static final long pairOffset =
//            objectFieldOffset(UNSAFE, "pair", AtomicStampedReference.class);

    /**
     * 将元素值和版本号绑定在一起，存储在Pair的reference和stamp（邮票、戳的意思）中
     * @param <T>
     */
    private static class Pair<T> {
        final T reference;
        final int stamp;
        private Pair(T reference, int stamp) {
            this.reference = reference;
            this.stamp = stamp;
        }
        static <T> Pair<T> of(T reference, int stamp) {
            return new Pair<T>(reference, stamp);
        }
    }

    /**
     * 构造方法需要传入初始值及初始版本号
     */
//    public AtomicStampedReference(V initialRef, int initialStamp) {
//        pair = Pair.of(initialRef, initialStamp);
//    }

    /**
     * （1）如果元素值和版本号都没有变化，并且和新的也相同，返回true；
     * （2）如果元素值和版本号都没有变化，并且和新的不完全相同，就构造一个新的Pair对象并执行CAS更新pair。
     *
     * 可以看到，java中的实现跟我们上面讲的ABA的解决方法是一致的。
     * 首先，使用版本号控制；
     * 其次，不重复使用节点（Pair）的引用，每次都新建一个新的Pair来作为CAS比较的对象，而不是复用旧的；
     * 最后，外部传入元素值及版本号，而不是节点（Pair）的引用。
     */
//    public boolean compareAndSet(V   expectedReference,
//                                 V   newReference,
//                                 int expectedStamp,
//                                 int newStamp) {
//        // 获取当前的（元素值，版本号）对
//        Pair<V> current = pair;
//        return
//                // 引用没变
//                expectedReference == current.reference &&
//                        // 版本号没变
//                        expectedStamp == current.stamp &&
//                        // 新引用等于旧引用
//                        ((newReference == current.reference &&
//                                // 新版本号等于旧版本号
//                                newStamp == current.stamp) ||
//                                // 构造新的Pair对象并CAS更新
//                                casPair(current, Pair.of(newReference, newStamp)));
//    }

    /**
     *
     */
//    private boolean casPair(Pair<V> cmp, Pair<V> val) {
//        // 调用Unsafe的compareAndSwapObject()方法CAS更新pair的引用为新引用
//        return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
//    }
}
