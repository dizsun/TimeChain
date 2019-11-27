package com.dizsun.timechain.service;


import com.alibaba.fastjson.JSON;
import com.dizsun.timechain.util.DateUtil;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by sunysen on 2017/7/6.
 */
public class HTTPService {
    private BlockService blockService;
    private PeerService peerService;

    public HTTPService() {

    }

    public void initHTTPServer(int port) {
        this.blockService = BlockService.getInstance();
        this.peerService=PeerService.getInstance();
        try {
            Server server = new Server(port);
            System.out.println("listening http port on: " + port);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);
            context.addServlet(new ServletHolder(new BlocksServlet()), "/blocks");
//            context.addServlet(new ServletHolder(new MineBlockServlet()), "/mineBlock");
            context.addServlet(new ServletHolder(new PeersServlet()), "/peers");
            context.addServlet(new ServletHolder(new AddPeerServlet()), "/addPeer");
            //context.addServlet(new ServletHolder(new TimeCenterServlet()), "/setTC");

            server.start();
            server.join();
        } catch (Exception e) {
            System.out.println("init http server is error:" + e.getMessage());
        }
    }

    private class BlocksServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().println(JSON.toJSONString(blockService.getBlockChain()));
        }
    }

    /**
     * 格式peer=http://192.168.1.1:6001
     */
    private class AddPeerServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            this.doPost(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setCharacterEncoding("UTF-8");
            String peer = req.getParameter("peer");
            peerService.connectToPeer(peer);
            resp.getWriter().print("ok");
        }
    }


    private class PeersServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().println(JSON.toJSONString(peerService.getPeerArray()));
        }
    }

//    private class TimeCenterServlet extends HttpServlet{
//        @Override
//        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//            this.doPost(req,resp);
//        }
//
//        @Override
//        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//            resp.setCharacterEncoding("UTF-8");
//            String host = req.getParameter("host");
//            DateUtil dateUtil=DateUtil.newDataUtil();
//            dateUtil.setHost(host);
//        }
//    }
}

