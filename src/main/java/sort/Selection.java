package sort;

import java.util.NoSuchElementException;

/**
 * @author ywt start
 * @create 2022-06-20 22:55
 */
public class Selection<T extends Comparable<T>> extends Example<T> {

    public void sort(T[] nums) {
        if(nums == null){
            throw new NoSuchElementException("没有元素可以排序");
        }
        int length = nums.length;

        if(length <= 1){
            return;
        }

        for(int i = 0;i < length - 1;i++){
            int min = i;//把每次循环的第一个元素假设为最小的元素
            for(int j = i + 1;j < length;j++){//内层循环中找到比min更小的把min设置为j
                if(less(nums[j],nums[min]))
                    min = j;
            }
            swap(nums,i,min);
        }
    }


    public static void main(String[] args) {
//        Integer arr[] = {2, 4, 7, 6, 8, 5, 9,13,11};
//        Selection selection = new Selection();
//        selection.sort(arr);
//        selection.show(arr);
//        System.out.println(selection.isSorted(arr));
    }
}
