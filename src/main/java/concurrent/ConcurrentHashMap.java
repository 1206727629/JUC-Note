package concurrent;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author yangwentian5
 * @Date 2022/3/7 18:37
 *
 * 总结
 * （1）ConcurrentHashMap是HashMap的线程安全版本；
 * （2）ConcurrentHashMap采用（数组 + 链表 + 红黑树）的结构存储元素；
 * （3）ConcurrentHashMap相比于同样线程安全的HashTable，效率要高很多；
 * （4）ConcurrentHashMap采用的锁有 synchronized，CAS，自旋锁，分段锁，volatile等；
 * （5）ConcurrentHashMap中没有threshold和loadFactor这两个字段，而是采用sizeCtl来控制；
 * （6）sizeCtl = -1，表示正在进行初始化；
 * （7）sizeCtl = 0，默认值，表示后续在真正初始化的时候使用默认容量；
 * （8）sizeCtl > 0，在初始化之前存储的是传入的容量，在初始化或扩容后存储的是下一次的扩容门槛；
 * （9）sizeCtl = (resizeStamp << 16) + (1 + nThreads)，表示正在进行扩容，高位存储扩容邮戳，低位存储扩容线程数加1；
 * （10）更新操作时如果正在进行扩容，当前线程协助扩容；
 * （11）更新操作会采用synchronized锁住当前桶的第一个元素，这是分段锁的思想；
 * （12）整个扩容过程都是通过CAS控制sizeCtl这个字段来进行的，这很关键；
 * （13）迁移完元素的桶会放置一个ForwardingNode节点，以标识该桶迁移完毕；
 * （14）元素个数的存储也是采用的分段思想，类似于LongAdder的实现；
 * （15）元素个数的更新会把不同的线程hash到不同的段上，减少资源争用；
 * （16）元素个数的更新如果还是出现多个线程同时更新一个段，则会扩容段（CounterCell）；
 * （17）获取元素个数是把所有的段（包括baseCount和CounterCell）相加起来得到的；
 * （18）查询操作是不会加锁的，所以ConcurrentHashMap不是强一致性的；
 * （19）ConcurrentHashMap中不能存储key或value为null的元素；
 *
 *   ConcurrentHashMap中有哪些值得学习的技术呢？
 *   我认为有以下几点：
 *  （1）CAS + 自旋，乐观锁的思想，减少线程上下文切换的时间；
 *  （2）分段锁的思想，减少同一把锁争用带来的低效问题；
 *  （3）CounterCell，分段存储元素个数，减少多线程同时更新一个字段带来的低效；
 *  （4）@sun.misc.Contended（CounterCell上的注解），避免伪共享；（p.s.伪共享我们后面也会讲的^^）
 *  （5）多线程协同进行扩容；
 */
public class ConcurrentHashMap<K,V> {
    private static final int DEFAULT_CAPACITY = 16;

    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 构造方法与HashMap对比可以发现，没有了HashMap中的threshold和loadFactor，而是改用了sizeCtl来控制，而且只存储了容量在里面，那么它是怎么用的呢？官方给出的解释如下：
     * （1）-1，表示有线程正在进行初始化操作
     * （2）-(1 + nThreads)，表示有n个线程正在一起扩容，如 -2 就表示有 2-1 个线程正在扩容
     * （3）0，默认值，表示数组还没有被初始化，后续在真正初始化的时候使用默认容量
     * （4）> 0，初始化或扩容完成后下一次的扩容门槛
     */
    private transient volatile int sizeCtl;

    public ConcurrentHashMap() {
    }

    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException();
        }
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                MAXIMUM_CAPACITY :
                tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException();
        }

        if (initialCapacity < concurrencyLevel) {
            // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        }

        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
                MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }



    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

//    public V put(K key, V value) {
//        return putVal(key, value, false);
//    }

    public V remove(Object key) {
        // 调用替换节点方法
//        return replaceNode(key, null, null);
        return null;
    }

    /**
     * （1）计算hash；
     * （2）如果所在的桶不存在，表示没有找到目标元素，返回；
     * （3）如果正在扩容，则协助扩容完成后再进行删除操作；
     * （4）如果是以链表形式存储的，则遍历整个链表查找元素，找到之后再删除；删除元素也是分段锁synchronized的思想
     * （5）如果是以树形式存储的，则遍历树查找元素，找到之后再删除；
     * （6）如果是以树形式存储的，删除元素之后树较小，则退化成链表；
     * （7）如果确实删除了元素，则整个map元素个数减1，并返回旧值；
     * （8）如果没有删除元素，则返回null；
     */
    //    final V replaceNode(Object key, V value, Object cv) {
