package com.rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectManage {

    private static final Logger logger = LoggerFactory.getLogger(ConnectManage.class);

    private volatile static ConnectManage connectManage;
    //线程安全
    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers = new CopyOnWriteArrayList<>();
    private Map<InetSocketAddress, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long connectTimeoutMillis = 6000;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean isRuning = true;


    public static ConnectManage getInstance(){
        if(connectManage == null){
            synchronized (ConnectManage.class){
                if(connectManage == null){
                    connectManage = new ConnectManage();
                }
            }
        }
        return connectManage;
    }

    private ConnectManage(){

    }


    public void updateConnectedServer(List<String> allServerAddress){
        if(allServerAddress != null){
            if(allServerAddress.size() > 0){
                HashSet<InetSocketAddress> newAllServerNodesSet = new HashSet<>();
                for(int i = 0; i<allServerAddress.size(); i++){
                    String[] array = allServerAddress.get(i).split(":");
                    if(array.length == 2){
                        String host = array[0];
                        int port = Integer.parseInt(array[1]);
                        InetSocketAddress remotePeer = new InetSocketAddress(host, port);
                        newAllServerNodesSet.add(remotePeer);
                    }else{
                        logger.error("error server node {}", allServerAddress.get(i));
                    }
                }

                //close and delete invalid server node
                for(int i = 0; i<connectedHandlers.size(); i++){
                    RpcClientHandler connectedServerHandler = connectedHandlers.get(i);
                    SocketAddress remotePeer = connectedServerHandler.getRemotePeer();
                    if(!newAllServerNodesSet.contains(remotePeer)){
                        logger.info("remove invalid server node " + remotePeer);
                        RpcClientHandler handler = connectedServerNodes.get(remotePeer);
                        if (handler != null) {
                            handler.close();
                        }
                        connectedServerNodes.remove(remotePeer);
                        connectedHandlers.remove(connectedServerHandler);
                    }
                }

                //add new server node
                for (final InetSocketAddress serverNodeAddress : newAllServerNodesSet) {
                    if (!connectedServerNodes.keySet().contains(serverNodeAddress)) {
                        connectServerNode(serverNodeAddress);
                    }
                }
            } else {
                logger.error("No available server node (all server nodes are down )");
                for(RpcClientHandler connectServerHandler:connectedHandlers){
                    SocketAddress remoteAddr = connectServerHandler.getRemotePeer();
                    RpcClientHandler handler = connectedServerNodes.get(remoteAddr);
                    handler.close();
                    connectedServerNodes.remove(connectServerHandler);
                }
                connectedHandlers.clear();
            }
        }
    }

    private void connectServerNode(final InetSocketAddress remotePeer) {
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new RpcClientInitializer());

                ChannelFuture channelFuture = b.connect(remotePeer);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if(channelFuture.isSuccess()){
                            logger.debug("Successfully connect to remote server, remote peer = " + remotePeer);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            addHandler(handler);
                        }
                    }
                });
            }
        });
    }

    public void addHandler(RpcClientHandler handler){
        connectedHandlers.add(handler);
        InetSocketAddress remoteAddress = (InetSocketAddress)handler.getChannel().remoteAddress();
        connectedServerNodes.put(remoteAddress, handler);
        signalAvailableHandler();
    }

    public void signalAvailableHandler(){
        lock.lock();
        try{
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public RpcClientHandler chooseHandler(){
        int size = connectedHandlers.size();
        while (isRuning && size <= 0) {
            try {
                boolean available = waitingForHandler();
                if (available) {
                    size = connectedHandlers.size();
                }
            } catch (InterruptedException e) {
                logger.error("Waiting for available node is interrupted! ", e);
                throw new RuntimeException("Can't connect any servers!", e);
            }
        }
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return connectedHandlers.get(index);
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            return connected.await(this.connectTimeoutMillis, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }
}
