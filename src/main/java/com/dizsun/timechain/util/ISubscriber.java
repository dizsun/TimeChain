package com.dizsun.timechain.util;

public interface ISubscriber {
    public void doPerHour00();
    public void doPerHour59();
    public void doPerHour01();
    public void doPerHour45();
}
