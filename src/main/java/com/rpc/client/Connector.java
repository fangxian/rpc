package com.rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//save connect info
public class Connector {
    private static Logger logger = LoggerFactory.getLogger(Connector.class);
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 4,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private Map<String, List<InetSocketAddress>> serviceAddressMap;
    private Map<InetSocketAddress, RpcClientHandler> serviceHandlerMap;
    private List<RpcClientHandler> rpcClientHandlers;
    private long connectTimeoutMillis = 3000;
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private volatile boolean isRunning = true;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private static Connector connector = null;

    public static Connector getInstance(){
        if(connector == null){
            synchronized (ConnectManage.class){
                if(connector == null){
                    connector = new Connector();
                }
            }
        }
        return connector;
    }

    public Connector() {
        serviceAddressMap = new HashMap<>();
        serviceHandlerMap = new HashMap<>();
        rpcClientHandlers = new ArrayList<>();
    }

    public void connectServer(String service, InetSocketAddress remoteAddress){
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new RpcClientInitializer());
                ChannelFuture channelFuture = b.connect(remoteAddress);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if(channelFuture.isSuccess()){
                            logger.debug("Successfully connect to remote server, remote peer = " + remoteAddress);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            addHandler(remoteAddress, handler);
                            addServiceAddress(service, remoteAddress);
                        }
                    }
                });
            }
        });

    }

    public void addHandler(InetSocketAddress remoteAddress, RpcClientHandler rpcClientHandler) {
        rpcClientHandlers.add(rpcClientHandler);
        serviceHandlerMap.put(remoteAddress, rpcClientHandler);
        signalAvailableHandler();

    }

    public void addServiceAddress(String service, InetSocketAddress address) {
        if(serviceAddressMap.containsKey(service)) {
            List<InetSocketAddress> addresses = serviceAddressMap.get(service);
            addresses.add(address);
            serviceAddressMap.put(service, addresses);
        } else {
            List<InetSocketAddress> addresses = new ArrayList<>();
            addresses.add(address);
            serviceAddressMap.put(service, addresses);
        }
    }

    //serverAddress: ip:port:serviceName
    public void updateConnectServer(List<String> allServerAddress) {
        if(allServerAddress != null) {
            if(allServerAddress.size() > 0){
                //分离出地址和服务名
                List<InetSocketAddress> addressList = new ArrayList<>();
                List<String> serviceNames = new ArrayList<>();
                for(int i = 0; i < allServerAddress.size(); i++) {
                    String[] array = allServerAddress.get(i).split(":");
                    if(array.length == 3) {
                        String host = array[0];
                        int port = Integer.parseInt(array[1]);
                        String serviceName = array[2];
                        InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
                        addressList.add(remoteAddress);
                        serviceNames.add(serviceName);
                    } else{
                        logger.error("server node error {}", allServerAddress.get(i));
                    }
                }
                //判断server是否已经连接--若有则从列表中删除
                for(int i = 0; i < rpcClientHandlers.size(); i++) {
                    SocketAddress remotePeer = rpcClientHandlers.get(i).getRemotePeer();
                    if(!addressList.contains(remotePeer)) {
                        logger.info("remove invalid server node " + remotePeer);
                        RpcClientHandler rpcClientHandler = serviceHandlerMap.get(remotePeer);
                        if (rpcClientHandler != null) {
                            rpcClientHandler.close();
                        }
                        //从serviceAddressMap中删除以及失去连接的服务地址
                        List<InetSocketAddress> addresses = new ArrayList<>();
                        String tempServeiceName = null;
                        for(Map.Entry<String, List<InetSocketAddress>> entry : serviceAddressMap.entrySet()) {
                            addresses = entry.getValue();
                            if(addresses.contains(remotePeer)) {
                                addresses.remove(remotePeer);
                                tempServeiceName = entry.getKey();
                            }
                            break;
                        }
                        serviceAddressMap.put(tempServeiceName, addresses);
                        serviceHandlerMap.remove(remotePeer, rpcClientHandler);
                        rpcClientHandlers.remove(rpcClientHandler);
                    }
                }

                for(int i = 0; i < addressList.size(); i++) {
                    if(!serviceHandlerMap.containsKey(addressList.get(i))) {
                        connectServer(serviceNames.get(i), addressList.get(i));
                    }
                }
            } else {
                //清空和关闭所有连接
                logger.error("No available server node (all server nodes are down )");
                for(RpcClientHandler connectServerHandler: rpcClientHandlers){
                    connectServerHandler.close();
                }
                rpcClientHandlers.clear();
                serviceHandlerMap.clear();
                serviceAddressMap.clear();
            }
        }
    }

    //choose service handler base on round robin
    public RpcClientHandler chooseHandler(String service) {
        List<InetSocketAddress> addresses = serviceAddressMap.get(service);
        int size = 0;
        if(addresses != null)
            size = addresses.size();
        while(isRunning & size <= 0) {
            try {
                boolean available = waitForAvailableHandler();
                if (available) {
                    List<InetSocketAddress> temp = serviceAddressMap.get(service);
                    if(temp!= null) {
                        size = temp.size();
                        addresses = temp;
                    }
                }
            } catch (Exception e) {
                logger.error("Waiting for available node is interrupted! ", e);
                throw new RuntimeException("Can't connect any servers!", e);
            }
        }
        int index = (roundRobin.getAndAdd(1) + size) % size;
        InetSocketAddress socketAddress = addresses.get(index);
        return serviceHandlerMap.get(socketAddress);
    }

    public void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean waitForAvailableHandler() throws Exception{
        lock.lock();
        try {
            return connected.await(this.connectTimeoutMillis, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        isRunning = false;
        for (int i = 0; i < rpcClientHandlers.size(); ++i) {
            RpcClientHandler connectedServerHandler = rpcClientHandlers.get(i);
            connectedServerHandler.close();
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

}
