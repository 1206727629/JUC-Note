/**
 * @Author yangwentian5
 * @Date 2022/2/9 9:33
 */

public class BitMap { // Java中char类型占16bit，也即是2个字节
    private char[] bytes;
    private int nbits;

    public static void main(String[] args) {
        BitMap bitMap = new BitMap(33);
        bitMap.a();
        System.out.println(1 << 4);
        System.out.println(3 | 1 << 4);
    }

    public BitMap(int nbits) {
        this.nbits = nbits;
        this.bytes = new char[nbits/16+1];
    }

    public void set(int k) {
        if (k > nbits) return;
        int byteIndex = k / 16;
        int bitIndex = k % 16;
        bytes[byteIndex] |= (1 << bitIndex);
    }

    public boolean get(int k) {
        if (k > nbits) return false;
        int byteIndex = k / 16;
        int bitIndex = k % 16;
        return (bytes[byteIndex] & (1 << bitIndex)) != 0;
    }

    private void a () {
        System.out.println(this.bytes.length);
    }
}
