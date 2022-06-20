package sort;

import java.util.NoSuchElementException;

/**
 * @author ywt start
 * @create 2022-06-20 22:52
 */
public class Bubble<T extends Comparable<T>> extends Example<T> {

    public void sort(T[] nums) {
        if(nums == null){
            throw new NoSuchElementException("没有元素可以排序");
        }
        int length = nums.length;

        if(length <= 1){
            return;
        }

        boolean isSorted = false;//
        for (int i = length - 1; i < length && !isSorted; i--) {//总共执行n次，每次把最大的沉底
            isSorted = true;//提前退出冒泡循环的标志位,即一次比较中没有交换任何元素，这个数组就已经是有序的了
            for (int j = 0; j < i; j++) {
                if (less(nums[j + 1], nums[j])) {//需要交换位置的时候
                    isSorted = false;
                    swap(nums, j, j + 1);
                }
            }
        }
    }
    public static void main(String[] args) {
//        Integer arr[] = {2, 4, 7, 6, 8, 5, 9,13,11};
//        Bubble bubble = new Bubble();
//        bubble.sort(arr);
//        bubble.show(arr);
//        System.out.println(bubble.isSorted(arr));
    }

}