//        // 计算hash
//        int hash = spread(key.hashCode());
//        // 自旋
//        for (Node<K,V>[] tab = table;;) {
//            Node<K,V> f; int n, i, fh;
//            if (tab == null || (n = tab.length) == 0 ||
//                    (f = tabAt(tab, i = (n - 1) & hash)) == null)
//                // 如果目标key所在的桶不存在，跳出循环返回null
//                break;
//            else if ((fh = f.hash) == MOVED)
//                // 如果正在扩容中，协助扩容
//                tab = helpTransfer(tab, f);
//            else {
//                V oldVal = null;
//                // 标记是否处理过
//                boolean validated = false;
//                synchronized (f) {
//                    // 再次验证当前桶第一个元素是否被修改过
//                    if (tabAt(tab, i) == f) {
//                        if (fh >= 0) {
//                            // fh>=0表示是链表节点
//                            validated = true;
//                            // 遍历链表寻找目标节点
//                            for (Node<K,V> e = f, pred = null;;) {
//                                K ek;
//                                if (e.hash == hash &&
//                                        ((ek = e.key) == key ||
//                                                (ek != null && key.equals(ek)))) {
//                                    // 找到了目标节点
//                                    V ev = e.val;
//                                    // 检查目标节点旧value是否等于cv
//                                    if (cv == null || cv == ev ||
//                                            (ev != null && cv.equals(ev))) {
//                                        oldVal = ev;
//                                        if (value != null)
//                                            // 如果value不为空则替换旧值
//                                            e.val = value;
//                                        else if (pred != null)
//                                            // 如果前置节点不为空
//                                            // 删除当前节点
//                                            pred.next = e.next;
//                                        else
//                                            // 如果前置节点为空
//                                            // 说明是桶中第一个元素，删除之
//                                            setTabAt(tab, i, e.next);
//                                    }
//                                    break;
//                                }
//                                pred = e;
//                                // 遍历到链表尾部还没找到元素，跳出循环
//                                if ((e = e.next) == null)
//                                    break;
//                            }
//                        } else if (f instanceof TreeBin) {
//                            // 如果是树节点
//                            validated = true;
//                            TreeBin<K,V> t = (TreeBin<K,V>)f;
//                            TreeNode<K,V> r, p;
//                            // 遍历树找到了目标节点
//                            if ((r = t.root) != null &&
//                                    (p = r.findTreeNode(hash, key, null)) != null) {
//                                V pv = p.val;
//                                // 检查目标节点旧value是否等于cv
//                                if (cv == null || cv == pv ||
//                                        (pv != null && cv.equals(pv))) {
//                                    oldVal = pv;
//                                    if (value != null)
//                                        // 如果value不为空则替换旧值
//                                        p.val = value;
//                                    else if (t.removeTreeNode(p))
//                                        // 如果value为空则删除元素
//                                        // 如果删除后树的元素个数较少则退化成链表
//                                        // t.removeTreeNode(p)这个方法返回true表示删除节点后树的元素个数较少
//                                        setTabAt(tab, i, untreeify(t.first));
//                                }
//                            }
//                        }
//                    }
//                }
//                // 如果处理过，不管有没有找到元素都返回
//                if (validated) {
//                    // 如果找到了元素，返回其旧值
//                    if (oldVal != null) {
//                        // 如果要替换的值为空，元素个数减1
//                        if (value == null)
//                            addCount(-1L, -1);
//                        return oldVal;
//                    }
//                    break;
//                }
//            }
//        }
//        // 没找到元素返回空
//        return null;
//    }

    /**
     * （1）hash到元素所在的桶；
     * （2）如果桶中第一个元素就是该找的元素，直接返回；
     * （3）如果是树或者正在迁移元素，则调用各自Node子类的find()方法寻找元素；
     * （4）如果是链表，遍历整个链表寻找元素；
     * （5）获取元素没有加锁；
     */
//    public V get(Object key) {
//        Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
//        // 计算hash
//        int h = spread(key.hashCode());
//        // 如果元素所在的桶存在且里面有元素
//        if ((tab = table) != null && (n = tab.length) > 0 &&
//                (e = tabAt(tab, (n - 1) & h)) != null) {
//            // 如果第一个元素就是要找的元素，直接返回
//            if ((eh = e.hash) == h) {
//                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
//                    return e.val;
//            }
//            else if (eh < 0)
//                // hash小于0，说明是树或者正在扩容
//                // 使用find寻找元素，find的寻找方式依据Node的不同子类有不同的实现方式
//                return (p = e.find(h, key)) != null ? p.val : null;
//
//            // 遍历整个链表寻找元素
//            while ((e = e.next) != null) {
//                if (e.hash == h &&
//                        ((ek = e.key) == key || (ek != null && key.equals(ek))))
//                    return e.val;
//            }
//        }
//        return null;
//    }

    /**
     *  Node为单向链表
     *
     * （1）如果桶数组未初始化，则初始化；
     * （2）如果待插入的元素所在的桶为空，则尝试把此元素直接插入到桶的第一个位置(注意，这里HashMap在多线程环境下，会赋值第一个元素的时候出现元素覆盖的问题，所以ConcurrentHashMap在这里使用了乐观锁保证)；
     * （3）如果正在扩容，则当前线程一起加入到扩容的过程中；
     * （4）如果待插入的元素所在的桶不为空且不在迁移元素，则锁住这个桶（分段锁）；
     * （5）如果当前桶中元素以链表方式存储，则在链表中寻找该元素或者插入元素；
     * （6）如果当前桶中元素以红黑树方式存储，则在红黑树中寻找该元素或者插入元素；
     * （7）如果元素存在，则返回旧值；
     * （8）如果元素不存在，整个Map的元素个数加1，并检查是否需要扩容；
     *
     * 添加元素操作中使用的锁主要有（自旋锁 + CAS + synchronized + 分段锁）
     * @param
     */
    //    final V putVal(K key, V value, boolean onlyIfAbsent) {
//        // key和value都不能为null
//        if (key == null || value == null) throw new NullPointerException();
//        // 计算hash值
//        int hash = spread(key.hashCode());

