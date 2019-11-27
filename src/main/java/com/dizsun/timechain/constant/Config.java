package com.dizsun.timechain.constant;

import java.io.*;
import java.util.Properties;

public class Config {
    private Properties properties;
    //TODO 将所有配置设置私有属性，可以读取也可以直接设置，读取时先从属性值找，没有时再读取配置文件，配置文件也没有就是用默认值

    public String getLocalHost() {
        String local_host = properties.getProperty("local_host");
        if(local_host==null){
            return R.DEFAULT_LOCAL_HOST;
        }
        return local_host;
    }

    public String getTimeCenterIp() {
        String time_center_ip = properties.getProperty("time_center_ip");
        if(time_center_ip==null){
            return R.DEFAULT_TIME_CENTER_IP;
        }
        return time_center_ip;
    }

    public int getNtpListenPort() {
        try {
            return Integer.parseInt(properties.getProperty("ntp_listen_port"));
        }catch (NumberFormatException e){
            return R.DEFAULT_NTP_LISTEN_PORT;
        }
    }

    public int getTimeCenterListenPort(){
        try {
            return Integer.parseInt(properties.getProperty("time_center_listen_port"));
        }catch (NumberFormatException e){
            return R.DEFAULT_TIME_CENTER_LISTEN_PORT;
        }
    }

    public int getDefaultHttpPort(){
        try {
            return Integer.parseInt(properties.getProperty("default_http_port"));
        }catch (NumberFormatException e){
            return R.DEFAULT_HTTP_PORT;
        }
    }

    public int getDefaultP2pPort(){
        try {
            return Integer.parseInt(properties.getProperty("default_p2p_port"));
        }catch (NumberFormatException e){
            return R.DEFAULT_P2P_PORT;
        }
    }

    private Config() {
    }

    /**
     * 从config.properties文件中载入所有配置
     */
    public void init() {
        String filePath = System.getProperty("user.dir") + System.getProperty("file.separator") + "config.properties";
        properties = new Properties();
        InputStream in = null;
        try {
            File file = new File(filePath);
            if (file.canRead()) {
                in = new BufferedInputStream(new FileInputStream(file));
            } else {
                in = Config.class.getClassLoader().getResourceAsStream(filePath);
            }
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static class Holder {
        private static final Config config = new Config();
    }

    public static Config getInstance() {
        return Holder.config;
    }


}
