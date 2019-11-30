package com.dizsun.timechain;

import com.dizsun.timechain.constant.Broadcaster;
import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.service.*;
import com.dizsun.timechain.util.*;
import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class Main {
    private static String Drivder = "org.sqlite.JDBC";
    static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        Config config = Config.getInstance();
        config.init();
        if (args == null || args.length == 0) {
            //使用配置文件或者默认值
        } else if (args.length == 4) {
            config.setIndex(Integer.parseInt(args[0]));
            config.setLocalHost(args[1]);
            config.setTimeCenterIp(args[2]);
            config.setMainNode(args[3]);
        } else if (args.length == 8) {
            config.setHttpPort(Integer.parseInt(args[0]));
            config.setP2pPort(Integer.parseInt(args[1]));
            config.setTimeCenterListenPort(Integer.parseInt(args[2]));
            config.setNtpListenPort(Integer.parseInt(args[3]));
            config.setIndex(Integer.parseInt(args[4]));
            config.setLocalHost(args[5]);
            config.setTimeCenterIp(args[6]);
            config.setMainNode(args[7]);
        } else {
            logger.error("传入参数错误，应传入参数为：\n" +
                    "  1.无参数\n" +
                    "  2.index,localHost,timeCenterIp,mainNode\n" +
                    "  3.httpPort,p2pPort,timeCenterPort,ntpPort,index,localHost,timeCenterIp,mainNode");
            System.exit(0);
        }
        try {
            LogUtil.init(config.getIndex());
            NTPService ntpService = new NTPService();
            ntpService.start();
            Broadcaster broadcaster = new Broadcaster();
            P2PService p2pService = P2PService.getInstance();
            broadcaster.subscribe(p2pService);
            p2pService.initP2PServer(config.getP2pPort());
            HTTPService httpService = new HTTPService();
            broadcaster.broadcast();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (config.getLocalHost().equals(config.getMainNode())) return;
                    p2pService.connect(config.getMainNode());
                }
            }, 5000);
            httpService.initHTTPServer(config.getHttpPort());
        } catch (Exception e) {
            logger.error("startup is error:" + e.getMessage());

        }

    }
}

//        File dbFileFolder = new File("./db");
//        if(!dbFileFolder.exists()){
//            System.out.println("db文件夹不存在!");
//            if(dbFileFolder.mkdir()){
//                System.out.println("db文件夹创建成功!");
//            }else {
//                System.out.println("db文件夹创建失败!");
//                return;
//            }
//        }
//        File dbFile = new File("./db/blocks.db");
//        if(!dbFile.exists()){
//            System.out.println("db文件不存在!");
//            try {
//                SQLUtil sqlUtil=new SQLUtil();
//                sqlUtil.initBlocks(null);
//                System.out.println("db文件创建成功!");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }