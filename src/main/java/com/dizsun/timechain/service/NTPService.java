package com.dizsun.timechain.service;

import com.dizsun.timechain.util.Config;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NTPService extends Thread {
    private ServerSocket serverSocket;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(Config.NTPPORT);
            while(true){
                try{
                    Socket inSocket = serverSocket.accept();
                    NTPRequestHandler ntpRequestHandler = new NTPRequestHandler(inSocket);
                    new Thread(ntpRequestHandler).start();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                serverSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private class NTPRequestHandler implements Runnable {
        private Socket mClientSocket;

        public NTPRequestHandler(Socket clientSocket) {
            this.mClientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                DataOutputStream dos = new DataOutputStream(mClientSocket.getOutputStream());
                dos.writeBoolean(true);
                dos.flush();
                dos.close();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
