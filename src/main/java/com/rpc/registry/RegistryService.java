package com.rpc.registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class RegistryService {
    private String zookeeperServer;
    private String zookeeperPort;
    private String connectionZK;

    private CountDownLatch latch = new CountDownLatch(1);
    private ZooKeeper zooKeeper;

    private static final Logger logger = LoggerFactory.getLogger(RegistryService.class);

    public RegistryService(String server, String port) {
        this.zookeeperPort = port;
        this.zookeeperServer = server;
        connectionZK = server+":" + port;


    }

    public void connectZkServer() throws IOException {
        try {
            zooKeeper = new ZooKeeper(connectionZK, ConstantClass.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    logger.info("event is triggered -- {" + watchedEvent.getState() + "}");
                    if(watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            //wait until connecting to zookeeper
            latch.await();

        } catch (IOException e)
        {
            logger.error("", e);
        } catch (InterruptedException e) {
            logger.error(e.toString());
        }

        logger.info("zookeeper connection success!");
    }

    public void registerService(String data) throws IOException {
        if(data != null) {
            connectZkServer();
            if(this.zooKeeper != null){
                addRootNode();
                createNode(data);
            }
        }
        logger.debug("service "+data);
    }

    public void addRootNode() {
        try{
            Stat s = zooKeeper.exists(ConstantClass.ZK_REGISTRY_PATH, false);
            if(s == null){
                zooKeeper.create(ConstantClass.ZK_REGISTRY_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                logger.info("zookeeper registry path creates success.");
            }
        } catch (KeeperException e){
            logger.error(e.toString());
        } catch (InterruptedException e){
            logger.error(e.toString());
        }
    }


    public void createNode(String data)
    {
        try{
            byte[] bytes = data.getBytes();
            String path = zooKeeper.create(ConstantClass.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("create zookeeper node ({} ==> {})", path , data);
        } catch (KeeperException e){
            logger.error(e.toString());
        } catch (InterruptedException e){
            logger.error(e.toString());
        }
    }



}
