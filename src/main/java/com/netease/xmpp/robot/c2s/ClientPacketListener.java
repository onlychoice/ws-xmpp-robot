package com.netease.xmpp.robot.c2s;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import com.netease.xmpp.robot.Robot;

public class ClientPacketListener implements PacketListener {
    private static Logger logger = Logger.getLogger(ClientPacketListener.class);

    private static final String USER_PARA = "user";

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    class RequestTask implements Runnable {
        private String user = null;
        private String message = null;
        private String appServerAddr = null;

        public RequestTask(String user, String message) {
            this.user = user;
            this.message = message;
            this.appServerAddr = Robot.getInstance().getAppServerAddr();
        }

        @Override
        public void run() {
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
            PostMethod method = new PostMethod(appServerAddr);

            RequestEntity entity;
            try {
                entity = new StringRequestEntity(message, "text/html;charset=GBK", "GBK");
                method.setRequestEntity(entity);
                method.setRequestHeader(USER_PARA, user);

                int statusCode = client.executeMethod(method);
                logger.debug(">>> Send msg: " + message);
                if (statusCode == HttpStatus.SC_OK) {
                    String result = method.getResponseBodyAsString();

                    logger.debug(">>> Response: " + result);
                    Robot.getInstance().getServerSurrogate().send(result, user, false);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void processPacket(Packet packet) {
        Message msg = (Message) packet;
        String jid = msg.getFrom();
        String user = jid.substring(0, jid.indexOf("@"));
        user = user.replace("\\40", "@");
        String content = msg.getBody();

        logger.debug(">>> Recv msg from: " + user + ": " + content);

        threadPool.execute(new RequestTask(user, content));
    }
}
