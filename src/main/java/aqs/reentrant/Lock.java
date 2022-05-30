package aqs.reentrant;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @Author yangwentian5
 * @Date 2022/3/4 11:49
 */
public interface Lock {
    // 获取锁
    void lock();

    // 获取锁（可中断）
    void lockInterruptibly() throws InterruptedException;

    // 尝试获取锁，如果没获取到锁，就返回false
    boolean tryLock();

    // 尝试获取锁，如果没获取到锁，就等待一段时间，这段时间内还没获取到锁就返回false
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    // 释放锁
    void unlock();

    // 条件锁
    Condition newCondition();
}
