/**
 * $RCSfile$ $Revision: $ $Date: $
 * 
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * 
 * This software is published under the terms of the GNU Public License (GPL), a copy of which is
 * included in this distribution.
 */

package com.netease.xmpp.robot.s2c;

import com.netease.xmpp.master.client.ClientConfigCache;
import com.netease.xmpp.master.common.ServerListProtos.Server.ServerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Surrogate of the main server where the Connection Manager is routing client packets. This class
 * is responsible for keeping a pool of working threads to processing incoming clients traffic and
 * forward it to the main server. Each working thread uses its own connection to the server. By
 * default 5 threads/connections are established to the server. Use the system property
 * <tt>xmpp.manager.connections</tt> to modify the default value.
 * <p>
 * 
 * ServerSurrogate is also responsible for caching the server configuration such as if non-sasl
 * authentication or in-band registration are available.
 * <p>
 * 
 * Each connection to the server has its own {@link ServerPacketReader} to read incoming traffic
 * from the server. Incoming server traffic is then handled by {@link ServerPacketHandler}.
 * 
 * @author Gaston Dombiak
 */
public class ServerSurrogate {
    /**
     * Pool of threads that will send stanzas to the server. The number of threads in the pool will
     * match the number of connections to the server.
     */
    // private ThreadPoolExecutor threadPool;
    /**
     * Map that holds the list of connections to the server. Key: thread name, Value:
     * ConnectionWorkerThread.
     */
    Map<String, ConnectionWorkerThread> serverConnections = new ConcurrentHashMap<String, ConnectionWorkerThread>(
            0);

    // Proxy
    private ClientConfigCache clientConfig = null;
    private List<ServerInfo> serverList = new ArrayList<ServerInfo>();
    private List<ThreadPoolExecutor> threadPoolList = new LinkedList<ThreadPoolExecutor>();
    private Map<String, Integer> threadPoolIndexMap = new HashMap<String, Integer>();

    public ServerSurrogate() {
        clientConfig = ClientConfigCache.getInstance();
    }

    private String getKey(ServerInfo sh) {
        StringBuilder sb = new StringBuilder();
        sb.append(sh.getIp());
        sb.append(":");
        sb.append(sh.getClientPort());

        return sb.toString();
    }

    public void start() {
        HashSet<String> serverSet = new HashSet<String>();
        TreeMap<Long, ServerInfo> serverNodes = clientConfig.getServerNodes();
        for (Map.Entry<Long, ServerInfo> entry : serverNodes.entrySet()) {
            ServerInfo sh = entry.getValue();
            if (serverSet.add(getKey(sh))) {
                serverList.add(sh);
            }
        }

        for (int i = 0; i < serverList.size(); i++) {
            ServerInfo sh = serverList.get(i);
            // Create empty thread pool
            ThreadPoolExecutor t = createThreadPool(sh);
            // Populate thread pool with threads that will include connections to the server
            t.prestartAllCoreThreads();

            threadPoolList.add(t);
            threadPoolIndexMap.put(getKey(sh), i);
        }
    }

    /**
     * Closes existing connections to the server. A new thread pool will be created but no
     * connections will be created. New connections will be created on demand.
     */
    void closeAll() {
        shutdown(true);

        // Create new thread pool but this time do not populate it
        for (int i = 0; i < serverList.size(); i++) {
            // Create empty thread pool
            threadPoolList.add(i, createThreadPool(serverList.get(i)));
        }
    }

    /**
     * Closes connections of connected clients and stops forwarding clients traffic to the server.
     * If the server is the one that requested to stop forwarding traffic then stop doing it now.
     * This means that queued packets will be discarded, otherwise stop queuing packet but continue
     * processing queued packets.
     * 
     * @param now
     *            true if forwarding packets should be done now.
     */
    void shutdown(boolean now) {
        // Shutdown the threads that send stanzas to the server
        if (now) {
            for (int i = 0; i < threadPoolList.size(); i++) {
                threadPoolList.get(i).shutdownNow();
            }
        } else {
            for (int i = 0; i < threadPoolList.size(); i++) {
                threadPoolList.get(i).shutdown();
            }
        }
    }

