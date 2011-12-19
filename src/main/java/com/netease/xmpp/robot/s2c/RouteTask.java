package com.netease.xmpp.robot.s2c;

import org.jivesoftware.smack.packet.PacketExtension;

public class RouteTask implements Runnable {
    private String user = null;
    private String msg = null;
    private boolean check;
    private PacketExtension extention = null;

    public RouteTask(String user, String msg, boolean check) {
        this.user = user;
        this.msg = msg;
        this.check = check;
    }

    public RouteTask(String user, PacketExtension extention, boolean check) {
        this.user = user;
        this.extention = extention;
        this.check = check;
    }

    @Override
    public void run() {
        ConnectionWorkerThread workerThread = (ConnectionWorkerThread) Thread.currentThread();

        if (extention != null) {
            workerThread.deliver(extention, user, check);
        } else {
            workerThread.deliver(msg, user, check);
        }
    }
}
