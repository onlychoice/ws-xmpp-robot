package com.netease.xmpp.robot.monitor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class TestRequestListener implements RequestListener {
    private AtomicLong requestNum = new AtomicLong(0);
    private AtomicLong responseNum = new AtomicLong(0);

    private Executor executor = Executors.newSingleThreadExecutor();

    private static TestRequestListener instance = null;

    public static TestRequestListener getInstance() {
        if (instance == null) {
            instance = new TestRequestListener();
        }
        return instance;
    }

    class RequestChecker implements Runnable {
        @Override
        public void run() {
            while (true) {
                System.out.println("Request number: " + requestNum.get());
                System.out.println("Response number: " + responseNum.get());

                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException e) {
                    // Do nothing, continue
                }
            }
        }
    }

    private TestRequestListener() {
        executor.execute(new RequestChecker());
    }

    @Override
    public void onRequest(String jid, String message) {
        requestNum.incrementAndGet();
    }

    @Override
    public void onResponse(String jid, String result) {
        responseNum.incrementAndGet();
    }
}
