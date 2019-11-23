package com.dizsun.timechain.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class LogUtil {
    public static String CONSENSUS = "consensus";
    public static String NTP = "ntp";
    public static void init(String path,String index){
        LogUtil.CONSENSUS = path+"consensus_" + index + ".txt";
        LogUtil.NTP = path+"ntp_" + index + ".txt";
    }
    public static void writeLog(String msg, String fileName){
        File file = new File(fileName);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream out = new FileOutputStream(file, true);
            StringBuffer sb = new StringBuffer();
            sb.append(msg+"\n");
            out.write(sb.toString().getBytes("utf-8"));
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
