package com.netease.xmpp.robot;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.netease.xmpp.master.client.ClientConfigCache;
import com.netease.xmpp.master.client.ClientGlobal;
import com.netease.xmpp.master.common.Message;
import com.netease.xmpp.master.common.ServerListProtos.Server.ServerInfo;
import com.netease.xmpp.master.event.client.ServerUpdateEventHandler;
import com.netease.xmpp.robot.s2c.ServerSurrogate;

public class RobotServerUpdateEventHandler extends ServerUpdateEventHandler {
    public RobotServerUpdateEventHandler(ClientConfigCache config) {
        super(config);
    }

    @Override
    public void serverInfoUpdated(Message message, List<ServerInfo> serverHashList) {
        TreeMap<Long, ServerInfo> oldServerNodes = ClientGlobal.getServerNodes();
        TreeMap<Long, ServerInfo> newServerNodes = new TreeMap<Long, ServerInfo>();
        TreeMap<Long, ServerInfo> invalidServerNodes = new TreeMap<Long, ServerInfo>();
        TreeMap<Long, ServerInfo> addServerNodes = new TreeMap<Long, ServerInfo>();

        synchronized (oldServerNodes) {
            for (ServerInfo node : serverHashList) {
                newServerNodes.put(node.getHash(), node);
            }

            for (Map.Entry<Long, ServerInfo> entry : oldServerNodes.entrySet()) {
                if (!newServerNodes.containsKey(entry.getKey())) {
                    invalidServerNodes.put(entry.getKey(), entry.getValue());
                } else {
                    ServerInfo s1 = newServerNodes.get(entry.getKey());
                    ServerInfo s2 = entry.getValue();
                    
                    if(!(s1.getIp().equals(s2.getIp()) && s1.getClientPort() == s2.getClientPort())) {
                        invalidServerNodes.put(entry.getKey(), entry.getValue());
                    }
                }
            }          
            
            for (Map.Entry<Long, ServerInfo> entry : newServerNodes.entrySet()) {
                if (!oldServerNodes.containsKey(entry.getKey())) {
                    addServerNodes.put(entry.getKey(), entry.getValue());
                }else {
                    ServerInfo s1 = oldServerNodes.get(entry.getKey());
                    ServerInfo s2 = entry.getValue();
                    
                    if(!(s1.getIp().equals(s2.getIp()) && s1.getClientPort() == s2.getClientPort())) {
                        addServerNodes.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            
            ClientGlobal.setServerNodes(newServerNodes);
            
            if (ClientGlobal.getIsClientStartup()) {
                ServerSurrogate serverSurrogate = Robot.getInstance()
                        .getServerSurrogate();
                serverSurrogate.updateServerConnection(invalidServerNodes, addServerNodes);
            }
            
            oldServerNodes.clear();
        }
    }
}
