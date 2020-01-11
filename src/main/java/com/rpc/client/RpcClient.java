package com.rpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private String serverAddr;
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
    public RpcClient(String serverAddr){
        this.serverAddr = serverAddr;
    }

    public void submit(Runnable task){
            threadPoolExecutor.submit(task);
    }

}
