package sort;

/**
 * @author ywt start
 * @create 2022-06-20 22:58
 */
public class ThreeWayQuickSort<T extends Comparable<T>> extends QuickSort<T> {

    private void sort(T[] nums, int l, int h) {
        int lt = l,i = l + 1,gt = h; // 把lt和gt当成围栏就明白了
        T v = nums[l];// 起始位置作为分隔点
        while(i <= gt){
            int cmp = nums[i].compareTo(v);
            if(cmp < 0) //如果nums[i]比v小
                swap(nums,lt++,i++);//循环未结束时[l,lt-1]是存取小于v的元素,因此要lt++,也因为[lt,i-1]之间存取等于v的元素，因此要i++
            else if(cmp > 0)//如果nums[i]比v大
                swap(nums,i,gt--);//循环未结束时[gt+1,h]是存取大于v的元素,因此要gt--,因为换过来的数未知所以i不能++
            else//如果nums[i]比v相等
                i++;//因为[lt,i-1]之间存取等于v的元素，因此要i++
        }
        sort(nums,l,lt - 1);
        sort(nums,lt + 1,h);
    }

//    public static void main(String[] args) {
//
//        Integer arr[] = {2, 4, 7, 6, 8, 5, 9,13,11};
//        ThreeWayQuickSort threeWayQuickSort = new ThreeWayQuickSort();
//        threeWayQuickSort.sort(arr);
//        threeWayQuickSort.show(arr);
//        System.out.println(threeWayQuickSort.isSorted(arr));
//    }
}