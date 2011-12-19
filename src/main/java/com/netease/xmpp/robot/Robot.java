package com.netease.xmpp.robot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.netease.xmpp.master.client.ClientGlobal;
import com.netease.xmpp.master.client.SyncClient;
import com.netease.xmpp.robot.s2c.ServerSurrogate;
import com.netease.xmpp.robot.s2c.gate.MessageGate;
import com.netease.xmpp.util.ResourceUtils;

public class Robot {
    private static final String CONFIG_FILE = "robot.properties";
    private static final String ROBOT_NAME = "robot";
    private static final String ROBOT_PASSWORD = "robot";

    private static final String KEY_ROBOT_GATE_PORT = "robot.gate.port";
    private static final String KEY_ROBOT_APP_ADDR = "robot.app.addr";

    private static final int DEFAULT_GATE_PORT = 5000;

    private static Robot instance = null;

    private int gatePort = DEFAULT_GATE_PORT;
    private String appServerAddr = null;

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

        if (!loadConfig()) {
            throw new RuntimeException("Load config file failed.");
        }

        instance = this;

        ClientGlobal.setIsClientStarted(false);

        robotClient = new RobotSyncClient(SyncClient.CLIENT_TYPE_ROBOT);
        robotClient.start();
    }

    private boolean loadConfig() {
        try {
            Properties prop = new Properties();
            InputStream input = null;
            String configFilePath = CONFIG_FILE;
            input = ResourceUtils.getResourceAsStream(configFilePath);

            prop.load(input);

            String value = prop.getProperty(KEY_ROBOT_GATE_PORT);
            if (value != null) {
                gatePort = Integer.parseInt(value);
            }

            appServerAddr = prop.getProperty(KEY_ROBOT_APP_ADDR);
            if (appServerAddr == null) {
                return false;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void start() {
        // Open the door
        serverSurrogate = new ServerSurrogate();
        serverSurrogate.start();

        messageGate = new MessageGate(this.gatePort);
        messageGate.start();

        ClientGlobal.setIsClientStarted(true);
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

    public String getAppServerAddr() {
        return appServerAddr;
    }

    public int getGatePort() {
        return gatePort;
    }

    public static void main(String[] args) throws Exception {
         new Robot();
    }
}
