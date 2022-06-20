package sort;

/**
 * @author ywt start
 * @create 2022-06-20 23:00
 */
public class Heap<T extends Comparable<T>> {
    private T[] heap;
    private int N;

    public Heap(int maxN){
        heap = (T[]) new Comparable[maxN + 1];
    }

    public boolean isEmpty() {
        return N == 0;
    }

    public int size() {
        return N;
    }

    private void swim(int k){//上浮
        while(k > 1 && less(k / 2,k)){
            swap(k / 2,k);
            k = k / 2;
        }
    }

    private void sink(int k){//下沉
        while(2*k <= N ){
            int j = 2 * k;
            if(j < N && less(j,j + 1))//举个例子就知道为什么不是<=了
                j = j + 1;//若是右子结点大，j就是右子结点
            if(less(j,k))//若是父结点大了就可以跳出循环了
                break;
            swap(k,j);
            k = j;
        }
    }

    public void insert(T v) {//插入元素
        heap[++N] = v;
        swim(N);
    }

    public T delMax() {//删除最大元素
        T max = heap[1];//根节点得到最大元素
        swap(1,N--);//将其和最后一个结点交接
        heap[N + 1] = null;//防止越界
        sink(1);//恢复堆的有序性
        return max;
    }

    private boolean less(int i, int j) {//比较数组中两个位置的元素
        return heap[i].compareTo(heap[j]) < 0;
    }

    private void swap(int i, int j) {//交换数组中两个位置的元素
        T t = heap[i];
        heap[i] = heap[j];
        heap[j] = t;
    }

}
