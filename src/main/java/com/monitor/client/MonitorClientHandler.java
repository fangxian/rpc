package com.monitor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.util.MonitorConstant;
import com.rpc.kafka.KafkaTopicConstant;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

//TODO
public class MonitorClientHandler extends SimpleChannelInboundHandler<Object> {
    private static Logger logger = LoggerFactory.getLogger(MonitorClientHandler.class);
    private ObjectMapper mapper = new ObjectMapper();
    private MonitorClient monitorClient;

    public MonitorClientHandler(MonitorClient client) {
        monitorClient = client;
    }

    public String constructCommandStr() {
        String requestStr = KafkaTopicConstant.STATISTIC_INTERFACE + "/r/n";
        return requestStr;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    ctx.writeAndFlush(constructCommandStr());
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        logger.error(e.toString());
                    }
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause.toString());
        ctx.close();
    }



    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        String receiveMsg = (String)msg;
        if(receiveMsg.equalsIgnoreCase(MonitorConstant.COMMADN_ERROR)) {
            logger.error("send command error");
        } else {
            try {
                Map<String, Integer> receiveMap = mapper.readValue(receiveMsg, Map.class);
                monitorClient.updateCount(receiveMap);
            } catch (Exception e) {
                logger.error(e.toString());
            }

        }
    }
}
