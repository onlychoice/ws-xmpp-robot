package com.netease.xmpp.robot.s2c;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.netease.xmpp.robot.RobotGlobal;

public class TaskExecutor {
    private static Logger logger = Logger.getLogger(TaskExecutor.class);

    private LinkedBlockingQueue<Runnable> clientTaskQueue = new LinkedBlockingQueue<Runnable>();
    private LinkedBlockingQueue<Runnable> serverTaskQueue = new LinkedBlockingQueue<Runnable>();

    private ThreadPoolExecutor threadPool;

    public Thread clientTaskExecutor = null;
    public Thread serverTaskExecutor = null;

    private static TaskExecutor instance = null;

    private TaskExecutor() {
        // Create a pool of threads that will process incoming packets.
        int maxThreads = 5;

        threadPool = new ThreadPoolExecutor(maxThreads * 2, maxThreads * 2, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());

        clientTaskExecutor = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Runnable task = clientTaskQueue.take();
                        if (RobotGlobal.getIsUpdating()) {
                            synchronized (clientTaskExecutor) {
                                clientTaskExecutor.wait();
                            }
                        }
                        threadPool.execute(task);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        });

        serverTaskExecutor = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Runnable task = serverTaskQueue.take();
                        if (RobotGlobal.getIsUpdating()) {
                            synchronized (serverTaskExecutor) {
                                serverTaskExecutor.wait();
                            }
                        }

                        threadPool.execute(task);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        });
    }

    public static TaskExecutor getInstance() {
        if (instance == null) {
            instance = new TaskExecutor();
        }

        return instance;
    }

    public void start() {
        clientTaskExecutor.start();
        serverTaskExecutor.start();
    }
    
    public void resume() {
        synchronized (clientTaskExecutor) {
            clientTaskExecutor.notify();
        }
        
        synchronized (serverTaskExecutor) {
            serverTaskExecutor.notify();
        }
    }

    public void addClientTask(Runnable task) {
        if (!clientTaskQueue.offer(task)) {
            logger.error("Add client task fail.");
        }
    }

    public void addServerTask(Runnable task) {
        if (!serverTaskQueue.offer(task)) {
            logger.error("Add server task fail.");
        }
    }
}
