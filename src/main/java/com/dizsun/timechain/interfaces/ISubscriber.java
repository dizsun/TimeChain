package com.dizsun.timechain.interfaces;

public interface ISubscriber {
    void doPerHour00();

    void doPerHour59();

    void doPerHour01();

    void doPerHour45();
}