//        // 要插入的元素所在桶的元素个数
//        int binCount = 0;

//        // 死循环，结合CAS使用（如果CAS失败，则会重新取整个桶进行下面的流程）
//        for (Node<K,V>[] tab = table;;) {
//            Node<K,V> f; int n, i, fh;

//            if (tab == null || (n = tab.length) == 0)
//                // 如果桶未初始化或者桶个数为0，则初始化桶
//                tab = initTable();

            // i是key所在桶的位置
//            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
//                // 如果要插入的元素所在的桶还没有元素，则把这个元素插入到这个桶中
                  // 乐观锁使用地方1：注意，这里HashMap在多线程环境下，会赋值第一个元素的时候出现元素覆盖的问题，所以ConcurrentHashMap在这里使用了乐观锁保证
//                if (casTabAt(tab, i, null,
//                        new Node<K,V>(hash, key, value, null)))
//                    // 如果使用CAS插入元素时，发现已经有元素了，往下走
//                    // 如果使用CAS插入元素成功，则break跳出循环，流程结束
//                    break;                   // no lock when adding to empty bin
//            }

//            else if ((fh = f.hash) == MOVED)
//                // 如果要插入的元素所在的桶的第一个元素的hash是MOVED，则当前线程帮忙一起迁移元素
//                tab = helpTransfer(tab, f);

//            else {
//                // 如果这个桶不为空且不在迁移元素，则锁住这个桶（分段锁）
//                // 并查找要插入的元素是否在这个桶中
//                // 存在，则替换值（onlyIfAbsent=false）
//                // 不存在，则插入到链表结尾或插入树中
//                V oldVal = null;
//                synchronized (f) {
//                    // 再次检测第一个元素是否有变化，如果有变化则进入下一次循环，从头来过
//                    if (tabAt(tab, i) == f) {
//                        // 如果第一个元素的hash值大于等于0（说明不是在迁移，也不是树）
//                        // 那就是桶中的元素使用的是链表方式存储
//                       // TODO至于为什么fh>=0就不是树而是链表，底层的f.hash用的本地方法计算出来的，不知道了
//                        if (fh >= 0) {
//                            // 桶中元素个数赋值为1
//                            binCount = 1;
//                            // 遍历整个桶，每次结束binCount加1
//                            for (Node<K,V> e = f;; ++binCount) {
//                                K ek;
//                                // 节点的哈希值相同同时key也相等
//                                if (e.hash == hash &&
//                                        ((ek = e.key) == key ||
//                                                (ek != null && key.equals(ek)))) {
//                                    // 如果找到了这个元素，则赋值了新值（onlyIfAbsent=false）
//                                    // 并退出循环
//                                    oldVal = e.val;
//                                    // 这里对应着putIfAbsent的话如果，onlyIfAbsent为true，然后有老值就返回老值，无老值就返回null
//                                    if (!onlyIfAbsent)
//                                        e.val = value;
//                                    break;
//                                }

//                                Node<K,V> pred = e;
//                                if ((e = e.next) == null) {
//                                    // 如果到链表尾部还没有找到元素
//                                    // 就把它插入到链表结尾并退出循环
//                                    pred.next = new Node<K,V>(hash, key,
//                                            value, null);
//                                    break;
//                                }
//                            }
//                        } else if (f instanceof TreeBin) {
//                            // 如果第一个元素是树节点
//                            Node<K,V> p;
//                            // 桶中元素个数赋值为2
//                            binCount = 2;
//                            // 调用红黑树的插入方法插入元素
//                            // 如果成功插入则返回null
//                            // 否则返回寻找到的节点
//                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
//                                    value)) != null) {
//                                // 如果找到了这个元素，则赋值了新值（onlyIfAbsent=false）
//                                // 并退出循环
//                                oldVal = p.val;
//                                if (!onlyIfAbsent)
//                                    p.val = value;
//                            }
//                        }
//                    }
//                }
//                // 如果binCount不为0，说明成功插入了元素或者寻找到了元素
//                if (binCount != 0) {
//                    // 如果链表元素个数达到了8，则尝试树化
//                    // 因为上面把元素插入到树中时，binCount只赋值了2，并没有计算整个树中元素的个数
//                    // 所以不会重复树化
//                    if (binCount >= TREEIFY_THRESHOLD)
//                        treeifyBin(tab, i);
//                    // 如果要插入的元素已经存在，则返回旧值
//                    if (oldVal != null)
//                        return oldVal;
//                    // 退出外层大循环，流程结束
//                    break;
//                }
//            }
//        }
//        // 成功插入元素，元素个数加1（是否要扩容在这个里面）
//        addCount(1L, binCount);
//        // 成功插入元素返回null
//        return null;
//    }

    /**
     * 桶的初始化
     *
     * 第二个分支采用了 CAS 操作，因为 SIZECTL 默认为 0，所以这里如果可以替换成功，则当前线程可以执行初始化操作;
     * CAS 失败，说明其他线程抢先一步把 sizeCtl 改为了 -1。
     * 扩容成功之后会把下一次扩容的阈值赋值给 sc，即 sizeClt。
     *
     * （1）乐观锁使用地方2：使用CAS锁控制只有一个线程初始化桶数组；
     * （2）sizeCtl在初始化后存储的是扩容门槛；
     * （3）扩容门槛写死的是桶数组大小的0.75倍，桶数组大小即map的容量，也就是最多存储多少个元素。
     * @param m
     */
