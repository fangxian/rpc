package com.rpc.registry;

import com.rpc.client.ConnectManage;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

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
        if(size > 0){
            if(size == 1){
                data = dataList.get(0);
                logger.debug("using only data: {}", data);
            } else {
                data = dataList.get(ThreadLocalRandom.current().nextInt(size));
                logger.debug("using random data: {}", data);
            }
        }

        return data;
    }

    private void watchNode(){
        try {
            List<String> nodeList = zooKeeper.getChildren(ConstantClass.ZK_REGISTRY_PATH, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchNode();
                    }
                }
            });
            List<String> dataList = new ArrayList<>();
            for (String node : nodeList) {
                byte[] bytes = zooKeeper.getData(ConstantClass.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }
            logger.debug("node data: {}", dataList);
            this.dataList = dataList;

            logger.debug("Service discovery triggered updating connected server node.");
            UpdateConnectedServer();
        } catch (KeeperException | InterruptedException e) {
            logger.error("", e);
        }
    }

    private void UpdateConnectedServer(){
        ConnectManage.getInstance().updateConnectedServer(this.dataList);
    }

    public void stop(){
        if(zooKeeper != null){
            try{
                zooKeeper.close();
            } catch (InterruptedException e){
                logger.error(e.toString());
            }
        }
    }

}
