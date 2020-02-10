package com.rpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.rpc.client.ObjectProxy;
import com.rpc.kafa.InterfaceStatisticsProducer;
import com.rpc.kafa.KafkaConstant;
import com.rpc.util.RpcRequest;
import com.rpc.util.RpcResponse;
import io.netty.channel.*;
import net.sf.cglib.reflect.FastClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);
    private Map<String, Object> handlerMap;
    private Map<String, Integer> interfaceCount = new HashMap<>();
    private String serverAddress;
    private InterfaceStatisticsProducer producer;
    private ObjectMapper mapper = new ObjectMapper(); //转换器

    public RpcServerHandler(Map<String, Object> map, String serverAddress){
        this.handlerMap = map;
        this.serverAddress = serverAddress;
        initInterfaceCount();
        producer = InterfaceStatisticsProducer.getInstance();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) throws Exception{
        RpcServer.submit(new Runnable() {
            @Override
            public void run() {
                logger.info("local address " + ctx.channel().localAddress().toString());
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
        String address = serverAddress+ "/" + rpcRequest.getClassName() + "/"+rpcRequest.getMethodName();
        if(interfaceCount.containsKey(address)) {
            int count = interfaceCount.get(address);
            count++;
            interfaceCount.put(address, count);
        } else {
            interfaceCount.put(address, 1);
        }
        //TODO 修改成定时上报接口调用统计
        reportInterfaceCount();

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

        //invoke的实现会动态生成
        return serviceFastClass.invoke(methodIndex, serviceBean, parameters);

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        logger.error("server caught exception",cause);
        ctx.close();
    }

    public void initInterfaceCount() {
        for(Map.Entry<String, Object> entry : handlerMap.entrySet()){
            for(Method method : entry.getValue().getClass().getDeclaredMethods()){
                String address = serverAddress + "/" +entry.getKey() + "/" + method.getName();
                interfaceCount.put(address, 0);
            }
        }
    }

    public void reportInterfaceCount() throws Exception{
        String msg = mapper.writeValueAsString(interfaceCount);
        producer.produce(KafkaConstant.STATISTIC_INTERFACE, msg);
        return ;
    }
}
