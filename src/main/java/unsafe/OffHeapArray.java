package unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Author yangwentian5
 * @Date 2022/3/3 14:35
 */
public class OffHeapArray {
    // 一个int等于4个字节
    private static final int INT = 4;
    private long size;
    private long address;

    private static Unsafe unsafe;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        OffHeapArray offHeapArray = new OffHeapArray(4);
        offHeapArray.set(0, 1);
        offHeapArray.set(1, 2);
        offHeapArray.set(2, 3);
        offHeapArray.set(3, 4);
        offHeapArray.set(2, 5); // 在索引2的位置重复放入元素

        int sum = 0;
        for (int i = 0; i < offHeapArray.size(); i++) {
            sum += offHeapArray.get(i);
        }
        // 打印12
        System.out.println(sum);

        // 使用Unsafe的allocateMemory()我们可以直接在堆外分配内存，这可能非常有用，
        // 但我们要记住，这个内存不受JVM管理，因此我们要调用freeMemory()方法手动释放它
        offHeapArray.freeMemory();
    }

    // 构造方法，分配内存
    public OffHeapArray(long size) {
        this.size = size;
        // 参数字节数
        address = unsafe.allocateMemory(size * INT);
    }

    // 获取指定索引处的元素
    public int get(long i) {
        return unsafe.getInt(address + i * INT);
    }

    // 设置指定索引处的元素
    public void set(long i, int value) {
        unsafe.putInt(address + i * INT, value);
    }

    // 元素个数
    public long size() {
        return size;
    }

    // 释放堆外内存
    public void freeMemory() {
        unsafe.freeMemory(address);
    }
}
