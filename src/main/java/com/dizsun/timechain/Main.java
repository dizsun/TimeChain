package com.dizsun.timechain;

import com.dizsun.timechain.constant.Broadcaster;
import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.service.*;
import com.dizsun.timechain.util.*;

import java.io.*;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    private static String Drivder = "org.sqlite.JDBC";
    private static String path = "/info/";

    public static void main(String[] args) {
        //TODO 读取输入的参数进行判定，如果有输入参数则配置config按照输入参数设置，否则直接获取
        if (args != null && args.length == 3) {
            try {
                String index = args[2];
                LogUtil.init(path, index);
                String mainHost = getMainHost();
                Config config = Config.getInstance();
                config.init();


                NTPService ntpService = new NTPService();
                ntpService.start();
                Broadcaster broadcaster = new Broadcaster();
                int httpPort = Integer.valueOf(args[0]);
                int p2pPort = Integer.valueOf(args[1]);
                P2PService p2pService = P2PService.getInstance();
                broadcaster.subscribe(p2pService);
                p2pService.initP2PServer(p2pPort);
                HTTPService httpService = new HTTPService();
                broadcaster.broadcast();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        p2pService.connect(mainHost);
                    }
                }, 5000);
                httpService.initHTTPServer(httpPort);
            } catch (Exception e) {
                System.out.println("startup is error:" + e.getMessage());
            }
        } else {
            System.out.println("usage: java -jar blockchainTest.jar 9000 6001 254");
        }

    }

    public static String getMainHost() {
        String res = "0.0.0.0";
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path + "config.txt"));
            String str = bufferedReader.readLine();
            res = str.split("=")[1];
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

//    public static void main(String[] args) {
//        File file = new File("/info/test.txt");
//        if (!file.exists()) {
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            FileOutputStream out = new FileOutputStream(file, true);
//            StringBuffer sb = new StringBuffer();
//            sb.append("test infomation");
//            out.write(sb.toString().getBytes("utf-8"));
//            out.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

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