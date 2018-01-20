package com.cat.bleprinter.util;

/**
 * Created by FMY on 2017/6/25 0025.
 */
public class ArrayFill {

    private byte[] arr;

    public byte[] getArr() {
        return arr;
    }



    public ArrayFill(int src_len) {
        this.arr = new byte[src_len];
    }

    /**
     * @param beginIndex 被填充的数组的起始
     * @param length     被填充的数组要填充的长度
     * @param operaed    提供填充数据源的数组
     * @return
     */
    public ArrayFill fillBytes(int beginIndex, int length, byte[] operaed) {

        if (length > operaed.length || beginIndex + length > arr.length) {
            throw new IndexOutOfBoundsException("填充数组时发生数组越界");
        }
        for (int i = 0; i < length; i++) {
            arr[i + beginIndex] = operaed[i];
        }

        return this;
    }
    /**
     * @param src        被填充的数组
     * @param beginIndex 被填充的数组的起始
     * @param length     被填充的数组要填充的长度
     * @param operaed    提供填充数据源的数组
     * @return
     */
    public static byte[] fillBytes(byte[] src ,int beginIndex, int length, byte[] operaed) {

        if (length > operaed.length || beginIndex + length > src.length) {
            throw new IndexOutOfBoundsException("填充数组时发生数组越界");
        }

            for (int i = 0; i < length; i++)
            {
                src[i + beginIndex] = operaed[i];
            }

        return src;
    }
}
