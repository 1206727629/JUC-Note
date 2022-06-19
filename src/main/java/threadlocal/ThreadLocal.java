package threadlocal;

/**
 * @author ywt start
 * @create 2022-06-19 9:37
 */
public class ThreadLocal {
    /**
     * 1. 没有ThreadLocalMap，则创建ThreadLocalMap
     * 2. 有ThreadLocalMap，则取值
     * ThreadLocal.set
     * @param value
     */
//    public void set(T value) {
//        Thread t = Thread.currentThread();
//        ThreadLocalMap map = getMap(t);
//        if (map != null)
//            map.set(this, value);
//        else
//            createMap(t, value);
//    }

    /**
     * 新建的ThreadLocalMap绑定在线程的threadLocals引用里
     * 也就是每个线程都会有自己的ThreadLocalMap
     */
//    void createMap(Thread t, T firstValue) {
//        t.threadLocals = new ThreadLocalMap(this, firstValue);
//    }

    /**
     * ThreadLocalMap#set
     * 1. 如果key值对应的桶中Entry数据不为空
     *      1.1 如果k = key，说明当前set操作是一个替换操作，做替换逻辑，直接返回
     *      1.2 如果key = null，说明当前桶位置的Entry是过期数据，执行replaceStaleEntry()方法(探测式清理)，然后返回
     * 2. 跳出for循环，说明向后迭代的过程中遇到了entry为null的情况，未发现过期元素或覆盖元素，直接set数据到对应的桶中
     * 3. 调用cleanSomeSlots()做一次启发式清理工作，清理散列数组中Entry的key过期的数据
     *      3.1 如果清理工作完成后，未清理到任何数据，且size超过了阈值(数组长度的2/3)，进行rehash()操作
     *      3.2 rehash()中会先进行一轮探测式清理，清理过期key，清理完成后如果size >= threshold - threshold / 4，就会执行真正的扩容逻辑(扩容逻辑往后看)
     * @param key
     * @param value
     */
//    private void set(ThreadLocal<?> key, Object value) {
//
//        // We don't use a fast path as with get() because it is at
//        // least as common to use set() to create new entries as
//        // it is to replace existing ones, in which case, a fast
//        // path would fail more often than not.
//
//        ThreadLocalMap.Entry[] tab = table;
//        int len = tab.length;
//        int i = key.threadLocalHashCode & (len-1);
//
//        for (ThreadLocalMap.Entry e = tab[i];
//             e != null;
//             e = tab[i = nextIndex(i, len)]) {
//            java.lang.ThreadLocal<?> k = e.get();
//
//            if (k == key) {
//                e.value = value;
//                return;
//            }
//
//            if (k == null) {
//                replaceStaleEntry(key, value, i);
//                return;
//            }
//        }
//
//        tab[i] = new ThreadLocalMap.Entry(key, value);
//        int sz = ++size;
//        if (!cleanSomeSlots(i, sz) && sz >= threshold)
//            rehash();
//    }

    /**
     * ThreadLocalMap#replaceStaleEntry
     * 1. slotToExpunge表示开始探测式清理过期数据的开始下标，默认从当前的staleSlot开始。以当前的staleSlot开始，向前迭代查找，
     *    找到没有过期的数据，for循环一直碰到Entry为null才会结束。如果向前找到了过期数据，更新探测清理过期数据的开始下标为i，即slotToExpunge=i
     * 2. 接着开始从staleSlot向后查找，也是碰到Entry为null的桶结束。
     *    如果迭代过程中，碰到k == key，这说明这里是替换逻辑，替换新数据并且交换当前staleSlot位置。
     *    如果slotToExpunge == staleSlot，这说明replaceStaleEntry()一开始向前查找过期数据时并未找到过期的Entry数据，接着向后查找过程中也未发现过期数据，修改开始探测式清理过期数据的下标为当前循环的index，即slotToExpunge = i。
     *    最后调用cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);进行启发式过期数据清理。方法返回
     * 3. k == null说明当前遍历的Entry是一个过期数据，slotToExpunge == staleSlot说明，一开始的向前查找数据并未找到过期的Entry。
     *    如果条件成立，则更新slotToExpunge 为当前位置，这个前提是前驱节点扫描时未发现过期数据。
     * 4. 往后迭代的过程中如果没有找到k == key的数据，且碰到Entry为null的数据，则结束当前的迭代操作。
     *    此时说明这里是一个添加的逻辑，将新的数据添加到table[staleSlot] 对应的slot中。
     * 5. 最后判断除了staleSlot以外，还发现了其他过期的slot数据，就要开启清理数据的逻辑
     */
//    private void replaceStaleEntry(`ThreadLocal`<?> key, Object value,
//                                   int staleSlot) {
//        Entry[] tab = table;
//        int len = tab.length;
//        Entry e;
//
//        int slotToExpunge = staleSlot;
//        for (int i = prevIndex(staleSlot, len);
//             (e = tab[i]) != null;
//             i = prevIndex(i, len))
//
//            if (e.get() == null)
//                slotToExpunge = i;
//
//        for (int i = nextIndex(staleSlot, len);
//             (e = tab[i]) != null;
//             i = nextIndex(i, len)) {
//
//            ThreadLocal<?> k = e.get();
//
//            if (k == key) {
//                e.value = value;
//
//                tab[i] = tab[staleSlot];
//                tab[staleSlot] = e;
//
//                if (slotToExpunge == staleSlot)
//                    slotToExpunge = i;
//                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
//                return;
//            }
//
//            if (k == null && slotToExpunge == staleSlot)
//                slotToExpunge = i;
//        }
//
//        tab[staleSlot].value = null;
//        tab[staleSlot] = new Entry(key, value);
//
//        if (slotToExpunge != staleSlot)
//            cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
//    }