//    private final Node<K,V>[] initTable() {
//        Node<K,V>[] tab; int sc;
//        while ((tab = table) == null || tab.length == 0) {
//            if ((sc = sizeCtl) < 0)
//                // 如果sizeCtl<0说明正在初始化或者扩容，让出CPU
//                Thread.yield(); // lost initialization race; just spin
//            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
//                // 如果把sizeCtl原子更新为-1成功，则当前线程进入初始化
//                // 如果原子更新失败则说明有其它线程先一步进入初始化了，则进入下一次循环
//                // 如果下一次循环时还没初始化完毕，则sizeCtl<0进入上面if的逻辑让出CPU
//                // 如果下一次循环更新完毕了，则table.length!=0，退出循环
//                try {
//                    // 再次检查table是否为空，防止ABA问题
                      // 个人感觉有种加时间戳判断的意思
//                    if ((tab = table) == null || tab.length == 0) {
//                        // 如果sc为0则使用默认值16
//                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
//                        // 新建数组
//                        @SuppressWarnings("unchecked")
//                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
//                        // 赋值给table桶数组
//                        table = tab = nt;
//                        // 设置sc为数组长度的0.75倍
//                        // n - (n >>> 2) = n - n/4 = 0.75n
//                        // 可见这里装载因子和扩容门槛都是写死了的
//                        // 这也正是没有threshold和loadFactor属性的原因
//                        sc = n - (n >>> 2);
//                    }
//                } finally {
//                    // 把sc赋值给sizeCtl，这时存储的是扩容门槛
//                    sizeCtl = sc;
//                }
//                break;
//            }
//        }
//        return tab;
//    }

    /**
     * ConcurrentHashMap 中存储数据采用的 Node 数组是采用了 volatile 来修饰的，但是这只能保证数组的引用在不同线程之间是可用的，并不能保证数组内部的元素在各个线程之间也是可见的，
     * 所以这里我们判定某一个下标是否有元素，并不能直接通过下标来访问
     *
     * 通过 tabAt 方法来获取元素，而 tableAt 方法实际上就是一个 CAS 操作
     * @param m
     */
    //    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
            //从对象的指定偏移量处获取变量的引用，使用volatile的加载语义
    //        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    //    }

    /**
     * 每次添加元素后，元素数量加1，并判断是否达到扩容门槛，达到了则进行扩容或协助扩容
     *
     * （1）元素个数的存储方式类似于LongAdder类，存储在不同的段上，减少不同线程同时更新size时的冲突；
     * （2）计算元素个数时把这些段的值及baseCount相加算出总的元素个数；
     * （3）若是当前线程的段更新失败或者CounterCell未初始化，数量强制加1
     * （4）正常情况下sizeCtl存储着扩容门槛，扩容门槛为容量的0.75倍；达到扩容门槛后开始扩容
     * （5）扩容时sizeCtl高位存储扩容邮戳(resizeStamp)，低位存储扩容线程数加1（1+nThreads）；
     * （6）其它线程添加元素后如果发现存在扩容，也会加入的扩容行列中来；
     * @param m
     */
//    private final void addCount(long x, int check) {
//        CounterCell[] as; long b, s;
//        // 这里使用的思想跟LongAdder类是一模一样的（后面会讲）
//        // 把数组的大小存储根据不同的线程存储到不同的段上（也是分段锁的思想）
//        // 并且有一个baseCount，优先更新baseCount，如果失败了再更新不同线程对应的段
//        // 这样可以保证尽量小的减少冲突
//
//        // 乐观锁使用地方3：先尝试把数量加到baseCount上，如果失败再加到分段的CounterCell上（也就是有线程竞争baseCount了）
          // 成功跳过此if
          // BASECOUNT是baseCount的位移
//        if ((as = counterCells) != null ||
//                !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
//            CounterCell a; long v; int m;
//            boolean uncontended = true;
//            // 如果as(counterCells)为空
//            // 或者长度为0
//            // 或者当前线程所在的段为null
//            // 乐观锁使用地方4：或者在当前线程的段上加数量失败，也就是有线程竞争
//            if (as == null || (m = as.length - 1) < 0 ||
//                    (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
//                    !(uncontended =
//                            U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
//                // 强制增加数量（无论如何数量是一定要加上的，并不是简单地自旋）
//                // 不同线程对应不同的段都更新失败了，说明已经发生冲突了，
//                // 那么就对counterCells进行扩容，以减少多个线程hash到同一个段的概率
//                fullAddCount(x, uncontended);
//                return;
//            }
//            if (check <= 1)
//                return;
//            // 计算元素个数
//            s = sumCount();
//        }
          // 尝试把数量加到baseCount上成功了走下面逻辑
          // 链表长度大于等于0才开始扩容
//        if (check >= 0) {
//            Node<K,V>[] tab, nt; int n, sc;
//            // 如果元素个数达到了扩容门槛，则进行扩容
//            // 注意，正常情况下sizeCtl存储的是扩容门槛，即容量的0.75倍
//            while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
//                    (n = tab.length) < MAXIMUM_CAPACITY) {
//                // rs是扩容时的一个邮戳标识，通过桶数组的个数计算而得
//                int rs = resizeStamp(n);
//                if (sc < 0) {
//                    // sc<0说明正在扩容中
                      //下面的这个if条件就是在判断什么时候结束扩容
                      // (sc >>> RESIZE_STAMP_SHIFT) != rs 这个条件实际上有 bug，在 JDK12 中已经换掉。
                     // sc == rs + 1 表示最后一个扩容线程正在执行首位工作，也代表扩容结束。
                    // sc == rs + MAX_RESIZERS 表示当前已经达到最大扩容线程数，所以不能继续让线程加入扩容。
                    //  nextTable（扩容的新数组） 如果为 null。
                    // transferIndex <= 0 表示当前可供扩容的下标已经全部分配完毕，也代表了当前线程扩容结束。
