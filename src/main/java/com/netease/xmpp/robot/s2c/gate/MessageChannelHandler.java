package com.netease.xmpp.robot.s2c.gate;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.netease.xmpp.robot.Robot;

public class MessageChannelHandler extends SimpleChannelHandler {
    private static final String USER_PARA = "user";
    private static final String MSG_PARA = "msg";

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    class GateWorker implements Runnable {
        private MessageEvent e = null;

        public GateWorker(MessageEvent e) {
            this.e = e;
        }

        @Override
        public void run() {
            Channel ch = e.getChannel();

            HttpRequest httpRequest = (HttpRequest) e.getMessage();
            String buf[] = httpRequest.getContent().toString(Charset.forName("UTF-8")).split("&");

            HttpResponseStatus res = null;
            if (buf.length == 2) {
                // user names & message
                try {
                    String userNameList = URLDecoder.decode(buf[0].substring(buf[0]
                            .indexOf(USER_PARA)
                            + USER_PARA.length() + 1), "UTF-8");
                    String msg = URLDecoder.decode(buf[1].substring(buf[0].indexOf(MSG_PARA)
                            + MSG_PARA.length() + 1), "UTF-8");

                    String[] userNameArray = userNameList.split(",");
                    for (String user : userNameArray) {
                        Robot.getInstance().getServerSurrogate().send(msg, user.trim());
                    }

                    res = HttpResponseStatus.OK;
                } catch (UnsupportedEncodingException ex) {
                    // Do nothing
                }
            }

            if (res == null) {
                res = HttpResponseStatus.BAD_REQUEST;
            }
            ch.write(res);
            ch.disconnect();
            ch.close();
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        threadPool.execute(new GateWorker(e));
    }
}
