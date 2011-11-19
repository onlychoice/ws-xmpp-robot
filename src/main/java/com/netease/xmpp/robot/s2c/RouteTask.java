package com.netease.xmpp.robot.s2c;

public class RouteTask implements Runnable {
    private String user;
    private String msg;
    private boolean check;
    
    public RouteTask(String user, String msg, boolean check) {
        this.user = user;
        this.msg = msg;
        this.check = check;
    }

    @Override
    public void run() {
        ConnectionWorkerThread workerThread = (ConnectionWorkerThread) Thread.currentThread();
        workerThread.deliver(msg, user, check);
    }
}