//                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
//                            sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
//                            transferIndex <= 0)
//                        // 扩容已经完成了，退出循环
//                        // 正常应该只会触发nextTable==null这个条件，其它条件没看出来何时触发
//                        break;
//
//                    // 扩容未完成，则当前线程加入迁移元素中
//                    // 并把扩容线程数加1
//                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
//                        transfer(tab, nt);
//                }
                  // 将 rs 左移 16 位就刚好使得最高位为 1，此时低 16 位全部是 0，而因为低 16 位要记录扩容线程数，所以应该 +1，但是这里是 +2，
                  // 原因是 sizeCtl 中 -1 这个数值已经被使用了，用来代替当前有线程准备扩容，所以如果直接 +1 是会和标志位发生冲突
                  // 只有初始化第一次记录扩容线程数的时候才需要 +2，后面就都是 +1了
//                else if (U.compareAndSwapInt(this, SIZECTL, sc,
//                        (rs << RESIZE_STAMP_SHIFT) + 2))// 乐观锁使用地方7：初始化sc，保证一个线程起初进入扩容
//                    // 这里是触发扩容的那个线程进入的地方
//                    // sizeCtl的高16位存储着rs这个扩容邮戳
//                    // sizeCtl的低16位存储着扩容线程数加1，即(1+nThreads)
//
//                    // 进入迁移元素
//                    transfer(tab, null);
//                // 重新计算元素个数
//                s = sumCount();
//            }
//        }
//    }

    /**
     * Integer.numberOfLeadingZeros那就是获取当前数据转成二进制后的最高非 0 位前的 0 的个数
     * 16 转成二进制是 10000，最高非 0 位是在第 5 位，因为 int 类型是 32 位，所以他前面还有 27 位，而且都是 0，那么这个方法得到的结果就是 27(1 的前面还有 27 个 0)
     *
     * 1 << (RESIZE_STAMP_BITS - 1)在当前版本就是 1<<15，也就是得到一个二进制数 1000000000000000，这里也是要做一件事，把这个 1 移动到第 16 位.
     * 这里之所以要保证第 16 位为 1，是为了保证 sizeCtl 变量为负数
     *
     * n 的默认大小是 16（ConcurrentHashMap 默认大小），所以实际上最多也就是 27（11011），也就是说这个数最高位的 1 也只是在第五位，执行 | 运算最多也就是影响低 5 位的结果
     */
//    static final int resizeStamp(int n) {
//        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
//    }

    /**
     * 注意这里面只包含了对 CounterCell 数组的初始化和赋值等操作
     * (1) CounterCell数组为空，则先进行初始化，默认是一个长度为 2 的数组，然后把当前线程对应的段赋值
     * (2) 如果再次调用 put 方法，判断了 CounterCell 数组不为空，然后会再次判断该线程在数组对应的位置是不是为空，如果为空，就需要初始化一个 CounterCell 对象放到数组；
     *     而如果元素不为空，则只需要 CAS 操作替换元素中的数量即可。两种情况任一完成都会跳出方法
     * (3) 当前 CounterCell 数组已经初始化完成。当前通过 hash 计算出来的 CounterCell 数组下标中的元素不为 null、并且是
     *     没有创建新的 CounterCell 数组，且当前 CounterCell 数组的小于 CPU 数量的前提下
     *     直接通过 CAS 操作修改 CounterCell 数组中指定下标位置中对象的数量失败(说明有其他线程在竞争修改同一个数组下标中的元素)
     *     则表示需要扩容，扩容2倍，迁移数据，直接把前一半的数据迁移过来
     */
    //    private final void fullAddCount(long x, boolean wasUncontended) {
//        int h;
//        if ((h = ThreadLocalRandom.getProbe()) == 0) {
//            ThreadLocalRandom.localInit();      // force initialization
//            h = ThreadLocalRandom.getProbe();
//            wasUncontended = true;
//        }
//        boolean collide = false;                // 是否需要扩容标识
//        for (;;) {
//            CounterCell[] as; CounterCell a; int n; long v;
              // counterCells不为空，则表示初始化了
//            if ((as = counterCells) != null && (n = as.length) > 0) {
                  // 获得当前线程所在as中的位置下标，同时counterCells里没有该线程对应的位置
//                if ((a = as[(n - 1) & h]) == null) {
                      // cellsBusy = 0表示CounterCell没有被操作
//                    if (cellsBusy == 0) {            // Try to attach new Cell
//                        CounterCell r = CounterCell(x); // Optimistic create
                          // 乐观锁使用地方5：cellsBusy由0改为1保证只有一个线程正在操作CounterCell数组，添加第一个元素
//                        if (cellsBusy == 0 &&
//                                U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
//                            boolean created = false;
//                            try {               // Recheck under lock
                                  // 计算下标，并将该线程对应的CounterCell对象放到CounterCell数组对应下标
