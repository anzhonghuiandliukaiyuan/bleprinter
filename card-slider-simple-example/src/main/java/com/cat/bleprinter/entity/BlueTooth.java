package com.cat.bleprinter.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * Created by FMY on 2017/6/27 0027.
 */
@Table(name = "t_bluetooth")
public class BlueTooth {
    @Column(name = "id",isId = true)
    private Integer id;
    @Column(name = "name")
    private String name;
    @Column(name = "mac")
    private String mac;

    public BlueTooth(){

    }

    public BlueTooth(String name, String mac) {

        this.name = name;
        this.mac = mac;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    @Override
    public String toString() {
        return "BlueTooth{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", mac='" + mac + '\'' +
                '}';
    }
}
