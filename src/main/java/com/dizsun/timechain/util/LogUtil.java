package com.dizsun.timechain.util;

import com.dizsun.timechain.constant.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LogUtil {
    // TODO 修改为单利模式并将常亮外置
    public static String CONSENSUS = "consensus";
    public static String NTP = "ntp";

    public static void init(int index) {
        LogUtil.CONSENSUS = R.LOG_FILE_PATH + "consensus_" + index + ".txt";
        LogUtil.NTP = R.LOG_FILE_PATH + "ntp_" + index + ".txt";
    }

    public static void writeLog(String msg, String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream out = new FileOutputStream(file, true);
            out.write((msg + "\n").getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
