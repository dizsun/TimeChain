package com.dizsun.timechain.util;

import com.dizsun.timechain.component.Block;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLUtil {
    private static String Drivder ="org.sqlite.JDBC";
    private static String url="jdbc:sqlite:./db/blocks.db";

    //测试数据库代码
//    public static void main(String[] args) {
//        try {
//            Class.forName(Drivder);// 加载驱动,连接sqlite的jdbc
//            Connection connection= DriverManager.getConnection("jdbc:sqlite:db/blocks.db");//连接数据库zhou.db,不存在则创建
//            Statement statement=connection.createStatement();   //创建连接对象，是Java的一个操作数据库的重要接口
//            //String sql="create table tables(name varchar(20),pwd varchar(20))";
//            statement.executeUpdate("drop table if exists blocks");//判断是否有表tables的存在。有则删除
//            //statement.executeUpdate(sql);            //创建数据库
//            //statement.executeUpdate("insert into tables values('zhou','156546')");//向数据库中插入数据
////            ResultSet rSet=statement.executeQuery("select * from tables");//搜索数据库，将搜索的放入数据集ResultSet中
////            while (rSet.next()) {            //遍历这个数据集
////                System.out.println("姓名："+rSet.getString(1));//依次输出 也可以这样写 rSet.getString(“name”)
////                System.out.println("密码："+rSet.getString("pwd"));
////            }
////            rSet.close();//关闭数据集
//            connection.close();//关闭数据库连接
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    /**
     * 取出区块链
     * @return
     */
    public List<Block> queryBlocks() {
        List<Block> dbBlocks=new ArrayList<Block>();
        try {
            Class.forName(Drivder);// 加载驱动,连接sqlite的jdbc
            Connection connection= DriverManager.getConnection(url);//连接数据库zhou.db,不存在则创建
            Statement statement=connection.createStatement();   //创建连接对象，是Java的一个操作数据库的重要接口
            ResultSet rSet=statement.executeQuery("select * from blocks");//搜索数据库，将搜索的放入数据集ResultSet中
            while (rSet.next()) {
                Block block = new Block();
                block.setIndex(rSet.getInt("index"));
                block.setPreviousHash(rSet.getString("previousHash"));
                Timestamp timestamp = rSet.getTimestamp("timestamp");
                block.setTimestamp(timestamp.getTime());
                block.setData(rSet.getString("data"));
                block.setHash(rSet.getString("hash"));
//                block.setProof(rSet.getInt("proof"));
                dbBlocks.add(block);
            }
            rSet.close();//关闭数据集
            connection.close();//关闭数据库连接
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if(dbBlocks.isEmpty())
            return null;
        else return dbBlocks;
    }

    /**
     * 初始化数据库的区块链
     * @param blockChain
     */
    public void initBlocks(List<Block> blockChain) {
        try {
            Class.forName(Drivder);// 加载驱动,连接sqlite的jdbc
            Connection connection= DriverManager.getConnection(url);//连接数据库,不存在则创建
            Statement statement=connection.createStatement();   //创建连接对象，是Java的一个操作数据库的重要接口
//            String sql="create table blocks( `id` INTEGER PRIMARY KEY   AUTOINCREMENT,`index` INT NOT NULL," +
//                    "`previousHash` VARCHAR(1024) NOT NULL,`timestamp` TIMESTAMP NOT NULL," +
//                    "`data` VARCHAR(1024) NOT NULL,`hash` VARCHAR(1024) NOT NULL,`proof` INT NOT NULL)";
            String sql="create table blocks(`index` INT PRIMARY KEY NOT NULL," +
                    "`previousHash` VARCHAR(1024) NOT NULL,`timestamp` TIMESTAMP NOT NULL," +
                    "`data` VARCHAR(1024) NOT NULL,`hash` VARCHAR(1024) NOT NULL,`vn` INT NOT NULL)";
            statement.executeUpdate("drop table if exists blocks");//判断是否有表的存在。有则删除
            statement.executeUpdate(sql);            //创建数据库
            if(blockChain!=null) {
                Block block = blockChain.get(0);
                String blockSQL = String.format("INSERT INTO blocks VALUES(%d,'%s',%d,'%s','%s',%d)", block.getIndex(), block.getPreviousHash()
                        , block.getTimestamp(), block.getData(), block.getHash(),block.getVN());
                statement.executeUpdate(blockSQL);//向数据库中插入数据
            }
            connection.close();//关闭数据库连接
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加新区块
     * @param newBlock
     * @return
     */
    public void addBlock(Block newBlock) {
        try {
            Class.forName(Drivder);// 加载驱动,连接sqlite的jdbc
            Connection connection= DriverManager.getConnection(url);//连接数据库zhou.db,不存在则创建
            Statement statement=connection.createStatement();   //创建连接对象，是Java的一个操作数据库的重要接口
            String blockSQL=String.format("INSERT INTO blocks VALUES(%d,'%s',%d,'%s','%s',%d)",newBlock.getIndex(),newBlock.getPreviousHash()
                    ,newBlock.getTimestamp(),newBlock.getData(),newBlock.getHash(),newBlock.getVN());
            statement.executeUpdate(blockSQL);//向数据库中插入数据
            connection.close();//关闭数据库连接
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用新区块链替换旧链
     * @param newBlocks
     */
    public void replaceChain(List<Block> newBlocks) {
        initBlocks(newBlocks);
        for (int i=1;i<newBlocks.size();i++){
            addBlock(newBlocks.get(i));
        }
    }
}
