package com.rpc.server;

import com.rpc.registry.RegistryService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServer {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;

    private String serverAddress;
    private RegistryService serviceRegistry;

    public RpcServer(String serverAddress){
        this.serverAddress = serverAddress;
    }

    public RpcServer(String serverAddress, RegistryService serviceRegistry){
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }



    public void start() throws Exception{
        if(bossGroup == null && workerGroup == null){
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    .addLast(new RpcHandler(handlerMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);
            ChannelFuture future = bootstrap.bind(host, port).sync();
            logger.info("server started on port {}", port);

            if(serviceRegistry != null){
                serviceRegistry.registerService(serverAddress);
            }
            future.channel().closeFuture().sync();
        }
    }

}
