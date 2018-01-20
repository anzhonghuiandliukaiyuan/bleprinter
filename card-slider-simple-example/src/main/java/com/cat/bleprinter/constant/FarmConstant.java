package com.cat.bleprinter.constant;

/**
 * Created by FMY on 2017/6/26 0026.
 */
public class FarmConstant {
    public static final byte  SUCCESS = 0x01;
    public static final byte  FAILED  = 0x00;

    public static final byte  NORMAL = 0x00;        //表示设备正常
    public static final byte  ABNORMAL = 0x01;      //表示设备异常或短路
    public static final byte  UNCIRCUIT = 0x02;     //表示设备断路

    public static final int PRINT_SUCCESS = 0;
    public static final int PRINT_FAILED = 1;
    public static final int TIMEOUT = 2;
    public static final int CONFIG_SUCCESS = 3;
    public static final int CONFIG_FAILED = 4;

    public static final byte CMD_VERSION = 0x06;
    public static final byte CMD_START = 0x07;
    public static final byte CMD_WAIT = 0x08;
    public static final byte CMD_PENSHE = 0x09;
    public static final byte CMD_PAIKONG = 0x0A;
    public static final byte CMD_JIARE = 0x0B;
    public static final byte CMD_HUILIU = 0x0C;
    public static final byte CMD_FANFIANG = 0x0D;
    public static final byte CMD_UPDATE = (byte) 0xF0;
    public static final byte CMD_HANDSHAKE = 0x01; //握手命令字

    public static final String SATE_ACTION_NAME = "com.cat.ble.state";
    public static final String ZJ_RESPONSE_ACTION_NAME = "com.cat.ble.response.zijian";
    public static final String TY_RESPONSE_ACTION_NAME = "com.cat.ble.response.tongyong";
    public static final String ZT_RESPONSE_ACTION_NAME = "com.cat.ble.response.zhuangtai";
    public static final String GZ_RESPONSE_ACTION_NAME = "com.cat.ble.response.zhuangtai";
    public static final String VERSION_RESPONSE_ACTION_NAME = "com.cat.ble.response.version";
    public static final String JY_RESPONSE_ACTION_NAME = "com.cat.ble.response.jianya";
    public static final String BLUETOOTH_RESULT = "com.cat.ble.bluetooth.result";

}
