package com.dizsun.timechain;

import com.dizsun.timechain.constant.Broadcaster;
import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.service.*;
import com.dizsun.timechain.util.*;
import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
//TODO 将websocket换成netty的tcp连接，并且不再持有长连接，而是每次使用时再建立
public class Main {
    private static String Drivder = "org.sqlite.JDBC";
    static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        //初始化参数
        Config config = Config.getInstance();
        config.init();
        /**
         *  默认为从配置文件或者R中寻找参数,从命令行传入的参数则可以覆盖原有的数据
         */
        if (args == null || args.length == 0) {
            //使用配置文件或者默认值
        } else if (args.length == 4) {
            config.setIndex(Integer.parseInt(args[0]));
            config.setLocalHost(args[1]);
            config.setTimeCenterIp(args[2]);
            config.setMainNode(args[3]);
            logger.info("config inited");
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
//        打印输出参数
//        logger.info(config.getLocalHost());
//        logger.info(config.getMainNode());
//        logger.info(config.getTimeCenterIp());
//        logger.info(config.getIndex());
//        logger.info(config.getNtpListenPort());
//        logger.info(config.getHttpPort());
//        logger.info(config.getP2pPort());
//        logger.info(config.getTimeCenterListenPort());
        //初始化并启动各个组件
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