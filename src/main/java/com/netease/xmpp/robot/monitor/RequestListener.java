package com.netease.xmpp.robot.monitor;

public interface RequestListener {
    public void onRequest(String jid, String message);
    public void onResponse(String jid, String result);
}
