package com.cat.bleprinter.exception;

/**
 * Created by FMY on 2017/6/29 0029.
 */
public class BleNoConnectedException extends Exception {
    public BleNoConnectedException() {
        super("蓝牙设备尚未连接");
    }
}
