package com.rpc.server;

import com.rpc.util.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);
    private Map<String, Object> handlerMap;

    public RpcServerHandler(Map<String, Object> map){
        this.handlerMap = map;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) throws Exception{

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){
        System.out.println((String)msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        logger.error("server caught exception",cause);
        ctx.close();
    }
}
