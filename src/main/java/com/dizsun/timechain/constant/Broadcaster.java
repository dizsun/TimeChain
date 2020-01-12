package com.dizsun.timechain.constant;

import com.dizsun.timechain.util.DateUtil;
import com.dizsun.timechain.interfaces.ISubscriber;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 广播时间变化事件,相当于计时器
 */
public class Broadcaster {
    private Timer timer;
    private ArrayList<ISubscriber> subscribers;
    private DateUtil dateUtil;

    public Broadcaster() {
        timer = new Timer();
        subscribers = new ArrayList<>();
        dateUtil = DateUtil.newDataUtil();
    }

    /**
     * 这里默认5分钟一个周期,可以更改模值来改变周期
     */
    public void broadcast() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(dateUtil.getCurrentMinute()%5==1){
                    for (ISubscriber s : subscribers) {
                        s.doPerHour45();
                    }
                }
                else if(dateUtil.getCurrentMinute()%5==2){
                    for (ISubscriber s : subscribers) {
                        s.doPerHour59();
                    }
                }
                else if(dateUtil.getCurrentMinute()%5==3){
                    for (ISubscriber s : subscribers) {
                        s.doPerHour00();
                    }
                }
                else if(dateUtil.getCurrentMinute()%5==4){
                    for (ISubscriber s : subscribers) {
                        s.doPerHour01();
                    }
                }
//                switch (dateUtil.getCurrentMinute()) {
//                    case 0:
//                    case 20:
//                    case 40:
//                        for (ISubscriber s : subscribers) {
//                            s.doPerHour00();
//                        }
//                        break;
//                    case 5:
//                    case 25:
//                    case 45:
//                        for (ISubscriber s : subscribers) {
//                            s.doPerHour01();
//                        }
//                        break;
//                    case 10:
//                    case 30:
//                    case 50:
//                        for (ISubscriber s : subscribers) {
//                            s.doPerHour45();
//                        }
//                        break;
//                    case 15:
//                    case 35:
//                    case 55:
//                        for (ISubscriber s : subscribers) {
//                            s.doPerHour59();
//                        }
//                        break;
//                    default:
//                        break;
//                }
            }
        }, 1, 60*1000);
    }

    public void subscribe(ISubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void unSubscribe(ISubscriber subscriber) {
        subscribers.remove(subscriber);
    }

}
