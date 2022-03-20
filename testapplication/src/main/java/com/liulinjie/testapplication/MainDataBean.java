package com.liulinjie.testapplication;

import com.llj.baselib.IOTInterfaceId;

public class MainDataBean {

    @IOTInterfaceId("14647")
    private int state;

    @IOTInterfaceId("14770")
    private Float money;

    public int getState() {
        return state;
    }

    @Override
    public String toString() {
        return "MainDataBean{" +
                "i=" + state +
                ", flo=" + money +
                '}';
    }

    public void setState(int state) {
        this.state = state;
    }

    public Float getMoney() {
        return money;
    }

    public void setMoney(Float money) {
        this.money = money;
    }
}
