package com.dizsun.timechain.constant;

import java.io.*;
import java.util.Properties;

/**
 * 定义了各种初始化参数的获取方法
 */
public class Config {
    private Properties properties;
    private String localHost;
    private String timeCenterIp;
    private int ntpListenPort = -1;
    private int timeCenterListenPort = -1;
    private int httpPort = -1;
    private int p2pPort = -1;
    private int index = -1;
    private String mainNode;

    public void setLocalHost(String localHost) {
        this.localHost = localHost;
    }

    public void setTimeCenterIp(String timeCenterIp) {
        this.timeCenterIp = timeCenterIp;
    }

    public void setNtpListenPort(int ntpListenPort) {
        this.ntpListenPort = ntpListenPort;
    }

    public void setTimeCenterListenPort(int timeCenterListenPort) {
        this.timeCenterListenPort = timeCenterListenPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void setP2pPort(int p2pPort) {
        this.p2pPort = p2pPort;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setMainNode(String mainNode) {
        this.mainNode = mainNode;
    }

    /**
     * 以localHost为例,如果没有在命令行传输localHost参数,则先从配置文件寻找,若没有则从R文件中查找
     * @return
     */
    public String getLocalHost() {
        if (localHost == null) {
            String local_host = properties.getProperty("local_host");
            if (local_host == null||local_host.isEmpty()) {
                localHost = R.DEFAULT_LOCAL_HOST;
            }else {
                localHost = local_host;
            }
        }
        return localHost;
    }

    public String getTimeCenterIp() {
        if (timeCenterIp == null) {
            String time_center_ip = properties.getProperty("time_center_ip");
            if (time_center_ip == null||time_center_ip.isEmpty()) {
                timeCenterIp = R.DEFAULT_TIME_CENTER_IP;
            }else {
                timeCenterIp = time_center_ip;
            }
        }
        return timeCenterIp;
    }

    public int getNtpListenPort() {
        if (ntpListenPort == -1) {
            try {
                ntpListenPort = Integer.parseInt(properties.getProperty("ntp_listen_port"));
            } catch (NumberFormatException e) {
                ntpListenPort = R.DEFAULT_NTP_LISTEN_PORT;
            }
        }
        return ntpListenPort;
    }

    public int getTimeCenterListenPort() {
        if (timeCenterListenPort == -1) {
            try {
                timeCenterListenPort = Integer.parseInt(properties.getProperty("time_center_listen_port"));
            } catch (NumberFormatException e) {
                timeCenterListenPort = R.DEFAULT_TIME_CENTER_LISTEN_PORT;
            }
        }
        return timeCenterListenPort;
    }

    public int getHttpPort() {
        if (httpPort == -1) {
            try {
                httpPort = Integer.parseInt(properties.getProperty("default_http_port"));
            } catch (NumberFormatException e) {
                httpPort = R.DEFAULT_HTTP_PORT;
            }
        }
        return httpPort;
    }

    public int getP2pPort() {
        if (p2pPort == -1) {
            try {
                p2pPort = Integer.parseInt(properties.getProperty("default_p2p_port"));
            } catch (NumberFormatException e) {
                p2pPort = R.DEFAULT_P2P_PORT;
            }
        }
        return p2pPort;
    }

    public int getIndex() {
        if (index == -1) {
            try {
                index = Integer.parseInt(properties.getProperty("index"));
            } catch (NumberFormatException e) {
                index = R.INDEX;
            }
        }
        return index;
    }

    public String getMainNode() {
        if (mainNode == null) {
            String main_node = properties.getProperty("main_node");
            if (main_node == null||main_node.isEmpty()) {
                mainNode = R.DEFAULT_MAIN_NODE;
            }else {
                mainNode = main_node;
            }
        }
        return mainNode;
    }

    private Config() {
    }

    /**
     * 从config.properties文件中载入所有配置
     */
    public void init() {
//        String filePath = System.getProperty("user.dir") + System.getProperty("file.separator") + "config.properties";
        String filePath = "config.properties";
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

            modifyLog4j(getIndex());
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

    private void modifyLog4j(int index) {
        Properties p = new Properties();
        InputStream inputStream = Config.class.getClassLoader().getResourceAsStream("log4j.properties");
        try {
            p.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        p.setProperty("log4j.appender.DIZSUN.File", "/info/" + index + ".log");
        FileOutputStream oFile = null;
        try {
            oFile = new FileOutputStream("log4j.properties");
            //将Properties中的属性列表（键和元素对）写入输出流
            p.store(oFile, "");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
