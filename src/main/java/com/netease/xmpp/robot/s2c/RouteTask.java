package com.netease.xmpp.robot.s2c;

public class RouteTask implements Runnable {
    private String user;
    private String msg;
    
    public RouteTask(String user, String msg) {
        this.user = user;
        this.msg = msg;
    }

    @Override
    public void run() {
        ConnectionWorkerThread workerThread = (ConnectionWorkerThread) Thread.currentThread();
        workerThread.deliver(msg, user);
    }
}