    /**
     * ThreadLocalMap#rehash
     * 1. 先进行一轮探测式清理
     * 2. size超过了3/4再进行扩容
     */
//    private void rehash() {
//        expungeStaleEntries();
//
//        // Use lower threshold for doubling to avoid hysteresis
//        if (size >= threshold - threshold / 4)
//            resize();
//    }

    /**
     * ThreadLocalMap#expungeStaleEntries
     * 遍历发现value为null，就需要清理
     */
//    private void expungeStaleEntries() {
//        Entry[] tab = table;
//        int len = tab.length;
//        for (int j = 0; j < len; j++) {
//            Entry e = tab[j];
//            if (e != null && e.get() == null)
//                expungeStaleEntry(j);
//        }
//    }

        /**
         * ThreadLocalMap#expungeStaleEntry
         * 1. 在遇到entry为null之前的遍历里
         *    遇到过期key就清理或者重新调整e在tab中的位置
         */
//        private int expungeStaleEntry(int staleSlot) {
//            Entry[] tab = table;
//            int len = tab.length;
//
//            // expunge entry at staleSlot
//            tab[staleSlot].value = null;
//            tab[staleSlot] = null;
//            size--;
//
//            // Rehash until we encounter null
//            ThreadLocalMap.Entry e;
//            int i;
//            for (i = nextIndex(staleSlot, len);
//                 (e = tab[i]) != null;
//                 i = nextIndex(i, len)) {
//                java.lang.ThreadLocal<?> k = e.get();
//                if (k == null) {
//                    e.value = null;
//                    tab[i] = null;
//                    size--;
//                } else {
//                    int h = k.threadLocalHashCode & (len - 1);
//                    if (h != i) {
//                        tab[i] = null;
//
//                        // Unlike Knuth 6.4 Algorithm R, we must scan until
//                        // null because multiple entries could have been stale.
//                        while (tab[h] != null)
//                            h = nextIndex(h, len);
//                        tab[h] = e;
//                    }
//                }
//            }
//            return i;
//        }

    /**
     * ThreadLocalMap#resize
     * 扩容后的tab的大小为oldLen * 2，然后遍历老的散列表，重新计算hash位置，然后放到新的tab数组中，
     * 如果出现hash冲突则往后寻找最近的entry为null的槽位，遍历完成之后，oldTab中所有的entry数据都已经放入到新的tab中了
     */
//    private void resize() {
//        Entry[] oldTab = table;
//        int oldLen = oldTab.length;
//        int newLen = oldLen * 2;
//        Entry[] newTab = new Entry[newLen];
//        int count = 0;
//
//        for (int j = 0; j < oldLen; ++j) {
//            Entry e = oldTab[j];
//            if (e != null) {
//                java.lang.ThreadLocal<?> k = e.get();
//                if (k == null) {
//                    e.value = null; // Help the GC
//                } else {
//                    int h = k.threadLocalHashCode & (newLen - 1);
//                    while (newTab[h] != null)
//                        h = nextIndex(h, newLen);
//                    newTab[h] = e;
//                    count++;
//                }
//            }
//        }
//
//        setThreshold(newLen);
//        size = count;
//        table = newTab;
//    }

    /**
     * ThreadLocal#get
     *
     * @return
     */
//    public T get() {
//        Thread t = Thread.currentThread();
//        ThreadLocalMap map = getMap(t);
//        if (map != null) {
//            ThreadLocalMap.Entry e = map.getEntry(this);
//            if (e != null) {
//                @SuppressWarnings("unchecked")
//                T result = (T)e.value;
//                return result;
//            }
//        }
//        return setInitialValue();
//    }

    /**
     * initialValue()就是返回null
     */
//    private T setInitialValue() {
//        T value = initialValue();
//        Thread t = Thread.currentThread();
//        ThreadLocalMap map = getMap(t);
//        if (map != null)
//            map.set(this, value);
//        else
//            createMap(t, value);
//        return value;
//    }

    /**
     * 1. table中若是没有桶冲突，直接返回
     * @param key
     * @param i
     * @param e
     * @return
     */
//    private Entry getEntry(ThreadLocal<?> key) {
//        int i = key.threadLocalHashCode & (table.length - 1);
//        Entry e = table[i];
//        if (e != null && e.get() == key)
//            return e;
//        else
//            return getEntryAfterMiss(key, i, e);
//    }

    /**
     * 有桶冲突的前提下，循环遍历桶元素获取值
     * 若是发现元素过期，则探测式清理
     */
//    private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
//        Entry[] tab = table;
//        int len = tab.length;
//
//        while (e != null) {
//            ThreadLocal<?> k = e.get();
//            if (k == key)
//                return e;
//            if (k == null)
//                expungeStaleEntry(i);
//            else
//                i = nextIndex(i, len);
//            e = tab[i];
//        }
//        return null;
//    }

    /**
     * ThreadLocal#remove
     */
//    public void remove() {
//        ThreadLocalMap m = getMap(Thread.currentThread());
//        if (m != null)
//            m.remove(this);
//    }

    /**
     * ThreadLocalMap#remove
     * 成功删除需要开启探测式清理
     */
//    private void remove(ThreadLocal<?> key) {
//        ThreadLocalMap.Entry[] tab = table;
//        int len = tab.length;
//        int i = key.threadLocalHashCode & (len-1);
//        for (Entry e = tab[i];
//             e != null;
//             e = tab[i = nextIndex(i, len)]) {
//            if (e.get() == key) {
//                e.clear();
//                expungeStaleEntry(i);
//                return;
//            }
//        }
//    }
}
