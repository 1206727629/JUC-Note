package sort;

/**
 * @author ywt start
 * @create 2022-06-20 22:56
 */
public abstract class MergeSort<T extends Comparable<T>> extends Example<T>  {
    protected T[] aux ;
    protected void merge(T[] nums, int l, int m, int h) {
        int length = nums.length;

        int i = l,j = m + 1;
        for(int k = l;k <= h;k++){//此循环把nums复制到额外的数组空间aux
            aux[k] = nums[k];
        }

        for(int k = l;k <= h;k++){//都要循环一遍
            if(i > m){//左半边用尽，取右半边元素
                nums[k] = aux[j++];
            }
            else if(j > h){//右半边用尽，取左半边元素
                nums[k] = aux[i++];
            }
            else if(less(aux[j],aux[i])){//左半边大于左半边，取右半边
                nums[k] = aux[j++];
            }
            else{//右半边大于左半边，取左半边，注意相等的时候要是i++才能保证稳定性
                nums[k] = aux[i++];
            }
        }
    }
}
