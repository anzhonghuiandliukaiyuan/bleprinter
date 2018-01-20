package com.cat.bleprinter.entity;

import com.cat.bleprinter.util.ArrayFill;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.io.UnsupportedEncodingException;

/**
 * Created by FMY on 2017/6/25 0025.
 */
@Table(name = "t_config")
public class Config {
    @Column(name = "id", isId = true, autoGen = true)
    private Integer id;
    @Column(name = "company")
    private String company;
    @Column(name = "phone")
    private String phone;
    //8位
    @Column(name = "carnum")
    private String carNum;
    //18位
    @Column(name = "drivercode")
    private String driverCode;
    @Column(name = "price")
    private Double price;
    @Column(name = "addmoney")
    private Double addMoney;


    public Config() {
    }

    public Config(String company, String phone, String carNum, String driverCode) {
        this.company = company;
        this.phone = phone;
        this.carNum = carNum;
        this.driverCode = driverCode;
    }


    /**
     * @return null 或 byte
     */
    public byte[] packData() {
        try {
            byte[] b_com = company.getBytes("GBK");
            byte[] b_phone = phone.getBytes();
            byte[] b_carnum = carNum.getBytes("GBK");

            for (int i = 0; i < driverCode.length(); i++) {
                if (driverCode.length() == 18) {
                    break;
                }
                driverCode += "0";
            }


            byte[] b_driverCode = driverCode.getBytes();
            int len = 8 + 18 + b_com.length + b_phone.length;
            int len2 = b_com.length + b_phone.length;
            int len3 = b_com.length + b_phone.length + b_carnum.length;

            byte[] data = new ArrayFill(len).fillBytes(0, b_com.length, b_com)
                    .fillBytes(b_com.length, b_phone.length, b_phone)
                    .fillBytes(len2, b_carnum.length, b_carnum)
                    .fillBytes(len3, b_driverCode.length, b_driverCode).getArr();

            return data;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getAddMoney() {
        return addMoney;
    }

    public void setAddMoney(Double addMoney) {
        this.addMoney = addMoney;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCarNum() {
        return carNum;
    }

    public void setCarNum(String carNum) {
        this.carNum = carNum;
    }

    public String getDriverCode() {
        return driverCode;
    }

    public void setDriverCode(String driverCode) {
        this.driverCode = driverCode;
    }

    @Override
    public String toString() {
        return "Config{" +
                "id=" + id +
                ", company='" + company + '\'' +
                ", phone='" + phone + '\'' +
                ", carNum='" + carNum + '\'' +
                ", driverCode='" + driverCode + '\'' +
                '}';
    }
}
