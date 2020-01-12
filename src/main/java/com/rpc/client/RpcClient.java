package com.rpc.client;

import com.rpc.registry.DiscoveryService;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RpcClient {
    private DiscoveryService discoveryService;
    private String serverAddress;

    public RpcClient(String serverAddress){
        this.serverAddress = serverAddress;
    }

    public RpcClient(DiscoveryService discoveryService){
        this.discoveryService = discoveryService;
    }

    public RpcClient(String serverAddress, DiscoveryService discoveryService){
        this.serverAddress = serverAddress;
        this.discoveryService = discoveryService;
    }


    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    public static void submit(Runnable task){
        threadPoolExecutor.submit(task);
    }
}
