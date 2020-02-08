package com.rpc.client;

public interface IAsyncObjectProxy {
    public RpcFuture call(String serviceName, String funcName, Object... args);
}
