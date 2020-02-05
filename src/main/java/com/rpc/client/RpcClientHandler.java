package com.rpc.client;

import com.rpc.util.RpcRequest;
import com.rpc.util.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private static Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);
    private volatile Channel channel;
    private SocketAddress remotePeer;
    private ConcurrentHashMap<String, Object> pendingRpc;


    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception{
        String requestId = rpcResponse.getRequestId();
        RpcFuture rpcFuture =(RpcFuture)pendingRpc.get(requestId);
        if (rpcFuture != null) {
            pendingRpc.remove(requestId);
            rpcFuture.done(rpcResponse);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.toString());
    }

    public void close(){
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public SocketAddress getRemotePeer(){
        return remotePeer;
    }

    public Channel getChannel(){
        return channel;
    }
}
