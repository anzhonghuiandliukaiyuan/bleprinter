package com.cat.bleprinter.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by FMY on 2017/6/25 0025.
 */
public class ProtocolUtil {

    /**
     * byte数组转换为16进制字符串
     * @param data
     * @return
     */
    public static String bytes2HexString(byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (byte b : data) {
            builder.append(String.format("%02x", b).toUpperCase()).append(" ");
        }

        return builder.toString();

    }
    /**
     * int数组转换为16进制字符串
     * @param data
     * @return
     */
    public static String ints2HexString(int[] data) {
        StringBuilder builder = new StringBuilder();
        for (int b : data) {
            builder.append(String.format("%02x", b).toUpperCase()).append(" ");
        }

        return builder.toString();

    }

    /**
     * 十进制转BCD码
     * @param src 不大于99
     * @return
     */
    public static byte Dec2BCD(int src) {
        assert src <= 99;
        String hexStr = String.valueOf(src);
        byte hex = (byte) Integer.parseInt(hexStr, 16);
        return hex;
    }
    /**
     * 十进制字符串转BCD码
     * @param src 不大于99
     * @return
     */
    public static byte Dec2BCD(String src) {
        int val= Integer.parseInt(src);
        assert val <= 99;
        String hexStr = String.valueOf(val);
        byte hex = (byte) Integer.parseInt(hexStr, 16);
        return hex;
    }

    /**
     * 把BCD编码转换为十进制数字组成的字符串
     *
     * @param src 源数组
     * @param beginIndex 起始索引
     * @param length 需要转换的长度
     * @return
     */
    public static String BCD2Dec(byte[] src, int beginIndex, int length) {
        StringBuilder builder = new StringBuilder();

        for (int i = beginIndex; i < length; i++) {
            int aa = src[i] & 0xFF;
            builder.append(String.format("%02x", aa));

        }

        return builder.toString().toUpperCase();

    }


    public static byte[] packData(byte cmd, byte[] param) {
        byte[] temp = new byte[param.length + 6];
        byte[] data = ArrayFill.fillBytes(temp, 4, param.length, param);
        data[0] = (byte) 0xAA;
        data[1] = 0x00;
        data[2] = cmd;
        data[3] = (byte) param.length;

        byte checkData[] = new byte[data.length-3];
        for (int i = 0; i < data.length-3; i++) {
            checkData[i] = data[i+1];
        }

        data[data.length - 2] = XorByByte(checkData);
        data[data.length - 1] = (byte) 0xCC;

        return data;
    }


    /**
     * 异或校检
     *
     * @return
     */
    public static byte XorByByte(byte[] data) {
        byte temp = data[0];
        for (int i = 1; i < data.length; i++) {
            temp ^= data[i];
        }
        return temp;
    }

    /**
     * 按位异或运算
     *
     * @param src0
     * @param src1
     * @return
     */
    private static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 }))
                .byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 }))
                .byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    /**
     * 将16进制字符串转化为要发送的字节数组
     *
     * @param src
     * @return
     */
    public static byte[] HexString2Buf(String src) {
        int len = src.length();
        byte[] ret = new byte[len / 2];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < len; i += 2) {
            ret[i / 2] = uniteBytes(tmp[i], tmp[i + 1]);
        }
        return ret;
    }

    /**
     * byte数组的追加
     * @param org
     * @param to
     * @return
     */
    public static byte[] append(byte[] org, byte to) {
        byte[] newByte = new byte[org.length + 1];
        System.arraycopy(org, 0, newByte, 0, org.length);
        newByte[org.length] = to;
        return newByte;
    }


    // 读取2个字节转为无符号整型
    public static int shortbyte2int(byte[] res) {
        DataInputStream dataInputStream = new DataInputStream(
                new ByteArrayInputStream(res));
        int a = 0;
        try {
            a = dataInputStream.readUnsignedShort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return a;
    }

    //检查收到的数据的异或校验
    public static  boolean checkReceiveData(int[] data){
        byte check = (byte) data[data.length - 2];
        byte checkData[] = new byte[data.length-3];
        for (int i = 0; i < data.length-3; i++) {
            checkData[i] = (byte)data[i+1];
        }
        if(check == XorByByte(checkData)){
            return true;
        }
        System.out.println("校验失败，正确的校验码为："+bytes2HexString(new byte[]{XorByByte(checkData)}));
        return false;
    }

}

