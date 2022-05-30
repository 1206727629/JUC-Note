package concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * @Author yangwentian5
 * @Date 2022/3/9 10:18
 * 上面只是举一个简单的例子，我们不能听说ConcurrentHashMap是线程安全的，
 * 就认为它无论什么情况下都是线程安全的，还是那句话尽信书不如无书。
 */
public class ConcurrentHashMapTest {
    private static final Map<Integer, Integer> map = new ConcurrentHashMap();

    /**
     * 这里如果有多个线程同时调用unsafeUpdate()这个方法，ConcurrentHashMap还能保证线程安全吗？
     * 答案是不能。因为get()之后if之前可能有其它线程已经put()了这个元素，这时候再put()就把那个线程put()的元素覆盖了。
     * 那怎么修改呢？
     * @param key
     * @param value
     */
    public void unsafeUpdateFirst(Integer key, Integer value) {
        Integer oldValue = map.get(key);
        if (oldValue == null) {
            map.put(key, value);
        }
    }

    /**
     * 使用putIfAbsent()方法，它会保证元素不存在时才插入元素
     * @param key
     * @param value
     */
    public void safeUpdate(Integer key, Integer value) {
        map.putIfAbsent(key, value);
    }

    /**
     * 那么，如果上面oldValue不是跟null比较，而是跟一个特定的值比如1进行比较怎么办？也就是下面这样：
     */
    public void unsafeUpdateSecond(Integer key, Integer value) {
        Integer oldValue = map.get(key);
        if (oldValue == 1) {
            map.put(key, value);
        }
    }

    /**
     * 这样的话就没办法使用putIfAbsent()方法了。
     * 其实，ConcurrentHashMap还提供了另一个方法叫replace(K key, V oldValue, V newValue)可以解决这个问题。
     * replace(K key, V oldValue, V newValue)这个方法可不能乱用，如果传入的newValue是null，则会删除元素。
     */
    public void safeUpdateSecond(Integer key, Integer value) {
        map.replace(key, 1, value);
    }

    /**
     * 那么，如果if之后不是简单的put()操作，而是还有其它业务操作，之后才是put()，比如下面这样，这该怎么办呢？
     * @param key
     * @param value
     */
    public void unsafeUpdateThird(Integer key, Integer value) {
        Integer oldValue = map.get(key);
        if (oldValue == 1) {
            System.out.println(System.currentTimeMillis());
            /**
             * 其它业务操作
             */
            System.out.println(System.currentTimeMillis());

            map.put(key, value);
        }
    }

    /**
     * 那么，如果if之后不是简单的put()操作，而是还有其它业务操作，之后才是put()，比如下面这样，这该怎么办呢？
     * 这样虽然不太友好，但是最起码能保证业务逻辑是正确的。
     * 当然，这里使用ConcurrentHashMap的意义也就不大了，可以换成普通的HashMap了。
     * @param key
     * @param value
     */
    public void safeUpdateThird(Integer key, Integer value) {
        synchronized (map) {
            Integer oldValue = map.get(key);
            if (oldValue == null) {
                System.out.println(System.currentTimeMillis());
                /**
                 * 其它业务操作
                 */
                System.out.println(System.currentTimeMillis());

                map.put(key, value);
            }
        }
    }
}
