package sort;

import java.util.NoSuchElementException;

/**
 * @author ywt start
 * @create 2022-06-20 22:56
 */
public class Shell<T extends Comparable<T>> extends Example<T> {

    public void sort(T[] nums) {
        if(nums == null){
            throw new NoSuchElementException("没有元素可以排序");
        }
        int length = nums.length;

        if(length <= 1){
            return;
        }

        int h = 1;
        while(h < length/3){
            h = 3*h + 1;
        }

        while(h >= 1){
            for(int i = h;i < length;i++){
                for(int j = i;j >= h && less(nums[j],nums[j - h]); j -= h){
                    swap(nums,j,j - h);
                }
            }
            h = h / 3;
        }
    }

//    public static void main(String[] args) {
//        Integer arr[] = {2, 4, 7, 6, 8, 5, 9,13,11};
//        Shell shell = new Shell();
//        shell.sort(arr);
//        shell.show(arr);
//        System.out.println(shell.isSorted(arr));
//    }
}