//                                CounterCell[] rs; int m, j;
//                                if ((rs = counterCells) != null &&
//                                        (m = rs.length) > 0 &&
//                                        rs[j = (m - 1) & h] == null) {
//                                    rs[j] = r;
//                                    created = true;// 是否创建成功标记为true
//                                }
//                            } finally {
//                                cellsBusy = 0;
//                            }
//                            if (created)
//                                break; // 成功放入后跳出循环
//                            continue;           // Slot is now non-empty
//                        }
//                    }
//                    collide = false; // 有线程操作counterCells数组，collide那么置为false
//                }
//                else if (!wasUncontended)       // CAS already known to fail
//                    wasUncontended = true;      // Continue after rehash
                  // 如果当前位置的元素不为空，则通过CAS操作加上数量，并跳出循环
//                else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x)) // 乐观锁使用地方6：保证只有一个线程对counterCells所在的位置+x
//                    break; // 更新成功退出循环
                  // 更新元素值失败，不相等代表有其他线程创建了新的CounterCell数组
                  // 或者当前CounterCell数组大小已经大于等于CPU数量（保证并发数不会操作CPU数量）
//                else if (counterCells != as || n >= NCPU)
//                    collide = false;            // At max size or stale
//                else if (!collide)
                      // 恢复collide状态，需要对CounterCell数组扩容
//                    collide = true;
//                else if (cellsBusy == 0 &&
//                        U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) { // 乐观锁使用地方6：保证起初只有一个线程扩容
//                    try {
                          // 跳到这里需要满足下列条件：
                          // 1. 当前 CounterCell 数组已经初始化完成。
                          // 2. 当前通过 hash 计算出来的 CounterCell 数组下标中的元素不为 null且直接通过 CAS 操作修改 CounterCell 数组中指定下标位置中对象的数量失败，说明有其他线程在竞争修改同一个数组下标中的元素。
                          // 3. 当前没有其他线程创建新的 CounterCell 数组并且当前 CounterCell 数组的小于 CPU 数量
                          // 4. 以上条件满足其一，就需要扩容
//                        if (counterCells == as) {// Expand table unless stale
                              // 扩容大小为扩大2倍
//                            CounterCell[] rs = new CounterCell[n << 1];
                              // 5. 迁移数据，直接把前一半的数据迁移过来了
//                            for (int i = 0; i < n; ++i)
//                                rs[i] = as[i];
//                            counterCells = rs;
//                        }
//                    } finally {
//                        cellsBusy = 0;
//                    }
//                    collide = false;
//                    continue;                   // Retry with expanded table
//                }
//                h = ThreadLocalRandom.advanceProbe(h);
//            }
              // 直接进入初始化阶段
              // cellsBusy，默认是 0，表示当前没有线程在初始化或者扩容。
              // 此线程想扩容，就使用CAS操作把cellsBusy更改为1
//            else if (cellsBusy == 0 && counterCells == as &&
//                    U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
//                boolean init = false;
//                try {                           // Initialize table
                      // as 其实在前面就是把全局变量 CounterCell 数组的赋值，这里之所以再判断一次就是再确认有没有其他线程修改过全局数组 CounterCell
//                    if (counterCells == as) {
                          // 默认构造一个长度为2的数组
//                        CounterCell[] rs = new CounterCell[2];
                          // 计算下标（h是一个随机，x表示添加元素的数量），也就是确定该线程所在的段
//                        rs[h & 1] = new CounterCell(x);
//                        counterCells = rs;
                          // 初始化完成标识
//                        init = true;
//                    }
//                } finally {
                      // 恢复cellsBusy标识
//                    cellsBusy = 0;
//                }
//                if (init)
//                    break;
//            }
//            else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
//                break;                          // Fall back on using base
//        }
//    }

    /**
     * 返回元素个数
     */
//    public int size() {
//        // 调用sumCount()计算元素个数
//        long n = sumCount();
//        return ((n < 0L) ? 0 :
//                (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
//                        (int)n);
//    }

    /**
     *  baseCount加分段锁上的值为总数
     * （1）元素的个数依据不同的线程存在在不同的段里；（见addCounter()分析）
     * （2）计算CounterCell所有段及baseCount的数量之和；
     * （3）获取元素个数没有加锁；
     * @param m
     */
    //    final long sumCount() {
//        CounterCell[] as = counterCells; CounterCell a;
//        long sum = baseCount;
//        if (as != null) {
//            for (int i = 0; i < as.length; ++i) {
//                if ((a = as[i]) != null)
//                    sum += a.value;
//            }
//        }
//        return sum;
//    }

    /**
     * 线程添加元素时发现正在扩容且当前元素所在的桶元素已经迁移完成了，则协助迁移其它桶的元素
     * @param m
     */
//    final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
//        Node<K,V>[] nextTab; int sc;
//        // 如果桶数组不为空，并且当前桶第一个元素为ForwardingNode类型，并且nextTab扩容后的新桶数组不为空
//        // 说明当前桶已经迁移完毕了，才去帮忙迁移
//        // 扩容时会把旧桶的第一个元素置为ForwardingNode，并让其nextTab指向新桶数组
//        if (tab != null && (f instanceof ForwardingNode) &&
//                (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
//            int rs = resizeStamp(tab.length);
//            // sizeCtl<0，说明正在扩容
//            while (nextTab == nextTable && table == tab &&
//                    (sc = sizeCtl) < 0) {
                 // 判断是否达到扩容条件，不达到就退出循环
