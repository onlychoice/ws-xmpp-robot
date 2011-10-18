/**
 * $RCSfile$ $Revision: $ $Date: $
 * 
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * 
 * This software is published under the terms of the GNU Public License (GPL), a copy of which is
 * included in this distribution.
 */

package com.netease.xmpp.robot.s2c;

import com.netease.xmpp.master.common.ServerHashProtos.Server.ServerHash;
import com.netease.xmpp.robot.Robot;
import com.netease.xmpp.robot.c2s.ClientPacketFilter;
import com.netease.xmpp.robot.c2s.ClientPacketListener;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * Thread that creates and keeps a connection to the server. This thread is responsable for actually
 * forwarding clients traffic to the server. If the connection is no longer active then the thread
 * is going to be discarded and a new one is created and added to the thread pool that is kept in
 * {@link ServerSurrogate}.
 * 
 * @author Gaston Dombiak
 */
public class ConnectionWorkerThread extends Thread {
    private static Logger logger = Logger.getLogger(ConnectionWorkerThread.class);
    /**
     * The default XMPP port for connection multiplex.
     */
    public static final int DEFAULT_MULTIPLEX_PORT = 5222;

    private String robotName;
    private String robotPassword;

    private String serverDomain = "blogp23v4.space.163.org";
    /**
     * Connection to the server.
     */
    private XMPPConnection connection;

    private ServerHash serverHash;

    public ConnectionWorkerThread(ThreadGroup group, Runnable target, String name, long stackSize,
            ServerHash serverHash) {
        super(group, target, name, stackSize);
        this.robotName = Robot.getInstance().getRobotName();
        this.robotPassword = Robot.getInstance().getRobotPassword();
        this.serverHash = serverHash;
        // Create connection to the server
        createConnection();
    }

    /**
     * Returns true if there is a connection to the server that is still active. Note that sometimes
     * a socket assumes to be opened when in fact the underlying TCP socket connection is closed. To
     * detect these cases we rely on heartbeats or timing out when writing data hasn't finished for
     * a while.
     * 
     * @return rue if there is a connection to the server that is still active.
     */
    public boolean isValid() {
        return connection != null && connection.isConnected();
    }

    /**
     * Returns the connection to the server.
     * 
     * @return the connection to the server.
     */
    public XMPPConnection getConnection() {
        return connection;
    }

    /**
     * Creates a new connection to the server
     * 
     * @return true if a connection to the server was established
     */
    private boolean createConnection() {
        ConnectionConfiguration config = new ConnectionConfiguration(serverHash.getIp(),
                DEFAULT_MULTIPLEX_PORT);
        connection = new XMPPConnection(config);
        try {
            connection.connect();
            connection.login(robotName, robotPassword);
            connection.addPacketListener(new ClientPacketListener(), new ClientPacketFilter());

            System.out.println("CONNECTION DONE: " + serverHash.getIp());

            return true;
        } catch (XMPPException e) {
            if (connection != null) {
                connection.disconnect();
            }
            return false;
        }
    }

    public void run() {
        try {
            super.run();
        } catch (IllegalStateException e) {
            // Do not print this exception that was thrown to stop this thread when
            // it was detected that the connection was closed before using this thread
        } finally {
            // Remove this thread/connection from the list of available connections
            Robot.getInstance().getServerSurrogate().serverConnections.remove(getName());
            // Close the connection
            connection.disconnect();
        }
    }

    /**
     * Indicates the server that the connection manager is being shut down.
     */
    void notifySystemShutdown() {
        connection.disconnect();
    }

    /**
     * Delivers clients traffic to the server. The client session that originated the traffic is
     * specified by the streamID attribute. Clients traffic is wrapped by a <tt>route</tt> element.
     * 
     * @param stanza
     *            the original client stanza that is going to be wrapped.
     * @param streamID
     *            the stream ID assigned by the connection manager to the client session.
     */
    public void deliver(String stanza, String user) {
        user = user.replace("@", "\\40");
        Message message = new Message(user + "@" + serverDomain);
        message.setBody(stanza);

        // Forward the wrapped stanza to the server
        connection.sendPacket(message);
    }
}