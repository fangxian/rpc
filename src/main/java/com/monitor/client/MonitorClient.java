package com.monitor.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class MonitorClient {
    private static Logger logger = LoggerFactory.getLogger(MonitorClient.class);
    private String host;
    private int port;
    private Map<String, Integer> interfaceCount = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock();

    private MonitorClient(String serverAddress) {
        host = serverAddress.split(":")[0];
        port = Integer.parseInt(serverAddress.split(":")[1]);
    }


    public void startClient() throws Exception{
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new MonitorClientInitializer(this));
            ChannelFuture f = b.connect(host, port);
            f.channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public Map<String, Integer> getCount() {
        lock.lock();
        Map<String, Integer> countMap = interfaceCount;
        lock.unlock();
        return countMap;
    }

    public void updateCount(Map<String, Integer> map) {
        lock.lock();
        interfaceCount = map;
        lock.unlock();
    }

}
