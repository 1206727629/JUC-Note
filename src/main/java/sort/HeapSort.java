package sort;

import java.util.NoSuchElementException;

/**
 * @author ywt start
 * @create 2022-06-20 23:00
 */
public class HeapSort<T extends Comparable<T>> extends Example<T> {

    public void sort(T[] nums) {
        if (nums == null) {
            throw new NoSuchElementException("没有元素可以排序");
        }
        int length = nums.length;

        if (length <= 1) {
            return;
        }
        int N = nums.length - 1;

        for(int k = N / 2;k >= 1;k--)//遍历一半的元素进行下沉操作使数组中的元素堆有序
            sink(nums,k,N);

        while(N > 1){//从最后一个元素开始，一直和最顶点元素交换
            swap(nums,1,N--);
            sink(nums,1,N);
        }

    }

    private void sink(T[] nums, int k, int N) {//下沉
        while(2 * k <= N){
            int j = 2 * k;
            if(j < N && less(nums,j, j + 1))
                j++;
            if (!less(nums, k, j))
                break;
            swap(nums, k, j);
            k = j;
        }
    }

    private boolean less(T[] nums, int i, int j) {
        return nums[i].compareTo(nums[j]) < 0;
    }

//    public static void main(String[] args) {
//
//        Integer arr[] = {2, 4, 7, 6, 8, 5, 9,13,11};
//        HeapSort heapSort = new HeapSort();
//        heapSort.sort(arr);
//        heapSort.show(arr);
//        System.out.println(heapSort.isSorted(arr));
//    }

}
