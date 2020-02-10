package com.rpc.client;

import com.rpc.util.RpcRequest;
import org.omg.CORBA.portable.InvokeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

public class ObjectProxy<T> implements IAsyncObjectProxy, InvocationHandler {
    private static Logger logger = LoggerFactory.getLogger(ObjectProxy.class);
    private Class<T> clazz;

    public ObjectProxy(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public RpcFuture call(String serviceName, String funcName, Object... args){
        RpcClientHandler handler = Connector.getInstance().chooseHandler(this.clazz.getName());
        RpcRequest request = createRequest(this.clazz.getName(), funcName, args);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws  Throwable{
        if(Object.class == method.getDeclaringClass()){
            String name = method.getName();
            if ("equals".equals(name)) {
                return proxy == args[0];
            } else if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            } else if ("toString".equals(name)) {
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler " + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(UUID.randomUUID().toString());
        rpcRequest.setClassName(method.getDeclaringClass().getName());
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParameterTypes(method.getParameterTypes());
        rpcRequest.setParameters(args);

        // Debug
        logger.debug(method.getDeclaringClass().getName());
        logger.debug(method.getName());
        for (int i = 0; i < method.getParameterTypes().length; ++i) {
            logger.debug(method.getParameterTypes()[i].getName());
        }
        for (int i = 0; i < args.length; ++i) {
            logger.debug(args[i].toString());
        }

        RpcClientHandler handler = Connector.getInstance().chooseHandler(this.clazz.getName());
        RpcFuture rpcFuture = handler.sendRequest(rpcRequest);
        return rpcFuture.get();
    }

    private RpcRequest createRequest(String className, String methodName, Object[] args){
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(UUID.randomUUID().toString());
        rpcRequest.setClassName(className);
        rpcRequest.setMethodName(methodName);
        rpcRequest.setParameters(args);
        Class[] parmeterTypes = new Class[args.length];

        logger.debug(className);
        logger.debug(methodName);
        for(int i = 0; i < args.length; i++){
            parmeterTypes[i] = getClassType(args[i]);
            logger.debug(args[i].toString());
            logger.debug(parmeterTypes[i].getName());

        }
        rpcRequest.setParameterTypes(parmeterTypes);
        return rpcRequest;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName) {
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }

        return classType;
    }

}
