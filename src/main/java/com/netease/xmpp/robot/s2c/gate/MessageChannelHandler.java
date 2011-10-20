package com.netease.xmpp.robot.s2c.gate;

import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.netease.xmpp.robot.Robot;

public class MessageChannelHandler extends SimpleChannelHandler {
    private static final String USER_PARA = "user";
    private static final String MSG_PARA = "msg";

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        HttpRequest httpRequest = (HttpRequest) e.getMessage();
        String buf[] = httpRequest.getContent().toString(Charset.forName("UTF-8")).split("&");
        if (buf.length == 2) {
            // user names & message
            try {
                String userNameList = URLDecoder.decode(buf[0].substring(buf[0].indexOf(USER_PARA)
                        + USER_PARA.length() + 1), "UTF-8");
                String msg = URLDecoder.decode(buf[1].substring(buf[0].indexOf(MSG_PARA)
                        + MSG_PARA.length() + 1), "UTF-8");

                String[] userNameArray = userNameList.split(",");
                for (String user : userNameArray) {
                    Robot.getInstance().getServerSurrogate().send(msg, user.trim());
                }
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
        }

        // TODO check online status

        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, new HttpResponseStatus(200,
                "Message Gate"));

        Channel ch = e.getChannel();
        ch.write(res);
        ch.disconnect();
        ch.close();
    }
}
