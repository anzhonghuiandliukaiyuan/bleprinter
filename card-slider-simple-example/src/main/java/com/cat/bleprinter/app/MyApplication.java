package com.cat.bleprinter.app;

import android.app.Application;
import android.content.Context;

import com.beardedhen.androidbootstrap.TypefaceProvider;

import net.danlew.android.joda.JodaTimeAndroid;

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

    }

    public static Context getContext(){
        return context;
    }
}