//                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
//                        sc == rs + MAX_RESIZERS || transferIndex <= 0)
//                    break;
//                // 扩容线程数加1
//                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {// 乐观锁使用地方8：cas设置扩容线程数
//                    // 当前线程帮忙迁移元素
//                    transfer(tab, nextTab);
//                    break;
//                }
//            }
//            return nextTab;
//        }
//        return table;
//    }

    /**
     *  分段扩容法，即每个线程负责一段
     * （1）新桶数组大小是旧桶数组的两倍；
     * （2）迁移元素先从靠后的桶开始；
     * （3）迁移完成的桶在里面放置一ForwardingNode类型的元素，标记该桶迁移完成，也指向了nextTab新桶数组；
     *      当前桶中的元素迁移完成后，旧数组就在数组中放置一个ForwardingNode。读操作或者迭代读时碰到ForwardingNode时，
     *      将操作转发到扩容后的新的table数组上去执行，写操作碰见它时，则尝试帮助扩容
     *
     *      ConcurrentHashMap 中采用的是分段扩容法，每个线程负责一段
     * （4）迁移时根据hash&n是否等于0把桶中元素分化成两个链表或树；
     * （5）低位链表（树）存储在原来的位置；
     * （6）高位链表（树）存储在原来的位置加n的位置；
     * （7）迁移元素时会锁住当前桶，也是分段锁的思想；
     * @param m
     */
//    private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
//        int n = tab.length, stride;
          // 非单核应用如果n/8在除以NCPU得到的数小于16，则只会有一个线程扩容
//        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
             // 默认每个线程最低位数为16
//            stride = MIN_TRANSFER_STRIDE; // subdivide range
//        if (nextTab == null) {            // initiating
//            // nextTab是扩容后的新数组，如果nextTab为空，说明还没开始迁移
//            // 就新建一个新桶数组
//            try {
//                // 新桶数组是原桶的两倍
//                @SuppressWarnings("unchecked")
//                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
//                nextTab = nt;
//            } catch (Throwable ex) {      // try to cope with OOME
//                sizeCtl = Integer.MAX_VALUE; // 扩容失败后将sizeCtl设置为最大值，也就是不再触发扩容
//                return;
//            }
//            nextTable = nextTab;
//            transferIndex = n; // 表示转移数据的下标，默认为旧数组大小
//        }
//        // 新桶数组大小
//        int nextn = nextTab.length;
//        // 新建一个ForwardingNode类型的节点，并把新桶数组存储在里面
//        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
//        boolean advance = true;
//        boolean finishing = false; // to ensure sweep before committing nextTab
//        for (int i = 0, bound = 0;;) {
//            Node<K,V> f; int fh;
              // 要确认当前线程负责的槽位，确认好之后会从大到小开始往前推进，比如线程一负责 1-16，那么对应的数组边界就是 0-15，然后会从最后一位 15 开始迁移数据
//            // 整个while循环就是在算i的值，过程太复杂，不用太关心
//            // i的值会从n-1依次递减，感兴趣的可以打下断点就知道了
//            // 其中n是旧桶数组的大小，也就是说i从15开始一直减到1这样去迁移元素
//            while (advance) {
//                int nextIndex, nextBound;
//                if (--i >= bound || finishing)
//                    advance = false;
//                else if ((nextIndex = transferIndex) <= 0) {
//                    i = -1;
//                    advance = false;
//                }
//                else if (U.compareAndSwapInt
//                        (this, TRANSFERINDEX, nextIndex,
//                                nextBound = (nextIndex > stride ?
//                                        nextIndex - stride : 0))) { // 乐观锁使用地方9：使用TRANSFERINDEX控制每一个线程确认当前的槽位
//                    bound = nextBound;
//                    i = nextIndex - 1;
//                    advance = false;
//                }
//            }
//            if (i < 0 || i >= n || i + n >= nextn) {
//                // 如果一次遍历完成了
//                // 也就是整个map所有桶中的元素都迁移完成了
//                int sc;
//                if (finishing) {
//                    // 如果全部迁移完成了，则替换旧桶数组
//                    // 并设置下一次扩容门槛为新桶数组容量的0.75倍
//                    nextTable = null;
//                    table = nextTab;
                      // 扩容完成后，对于sizeCtl不需要cas更新扩容门槛值
//                    sizeCtl = (n << 1) - (n >>> 1);
//                    return;
//                }
//                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) { // 乐观锁使用地方10：扩容完成后，扩容线程数-1
//                    // 当前线程扩容完成，把扩容线程数-1
//                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
//                        // 扩容完成两边肯定相等
//                        return;
//                    // 把finishing设置为true
//                    // finishing为true才会走到上面的if条件
//                    finishing = advance = true;
//                    // i重新赋值为n
//                    // 这样会再重新遍历一次桶数组，看看是不是都迁移完成了
//                    // 也就是第二次遍历都会走到下面的(fh = f.hash) == MOVED这个条件
//                    i = n; // recheck before commit
//                }
//            }
              // ForwardingNode节点是Node节点的子类，hash值固定为-1，只在扩容 transfer的时候出现，当旧数组中全部的节点都迁移到新数组中时，
              // 旧数组就在数组中放置一个ForwardingNode。读操作或者迭代读时碰到ForwardingNode时，将操作转发到扩容后的新的table数组上去执行，写操作碰见它时，则尝试帮助扩容
