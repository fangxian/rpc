package com.rpc.client;

public interface IAsyncObjectProxy {
    public RpcFuture call(String funcName, Object... args);
}
