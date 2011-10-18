package com.netease.xmpp.robot;

import com.netease.xmpp.master.client.SyncClient;
import com.netease.xmpp.robot.s2c.ServerSurrogate;
import com.netease.xmpp.robot.s2c.gate.MessageGate;

public class Robot {
    private static Robot instance = null;

    private String ROBOT_NAME = "robot";
    private String ROBOT_PASSWORD = "robot";

    public static Robot getInstance() {
        return instance;
    }

    private SyncClient robotClient;
    private ServerSurrogate serverSurrogate;
    private MessageGate messageGate;

    public Robot() {
        if (instance != null) {
            throw new IllegalStateException("A robot is already running");
        }
        
        instance = this;

        RobotGlobal.setIsRobotStartup(false);

        robotClient = new SyncClient(SyncClient.CLIENT_TYPE_ROBOT);
        robotClient.start();
    }

    public void start() {
        // Open the door
        serverSurrogate = new ServerSurrogate();
        serverSurrogate.start();

        messageGate = new MessageGate(5000);
        messageGate.start();

        RobotGlobal.setIsRobotStartup(true);
    }

    public ServerSurrogate getServerSurrogate() {
        return serverSurrogate;
    }

    public String getRobotName() {
        return ROBOT_NAME;
    }

    public String getRobotPassword() {
        return ROBOT_PASSWORD;
    }

    public static void main(String[] args) throws Exception {
        new Robot();
    }
}
