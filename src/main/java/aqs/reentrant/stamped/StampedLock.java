package aqs.reentrant.stamped;

/**
 * @Author yangwentian5
 * @Date 2022/3/11 12:39
 *
 * （1）写锁的使用方式基本一对待；
 * （2）读锁（悲观）的使用方式可以进行升级，通过tryConvertToWriteLock()方式可以升级为写锁；
 * （3）乐观读锁是一种全新的方式，它假定数据没有改变，乐观读之后处理完业务逻辑再判断版本号是否有改变，如果没改变则乐观读成功，如果有改变则转化为悲观读锁重试；
 */
public class StampedLock {
    private double x, y;
    private final java.util.concurrent.locks.StampedLock sl = new java.util.concurrent.locks.StampedLock();

    void move(double deltaX, double deltaY) {
        // 获取写锁，返回一个版本号（戳）
        long stamp = sl.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            // 释放写锁，需要传入上面获取的版本号
            sl.unlockWrite(stamp);
        }
    }

    double distanceFromOrigin() {
        // 乐观读
        long stamp = sl.tryOptimisticRead();
        double currentX = x, currentY = y;
        // 验证版本号是否有变化
        if (!sl.validate(stamp)) {
            // 版本号变了，乐观读转悲观读
            stamp = sl.readLock();
            try {
                // 重新读取x、y的值
                currentX = x;
                currentY = y;
            } finally {
                // 释放读锁，需要传入上面获取的版本号
                sl.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    void moveIfAtOrigin(double newX, double newY) {
        // 获取悲观读锁
        long stamp = sl.readLock();
        try {
            while (x == 0.0 && y == 0.0) {
                // 转为写锁
                long ws = sl.tryConvertToWriteLock(stamp);
                // 转换成功
                if (ws != 0L) {
                    stamp = ws;
                    x = newX;
                    y = newY;
                    break;
                }
                else {
                    // 转换失败
                    sl.unlockRead(stamp);
                    // 获取写锁
                    stamp = sl.writeLock();
                }
            }
        } finally {
            // 释放锁
            sl.unlock(stamp);
        }
    }
}