//            else if ((f = tabAt(tab, i)) == null)
//                // 如果桶中无数据，直接放入ForwardingNode标记该桶已迁移
//                advance = casTabAt(tab, i, null, fwd); // 乐观锁使用地方11：不同的线程一直扩容迁移数据，如果发现key对应的桶种无数据，直接放入ForwardingNode标记该桶已迁移
//            else if ((fh = f.hash) == MOVED)
//                // 如果桶中第一个元素的hash值为MOVED
//                // 说明它是ForwardingNode节点
//                // 也就是该桶已迁移
//                advance = true; // already processed
//            else {
//                // 锁定该桶并迁移元素
//                synchronized (f) {
//                    // 再次判断当前桶第一个元素是否有修改
//                    // 也就是可能其它线程先一步迁移了元素
//                    if (tabAt(tab, i) == f) {
//                        // 1. 把一个链表分化成两个链表
//                        // 2. 规则是桶中各元素的hash与桶大小n进行与操作
//                        // 3. 等于0的放到低位链表(low)中，不等于0的放到高位链表(high)中
//                        // 4. 其中低位链表迁移到新桶中的位置相对旧桶不变
//                        // 5. 高位链表迁移到新桶中位置正好是其在旧桶的位置加n
//                        // 6. 这也正是为什么扩容时容量在变成两倍的原因
//                        Node<K,V> ln, hn;
//                        if (fh >= 0) {
//                            // 第一个元素的hash值大于等于0
//                            // 说明该桶中元素是以链表形式存储的
//                            // 这里与HashMap迁移算法基本类似
//                            // 唯一不同的是多了一步寻找lastRun
//                            // 这里的lastRun是提取出链表后面不用处理再特殊处理的子链表
//                            // 比如所有元素的hash值与桶大小n与操作后的值分别为 0 0 4 4 0 0 0
//                            // 则最后后面三个0对应的元素肯定还是在同一个桶中
//                            // 这时lastRun对应的就是倒数第三个节点
//                            // 至于为啥要这样处理，我也没太搞明白
//                            int runBit = fh & n;
//                            Node<K,V> lastRun = f;
//                            for (Node<K,V> p = f.next; p != null; p = p.next) {
//                                int b = p.hash & n;
//                                if (b != runBit) {
//                                    runBit = b;
//                                    lastRun = p;
//                                }
//                            }
//                            // 看看最后这几个元素归属于低位链表还是高位链表
//                            if (runBit == 0) {
//                                ln = lastRun;
//                                hn = null;
//                            }
//                            else {
//                                hn = lastRun;
//                                ln = null;
//                            }
//                            // 遍历链表，把hash&n为0的放在低位链表中
//                            // 不为0的放在高位链表中
//                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
//                                int ph = p.hash; K pk = p.key; V pv = p.val;
//                                if ((ph & n) == 0)
//                                    ln = new Node<K,V>(ph, pk, pv, ln);
//                                else
//                                    hn = new Node<K,V>(ph, pk, pv, hn);
//                            }
//                            // 低位链表的位置不变
//                            setTabAt(nextTab, i, ln);
//                            // 高位链表的位置是原位置加n
//                            setTabAt(nextTab, i + n, hn);
//                            // 标记当前桶已迁移
//                            setTabAt(tab, i, fwd);
//                            // advance为true，返回上面进行--i操作
//                            advance = true;
//                        }
//                        else if (f instanceof TreeBin) {
//                            // 如果第一个元素是树节点
//                            // 也是一样，分化成两颗树
//                            // 也是根据hash&n为0放在低位树中
//                            // 不为0放在高位树中
//                            TreeBin<K,V> t = (TreeBin<K,V>)f;
//                            TreeNode<K,V> lo = null, loTail = null;
//                            TreeNode<K,V> hi = null, hiTail = null;
//                            int lc = 0, hc = 0;
//                            // 遍历整颗树，根据hash&n是否为0分化成两颗树
//                            for (Node<K,V> e = t.first; e != null; e = e.next) {
//                                int h = e.hash;
//                                TreeNode<K,V> p = new TreeNode<K,V>
//                                        (h, e.key, e.val, null, null);
//                                if ((h & n) == 0) {
//                                    if ((p.prev = loTail) == null)
//                                        lo = p;
//                                    else
//                                        loTail.next = p;
//                                    loTail = p;
//                                    ++lc;
//                                }
//                                else {
//                                    if ((p.prev = hiTail) == null)
//                                        hi = p;
//                                    else
//                                        hiTail.next = p;
//                                    hiTail = p;
//                                    ++hc;
//                                }
//                            }
//                            // 如果分化的树中元素个数小于等于6，则退化成链表
//                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
//                                    (hc != 0) ? new TreeBin<K,V>(lo) : t;
//                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
//                                    (lc != 0) ? new TreeBin<K,V>(hi) : t;
//                            // 低位树的位置不变
//                            setTabAt(nextTab, i, ln);
//                            // 高位树的位置是原位置加n
//                            setTabAt(nextTab, i + n, hn);
//                            // 标记该桶已迁移
//                            setTabAt(tab, i, fwd);
//                            // advance为true，返回上面进行--i操作
//                            advance = true;
//                        }
//                    }
//                }
//            }
//        }
//    }

    public void putAll(Map<? extends K, ? extends V> m) {
        /**
         * 先来个空实现
         */
    }
}
