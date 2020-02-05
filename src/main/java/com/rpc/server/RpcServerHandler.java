package com.rpc.server;

import com.rpc.util.RpcRequest;
import com.rpc.util.RpcResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
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
        RpcServer.submit(new Runnable() {
            @Override
            public void run() {
                //解析RpcRequest，执行，写回RpcResponse
                logger.info("receove request id {}", request.getRequestId());
                String id = request.getRequestId();
                RpcResponse rpcResponse = new RpcResponse();
                rpcResponse.setRequestId(id);
                try {
                    Object result = handle(request);
                    rpcResponse.setResult(result);
                } catch (Throwable t){
                    rpcResponse.setError(t.toString());
                    logger.error("rpc server handle error: {}", t);
                }
                ctx.writeAndFlush(rpcResponse).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        logger.debug("send rpc response for request : {}", request.getRequestId());
                    }
                });
            }
        });
    }

    //处理远程调用
    public Object handle(RpcRequest rpcRequest) throws Throwable{
        String className = rpcRequest.getClassName();
        Object serviceBean = handlerMap.get(className);

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = rpcRequest.getMethodName();
        Class<?>[] parametersType = rpcRequest.getParameterTypes();
        Object[] parameters = rpcRequest.getParameters();

        logger.debug(serviceClass.getName());
        logger.debug(methodName);
        for (int i = 0; i < parametersType.length; ++i) {
            logger.debug(parametersType[i].getName());
        }
        for (int i = 0; i < parameters.length; ++i) {
            logger.debug(parameters[i].toString());
        }

        FastClass serviceFastClass = FastClass.create(serviceClass);
        int methodIndex = serviceFastClass.getIndex(methodName, parametersType);
        return serviceFastClass.invoke(methodIndex, serviceBean, parameters);
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