    public void updateServerConnection(TreeMap<Long, ServerInfo> invalidServerNodes,
            TreeMap<Long, ServerInfo> addServerNodes) {
        // Assume that the server duplicate number is fix number and not changed during runtime
        HashSet<String> serverSet = new HashSet<String>();
        List<ServerInfo> invalidServerList = new ArrayList<ServerInfo>();
        for (Map.Entry<Long, ServerInfo> entry : invalidServerNodes.entrySet()) {
            ServerInfo sh = entry.getValue();
            if (serverSet.add(getKey(sh))) {
                invalidServerList.add(sh);
            }
        }

        for (ServerInfo sh : invalidServerList) {
            String key = getKey(sh);
            Integer index = threadPoolIndexMap.get(key);
            if (index != null) {
                serverList.remove(index);
                threadPoolList.remove(index);
                threadPoolIndexMap.remove(key);
                for (Map.Entry<String, Integer> e : threadPoolIndexMap.entrySet()) {
                    if (e.getValue() > index) {
                        threadPoolIndexMap.put(e.getKey(), e.getValue() - 1);
                    }
                }
            }
        }

        serverSet.clear();

        List<ServerInfo> addServerList = new ArrayList<ServerInfo>();
        for (Map.Entry<Long, ServerInfo> entry : addServerNodes.entrySet()) {
            ServerInfo sh = entry.getValue();
            if (serverSet.add(getKey(sh))) {
                addServerList.add(sh);
            }
        }

        for (ServerInfo sh : addServerList) {
            serverList.add(sh);

            ThreadPoolExecutor t = createThreadPool(sh);
            // Populate thread pool with threads that will include connections to the server
            t.prestartAllCoreThreads();

            threadPoolList.add(t);
            threadPoolIndexMap.put(getKey(sh), threadPoolList.size() - 1);
        }
    }

    /**
     * Forwards the specified stanza to the server. The client that is sending the stanza is
     * specified by the streamID parameter.
     * 
     * @param stanza
     *            the stanza to send to the server.
     * @param streamID
     *            the stream ID assigned by the connection manager to the session.
     */
    public void send(String stanza, String user) {
        long hash = clientConfig.getHashAlgorithm().hash(user);

        ServerInfo server = clientConfig.getServerNodeForKey(hash);
        int index = threadPoolIndexMap.get(getKey(server));
        
        threadPoolList.get(index).execute(new RouteTask(user, stanza));
    }

    /**
     * Creates a new thread pool that will not contain any thread. So new connections won't be
     * created to the server at this point.
     * 
     * @param serverHash
     */
    private ThreadPoolExecutor createThreadPool(ServerInfo serverInfo) {
        // Create a pool of threads that will process queued packets.
        return new ConnectionWorkerThreadPool(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new ConnectionsWorkerFactory(serverInfo),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * ThreadPoolExecutor that verifies connection status before executing a task. If the connection
     * is invalid then the worker thread will be dismissed and the task will be injected into the
     * pool again.
     */
    private class ConnectionWorkerThreadPool extends ThreadPoolExecutor {
        public ConnectionWorkerThreadPool(int corePoolSize, int maximumPoolSize,
                long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
                ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
                    handler);
        }

        protected void beforeExecute(Thread thread, Runnable task) {
            super.beforeExecute(thread, task);
            ConnectionWorkerThread workerThread = (ConnectionWorkerThread) thread;
            // Check that the worker thread is valid. This means that it has a valid connection
            // to the server
            if (!workerThread.isValid()) {
                // Request other thread to process the task. In fact, a new thread
                // will be created by the
                execute(task);
                // Throw an exception so that this worker is dismissed
                throw new IllegalStateException(
                        "There is no connection to the server or connection is lost.");
            }
        }

        public void shutdown() {
            // Notify the server that the connection manager is being shut down
            execute(new Runnable() {
                public void run() {
                    ConnectionWorkerThread thread = (ConnectionWorkerThread) Thread.currentThread();
                    thread.notifySystemShutdown();
                }
            });
            // Stop the workers and shutdown
            super.shutdown();
        }
    }

    /**
     * Factory of threads where is thread will create and keep its own connection to the server. If
     * creating new connections to the server failes 2 consecutive times then existing client
     * connections will be closed.
     */
    private class ConnectionsWorkerFactory implements ThreadFactory {
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final AtomicInteger failedAttempts = new AtomicInteger(0);
        private ServerInfo serverInfo = null;

        ConnectionsWorkerFactory(ServerInfo serverHash) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();

            this.serverInfo = serverHash;
        }

        public Thread newThread(Runnable r) {
            // Create new worker thread that will include a connection to the server
            ConnectionWorkerThread t = new ConnectionWorkerThread(group, r, "Connection Worker - "
                    + threadNumber.getAndIncrement(), 0, serverInfo);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            // Return null if failed to create worker thread
            if (!t.isValid()) {
                int attempts = failedAttempts.incrementAndGet();
                if (attempts == 2 && serverConnections.size() == 0) {
                    // Server seems to be unavailable so close existing client connections
                    closeAll();
                    // Clean up the counter of failed attemps to create new connections
                    failedAttempts.set(0);
                }
                return null;
            }
            // Clean up the counter of failed attemps to create new connections
            failedAttempts.set(0);
            // Update number of available connections to the server
            serverConnections.put(t.getName(), t);
            return t;
        }
    }
}
