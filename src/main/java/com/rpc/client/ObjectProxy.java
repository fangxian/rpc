package com.rpc.client;

import com.rpc.util.RpcRequest;
import org.omg.CORBA.portable.InvokeHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ObjectProxy implements IAsyncObjectProxy, InvocationHandler {

    @Override
    public RpcFuture call(String funcName, Object... args){
        //TODO
        
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws  Throwable{
        //TODO

        return new Object();
    }

    private RpcRequest createRequest(String className, String methodName, Object[] args){
        //TODO
        return new RpcRequest();
    }

}
