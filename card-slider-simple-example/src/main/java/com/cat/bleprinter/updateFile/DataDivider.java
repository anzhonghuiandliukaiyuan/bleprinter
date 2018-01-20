package com.cat.bleprinter.updateFile;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lkyzz on 2018/1/15.
 */

public class DataDivider {


    /**
     *将int转换成byte[]
     * @param file
     * @return
     */
    public static byte[] intToByteArray(int file) {
        return new byte[] {
                (byte) ((file >> 24) & 0xFF),
                (byte) ((file >> 16) & 0xFF),
                (byte) ((file >> 8) & 0xFF),
                (byte) (file & 0xFF)
        };
    }

    /**
     * 计算总包数
     *
     * @author 高杨
     * @date 2017年12月13日 下午3:44:08
     * @param rawData：升级文件
     * @return
     */
    public static int packCount(byte[] rawData) {
        return rawData.length % 128 == 0 ? rawData.length / 128 : rawData.length / 128 + 1;
    }

    /**
     * 对原始数据进行分包，每128字节为一包。
     * 核心方法
     * @author 高杨
     * @date 2017年12月13日 下午4:03:12
     * @param rawData
     * @return
     */
    public static List<byte[]> divide(byte[] rawData) {
        List<byte[]> result = new ArrayList<>();
        int length = rawData.length; // 获取原始数据的总长度
        int packCount = length % 128 == 0 ? length / 128 : length / 128 + 1; // 总包数
        for (int i = 0; i < packCount; i++) {
            byte[] buf = i == packCount - 1 && length % 128 != 0 ? new byte[length % 128] : new byte[128];
            for (int j = 0; j < buf.length; j++) {
                buf[j] = rawData[i * 128 + j];
            }
            if (buf.length != 128) { // 判断该包是否为最末包。若最后一包的长度恰好为128，则直接添加至list集合中
                byte[] ff = new byte[128 - buf.length]; // 生成一个值全为FF的数组，其长度是128减去buf数组的长度
                byte[] endBuf = new byte[128]; // 声明最后一个包的数据
                for (int k = 0; k < ff.length; k++) {
                    ff[k] = (byte) 0xFF;
                } // 填充FF数组
                // 两数组合并，合并后的数据保存到endBuf数组中
                System.arraycopy(buf, 0, endBuf, 0, buf.length);
                System.arraycopy(ff, 0, endBuf, buf.length, ff.length);
                result.add(endBuf);
            } else {
                result.add(buf);
            }

        }
        return result;
    }

    /**
     * ' 判断包序号是否在合法范围内（包序号从1开始计数）
     *
     * @author 高杨
     * @date 2017年12月13日 下午4:03:29
     * @param rawData
     * @param id
     * @return
     */
    public static boolean isInner(byte[] rawData, int id) {
        return id <= packCount(rawData) && id > 0 ? true : false;
    }

    /**
     * 根据包序号取得包数据
     * @author 高杨
     * @date 2017年12月13日 下午4:03:40
     * @param rawData
     * @param id
     * @return
     */
    public static byte[] getPackById(byte[] rawData, int id) {
        return id <= packCount(rawData) && id > 0 ? divide(rawData).get(id - 1) : null; // 包序号若不在合法范围内则返回null
    }

    /**
     * 计算校验和，累计计算
     * @author 高杨
     * @date 2017年12月13日 下午4:04:57
     * @param rawData
     * @return
     */
    public static int xor(byte[] rawData) {
        int sum = 0x0000;
        List<byte[]> result = divide(rawData); // 最后一包补的FF也要参与计算，所以要使用补过的数据集进行计算
        for (byte[] buf : result) {
            for (int i = 0; i < 128; i++) {
                short b = (short) (buf[i] & 0xFF);
                sum += b;
                sum &= 0xFFFF; // 按位与
            }
        }
        return sum;
    }

    // 计算校验和，累计计算
    public static int xor2int(byte[] rawData) {
        int sum = 0x0000;
        // List<byte[]> result = divide(rawData);
        // //最后一包补的FF也要参与计算，所以要使用补过的数据集进行计算
        // for(byte[] buf : result){
        for (int i = 0; i < rawData.length; i++) {
            short b = (short) (rawData[i] & 0xFF);
            sum += b;
            sum &= 0xFFFF; // 按位与
        }
        // }
        return sum;
    }

}

