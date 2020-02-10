package com.monitor.server;

import com.rpc.kafa.InterfaceStatisticsConsumer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorServer {
    private static Logger logger = LoggerFactory.getLogger(MonitorServer.class);
    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;
    private InterfaceStatisticsConsumer consumer;
    private String host;
    private int port;

    public MonitorServer(String serverAddress){
        consumer = InterfaceStatisticsConsumer.getInstance();
        consumer.start();
        host = serverAddress.split(":")[0];
        port = Integer.parseInt(serverAddress.split(":")[1]);
    }

    //用/r/n来表示数据的结尾
    //返回的时候也加上换行符
    public void startMonitor() throws Exception{
        if(bossGroup == null && workerGroup == null) {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new LineBasedFrameDecoder(65536)).addLast(new StringDecoder())
                                    .addLast(new MonitorServerHandler());

                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(host, port).sync();
            logger.info("monitor server started on port {}", port);
            future.channel().closeFuture().sync();
        }
    }

}
