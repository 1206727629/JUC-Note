package util.atomic.reference;

import java.util.concurrent.atomic.AtomicReference;

/**

 *
 * @Author yangwentian5
 * @Date 2022/3/30 10:44
 */
public class ABATest01 {

    /**
     * 假如，我们初始化栈结构为 top->1->2->3，然后有两个线程分别做如下操作：
     * （1）线程1执行pop()出栈操作，但是执行到if (top.compareAndSet(t, next)) {这行之前暂停了，所以此时节点1并未出栈；
     * （2）线程2执行pop()出栈操作弹出节点1，此时栈变为 top->2->3；
     * （3）线程2执行pop()出栈操作弹出节点2，此时栈变为 top->3；
     * （4）线程2执行push()入栈操作添加节点1，此时栈变为 top->1->3；
     * （5）线程1恢复执行，比较节点1的引用并没有改变，执行CAS成功，此时栈变为 top->2；
     *
     *  What？点解变成 top->2 了？不是应该变成 top->3 吗？
     *  那是因为线程1在第一步保存的next是节点2，所以它执行CAS成功后top节点就指向了节点2了。
     */
    static class Stack {
        // 将top放在原子类中
        private AtomicReference<Node> top = new AtomicReference<>();
        // 栈中节点信息
        static class Node {
            int value;
            Node next;

            public Node(int value) {
                this.value = value;
            }
        }
        // 出栈操作
        public Node pop() {
            for (;;) {
                // 获取栈顶节点
                Node t = top.get();
                if (t == null) {
                    return null;
                }
                // 栈顶下一个节点
                Node next = t.next;
                /**
                 * 在Stack的pop()方法的if (top.compareAndSet(t, next)) {
                 * 处打个断点，线程1运行到这里时阻塞它的执行，让线程2执行完，再执行线程1这句，这句执行完可以看到栈的top对象中只有2这个节点了。
                 * 记得打断点的时候一定要打Thread断点，在IDEA中是右击选择Suspend为Thread。
                 */
                // CAS更新top指向其next节点
                if (top.compareAndSet(t, next)) {
                    // 把栈顶元素弹出，应该把next清空防止外面直接操作栈
                    t.next = null;
                    return t;
                }
            }
        }

        /**
         * ABA的危害我们清楚了，那么怎么解决ABA呢？
         * （1）版本号
         * 比如，上面的栈结构增加一个版本号用于控制，每次CAS的同时检查版本号有没有变过。
         * 还有一些数据结构喜欢使用高位存储一个邮戳来保证CAS的安全。
         * （2）不重复使用节点的引用
         * 比如，上面的栈结构在线程2执行push()入栈操作的时候新建一个节点传入，而不是复用节点1的引用；
         * （3）直接操作元素而不是节点
         * 比如，上面的栈结构push()方法不应该传入一个节点（Node），而是传入元素值（int的value）。
         * @param node
         */
        // 入栈操作
        public void push(Node node) {
            for (;;) {
                // 获取栈顶节点
                Node next = top.get();
                // 设置栈顶节点为新节点的next节点
                node.next = next;
                // CAS更新top指向新节点
                if (top.compareAndSet(next, node)) {
                    return;
                }
            }
        }
    }


    private static void testStack() {
        // 初始化栈为 top->1->2->3
        Stack stack = new Stack();
        stack.push(new Stack.Node(3));
        stack.push(new Stack.Node(2));
        stack.push(new Stack.Node(1));

        new Thread(()->{
            // 线程1出栈一个元素
            stack.pop();
        }).start();

        new Thread(()->{
            // 线程2出栈两个元素
            Stack.Node A = stack.pop();
            Stack.Node B = stack.pop();
            // 线程2又把A入栈了
            stack.push(A);
        }).start();
    }

    public static void main(String[] args) {
        testStack();
    }
}
