package sort;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author ywt start
 * @create 2022-06-20 22:58
 */
public class QuickSort<T extends Comparable<T>> extends Example<T> {
    public void sort(T[] nums) {
        if(nums == null){
            throw new NoSuchElementException("没有元素可以排序");
        }
        int length = nums.length;
        if(length <= 1){
            return;
        }
        shuffle(nums);//打乱原来数组的顺序
        sort(nums,0,nums.length - 1);
    }

    private void sort(T[] nums, int l, int h) {
        if(h <= l)
            return;
        int length = nums.length;
        int j = partition(nums,l,h);//找到切分的元素
        sort(nums,l,j - 1);//将左半部分nums[l……j-1]排序
        sort(nums,j + 1,h);//将右半部分nums[j+1……h]排序
    }

    private void shuffle(T[] nums) {//注意是私有的方法,将输入的数组元素顺序打乱
        List<T> list = Arrays.asList(nums);
        Collections.shuffle(list);//打乱数组元素的顺序
        list.toArray(nums);
    }

    private int partition(T[] nums, int l, int h) {
        int i = l,j = h + 1;
        T v = nums[l];//假设nums[l]是切分元素
        while(true){
            while(less(nums[++i],v)) if(i == h) break;//从左往右扫描，直到找到大于等于v的
            while(less(v,nums[--j]));//j != l是冗余的。因为切分元素是a[l]，不可能比自己小。从右往左扫描，直到找到小于等于v的
            if(i >= j)
                break;
            swap(nums,i,j);
        }
        swap(nums,j,l);
        return j;
    }

//    public static void main(String[] args) {
//        Integer arr[] = {2, 4, 7, 6, 8, 5, 9,13,11};
//        QuickSort quickSort = new QuickSort();
//        quickSort.sort(arr);
//        quickSort.show(arr);
//        System.out.println(quickSort.isSorted(arr));
//    }
}
