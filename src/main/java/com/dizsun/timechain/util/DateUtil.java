package com.dizsun.timechain.util;

import com.dizsun.timechain.constant.Config;

import java.io.DataInputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
    private Date date;
    private SimpleDateFormat sdf;
    private String time;
    private static DateUtil dateUtil;

    private DateUtil(){
    }

    public static DateUtil newDataUtil(){
        if(dateUtil==null){
            dateUtil=new DateUtil();
        }
        return dateUtil;
    }

    public int getCurrentMinute(){
        sdf = new SimpleDateFormat("mm");
        date=new Date();
        return Integer.parseInt(sdf.format(date));
    }

    public int getCurrentSecond(){
        sdf = new SimpleDateFormat("ss");
        date=new Date();
        return Integer.parseInt(sdf.format(date));
    }

    public String getTimeFromRC(){
        time="-1";
        try {
            Config config = Config.getInstance();
            Socket socket=new Socket(config.getTimeCenterIp(), config.getTimeCenterListenPort());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            time=""+dis.readLong();
            socket.close();
            return time;
        }catch (Exception e){
            e.printStackTrace();
        }
        return time;
    }

}
