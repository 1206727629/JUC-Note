package sort;

import java.util.NoSuchElementException;

/**
 * @author ywt start
 * @create 2022-06-20 22:57
 */
public class Up2DownMergeSort<T extends Comparable<T>> extends MergeSort<T> {
    public void sort(T[] nums) {//公有方法
        if (nums == null) {
            throw new NoSuchElementException("没有元素可以排序");
        }
        int length = nums.length;

        if (length <= 1) {
            return;
        }

        aux = (T[]) new Comparable[length];//一定要赋予空间，否则空指针异常，一定要new否则空指针异常，也要用Comparable否则类型转换错误
        sort(nums, 0, length - 1);//调用本类的私有方法
    }

    private void sort(T[] nums, int l, int h) {//注意是私有方法
        if(h <= l)
            return ;
        int m = l + (h - l) / 2;//并不是每个数组的起始位置是0，因此要加l
        sort(nums,l,m);//左半边排序
        sort(nums,m + 1,h);//右半边排序
        merge(nums,l,m,h);//原地归并
    }

//    public static void main(String[] args) {
//        Integer arr[] = {2, 4, 7, 6, 8, 5, 9,13,11};
//        Up2DownMergeSort up2DownMergeSort = new Up2DownMergeSort();
//        up2DownMergeSort.sort(arr);
//        up2DownMergeSort.show(arr);
//        System.out.println(up2DownMergeSort.isSorted(arr));
//    }

}
