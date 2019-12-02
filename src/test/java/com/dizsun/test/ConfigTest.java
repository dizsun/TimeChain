package com.dizsun.test;

import com.dizsun.timechain.constant.Config;
import org.junit.Test;

public class ConfigTest{
    @Test
    public void configTest(){
        Config config=Config.getInstance();
        config.init();
        System.out.println(config.getIndex());
        System.out.println(config.getLocalHost());
        System.out.println(config.getMainNode());
        System.out.println(config.getNtpListenPort());
        System.out.println(config.getHttpPort());
        System.out.println(config.getP2pPort());
    }
}
