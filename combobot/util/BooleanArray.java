package combobot.util;

public class BooleanArray {
    private long[][] array;
    public BooleanArray() {
        array = new long[4096][];
    }
    public void setTrue(int i) {
        int index = i / 4096;
        int index2 = (i % 4096) / 64;
        if (array[index] == null) {
            array[index] = new long[64];
        }
        int index3 = i % 64;
        array[index][index2] |= (1L << index3);
    }
    public boolean get(int i) {
        int index = i / 4096;
        if (array[index] == null) {
            return false;
        }
        int index2 = (i % 4096) / 64;
        int index3 = i % 64;
        return ((array[index][index2] >>> index3) & 0b1) != 0;
    }
    public boolean getAndSetTrue(int i) {
        // bytecode awareness
        int index = i / 4096;
        int index2 = (i % 4096) / 64;
        int index3 = i % 64;
        long[] subarray = array[i / 4096];
        if (subarray == null) {
            long[] temp = new long[64];
            temp[index2] = 1L << index3;
            array[index] = temp;
            return false;
        }
        long bitmask = 1L << index3;
        if ((subarray[index2] & bitmask) == 0) {
            subarray[index2] |= bitmask;
            return false;
        } else {
            return true;
        }
    }
}
