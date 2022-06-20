package sort;

/**
 * @author ywt start
 * @create 2022-06-20 22:52
 */
public abstract class Example<T extends Comparable<T>> {


    protected boolean less(T v, T w) {
        return v.compareTo(w) < 0;
    }

    protected void swap(T[] a, int i, int j) {
        T t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
