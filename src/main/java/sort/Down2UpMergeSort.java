package sort;

import java.util.NoSuchElementException;

/**
 * @author ywt start
 * @create 2022-06-20 22:57
 */
public class Down2UpMergeSort<T extends Comparable<T>> extends MergeSort<T>  {
    public void sort(T[] nums) {
        if (nums == null) {
            throw new NoSuchElementException("没有元素可以排序");
        }
        int length = nums.length;

        if (length <= 1) {
            return;
        }

        aux = (T[]) new Comparable[length];//一定要赋予空间，否则空指针异常
        for(int sz = 1;sz < length;sz = sz + sz){//如果数组长度是16，最后有效的sz是8，可以进行十六十六归并没有问题
            for(int l = 0;l < length - sz;l = l + 2 * sz)//循环比较的是其实位置l，当然是以length - sz为准
                merge(nums,l,l + sz -1,Math.min(length - 1,l + 2 * sz - 1));
        }
    }

//    public static void main(String[] args) {
//        Integer arr[] = {2, 4, 7, 6, 8, 5, 9,13,11};
//        Down2UpMergeSort down2UpMergeSort = new Down2UpMergeSort();
//        down2UpMergeSort.sort(arr);
//        down2UpMergeSort.show(arr);
//        System.out.println(down2UpMergeSort.isSorted(arr));
//    }
}
