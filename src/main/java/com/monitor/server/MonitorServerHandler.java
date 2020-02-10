package com.monitor.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpc.kafa.InterfaceStatisticsConsumer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MonitorServerHandler extends SimpleChannelInboundHandler<Object> {
    private static Logger logger = LoggerFactory.getLogger(MonitorServerHandler.class);
    private ObjectMapper objectMapper;
    public MonitorServerHandler() {
        objectMapper = new ObjectMapper();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        String receiveMsg = (String)msg;
        Map<String, Integer> countMap = InterfaceStatisticsConsumer.getInstance().getInterfaceCount();
        String response = objectMapper.writeValueAsString(countMap) + "\r\n";
        ctx.writeAndFlush(response);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        logger.error("server caught exception",cause);
        ctx.close();
    }

}
