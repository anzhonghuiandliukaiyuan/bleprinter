package com.cat.bleprinter.dao;

import org.xutils.DbManager;

/**
 * Created by FMY on 2017/6/26 0026.
 */
public class DBManager {
    private static DbManager.DaoConfig daoConfig;

    public static DbManager.DaoConfig getDaoConfig(){
        if(daoConfig==null){
            daoConfig = new DbManager.DaoConfig();
            daoConfig.setDbVersion(2);
        }
        return daoConfig;
    }
}
