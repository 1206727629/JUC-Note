package sort;

import java.util.NoSuchElementException;

/**
 * @author ywt start
 * @create 2022-06-20 22:56
 */
public class Insertion<T extends Comparable<T>> extends Example<T> {

    public void sort(T[] nums) {
        if(nums == null){
            throw new NoSuchElementException("没有元素可以排序");
        }
        int length = nums.length;

        if(length <= 1){
            return;
        }

        for(int i = 0;i < length;i++){//从头到尾进行循环
            for(int j = i;j > 0 && less(nums[j],nums[j - 1]);j--){//每次都将当前元素插入到左侧已经排序的数组中，使得插入之后左侧数组依然有序。
                swap(nums,j,j - 1);
            }
        }
    }

//    public static void main(String[] args) {
//        Integer arr[] = {2, 4, 7, 6, 8, 5, 9,13,11};
//        Insertion insertion = new Insertion();
//        insertion.sort(arr);
//        insertion.show(arr);
//        System.out.println(insertion.isSorted(arr));
//    }
}
