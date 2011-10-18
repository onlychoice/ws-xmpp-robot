package com.netease.xmpp.robot.s2c.gate;

import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

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
    public MessageChannelHandler() {
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        HttpRequest httpRequest = (HttpRequest) e.getMessage();
        String user = httpRequest.getHeader("user");
        String msg = new String(httpRequest.getContent().array());

        Robot.getInstance().getServerSurrogate().send(msg, user);

        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, new HttpResponseStatus(200,
                "Message Gate"));

        Channel ch = e.getChannel();
        ch.write(res);
        ch.disconnect();
        ch.close();
    }
}
