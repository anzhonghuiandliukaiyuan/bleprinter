package com.cat.bleprinter.dao;

import com.cat.bleprinter.entity.BlueTooth;

import org.xutils.ex.DbException;
import org.xutils.x;

import java.util.Collections;
import java.util.List;

/**
 * Created by FMY on 2017/6/27 0027.
 */
public class BlueToothDao {
    public void save(BlueTooth ble) {
        try {
            ble.setId(1);
            x.getDb(DBManager.getDaoConfig()).save(ble);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    public List<BlueTooth> getALL(){
        try {
           return x.getDb(DBManager.getDaoConfig()).findAll(BlueTooth.class);
        } catch (DbException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public BlueTooth getFirst() {
        BlueTooth blueTooth = null;
        try {
            blueTooth = x.getDb(DBManager.getDaoConfig()).findFirst(BlueTooth.class);

        } catch (DbException e) {
            e.printStackTrace();
        }

        return blueTooth;

    }

    public void deleteAll(){
        try {
            x.getDb(DBManager.getDaoConfig()).delete(BlueTooth.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }
}
