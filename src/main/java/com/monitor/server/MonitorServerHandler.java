package com.monitor.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.util.MonitorConstant;
import com.rpc.kafka.InterfaceStatisticsConsumer;
import com.rpc.kafka.KafkaTopicConstant;
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
        if(receiveMsg.equals(KafkaTopicConstant.STATISTIC_INTERFACE)) {
            Map<String, Integer> countMap = InterfaceStatisticsConsumer.getInstance().getInterfaceCount();
            String response = objectMapper.writeValueAsString(countMap) + "\r\n";
            ctx.writeAndFlush(response);
        }
        else {
            logger.error("monitor server receive error message: {} ", receiveMsg);
            String message = MonitorConstant.COMMADN_ERROR + "\r\n";
            ctx.writeAndFlush(message);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        logger.error("server caught exception",cause);
        ctx.close();
    }

}
