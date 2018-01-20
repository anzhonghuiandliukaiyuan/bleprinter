package com.cat.bleprinter.app;

import android.app.Application;
import android.content.Context;

import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.cat.bleprinter.log4j.LogUtil;

import net.danlew.android.joda.JodaTimeAndroid;

import org.apache.log4j.Logger;
import org.xutils.x;


/**
 * Created by FMY on 2017/6/26 0026.
 */
public class MyApplication extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        x.Ext.init(this);
        TypefaceProvider.registerDefaultIconSets();
        context = getApplicationContext();


        //配置log4j基本参数
        LogUtil.configLog();
        //获取Application Log
        Logger log = Logger.getLogger(MyApplication.class);
        //输出MyApplication的信息
        log.info("Log4j Is Ready and My Application Was Created Successfully! ");
    }

    public static Context getContext(){
        return context;
    }
}
