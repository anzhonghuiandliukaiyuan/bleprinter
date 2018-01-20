package com.cat.bleprinter.dao;

import com.cat.bleprinter.entity.Config;

import org.xutils.ex.DbException;
import org.xutils.x;

import java.util.List;

/**
 * Created by FMY on 2017/6/26 0026.
 */
public class ConfigDao {
    public void save(Config config){
        try {
            x.getDb(DBManager.getDaoConfig()).save(config);
        } catch (DbException e) {
            e.printStackTrace();
        }

    }

    public Config getFirst(){
        Config config = null;
        try {
            config = x.getDb(DBManager.getDaoConfig()).findFirst(Config.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
        return config;
    }

    public void deleteAll(){
        try {
            x.getDb(DBManager.getDaoConfig()).delete(Config.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    public List<Config> getAll(){
        try {
           return x.getDb(DBManager.getDaoConfig()).findAll(Config.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
        return null;
    }
}
