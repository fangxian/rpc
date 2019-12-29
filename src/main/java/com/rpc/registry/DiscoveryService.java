package com.rpc.registry;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryService.class);

    private CountDownLatch latch = new CountDownLatch(1);

    private String registerAddr;
    private String registerPort;
    private String zookeeperServer;
    private ZooKeeper zooKeeper;

    private List<String> dataList;

    public DiscoveryService(String registerAddr, String port){
        this.registerAddr = registerAddr;
        this.registerPort = port;
        this.zookeeperServer = registerAddr+":"+port;
    }

    public DiscoveryService(String zookeeperServer){
        this.zookeeperServer = zookeeperServer;
        this.registerAddr = zookeeperServer.split(":")[0];
        this.registerPort = zookeeperServer.split("")[1];
    }

    //connect zookeeper
    public void connectServer() {
        try{
            if(zooKeeper == null){
                zooKeeper = new ZooKeeper(zookeeperServer, ConstantClass.ZK_SESSION_TIMEOUT, new Watcher() {
                    @Override
                    public void process(WatchedEvent watchedEvent) {
                        if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                            latch.countDown();
                            logger.info("connect zookeeper server success.");
                        }
                    }
                });
                latch.await();
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.toString());
        }
    }

    public String discover(){
        String data = null;

        int size = dataList.size();


        return data;
    }


}
