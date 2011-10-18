package com.netease.xmpp.robot.s2c.gate;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

public class MessageGate {
    private static MessageGate instance = null;

    private int port = 5000;
    private ChannelFactory factory = null;
    private Channel serverChannel = null;

    public MessageGate(int port) {
        if (instance != null) {
            throw new IllegalStateException("A robot is already running");
        }

        this.port = port;
        instance = this;
    }

    public void start() {
        factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors
                .newCachedThreadPool());
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("child.tcpNoDelay", true);
        config.put("child.keepAlive", true);

        ServerBootstrap gateBootstrap = new ServerBootstrap(factory);
        gateBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() {
                return Channels.pipeline(new HttpRequestDecoder(), new MessageChannelHandler(),
                        new HttpResponseEncoder());
            }
        });

        gateBootstrap.setOptions(config);
        serverChannel = gateBootstrap.bind(new InetSocketAddress(port));

        System.out.println("MSG GATE BINDED AT " + port);
    }

    public void stop() {
        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly();
        }

        if (factory != null) {
            factory.releaseExternalResources();
        }
    }
}
