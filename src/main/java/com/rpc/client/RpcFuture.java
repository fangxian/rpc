package com.rpc.client;

import com.rpc.util.RpcRequest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class RpcFuture implements Future<Object> {
    private RpcRequest rpcRequest;
    private long startTime;
    private Sync sync;

    public RpcFuture(RpcRequest rpcRequest){
        this.sync = new Sync();
        this.rpcRequest = rpcRequest;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {

        return false;
    }

    @Override
    public boolean isCancelled() {
        //TODO
        return false;
    }

    @Override
    public boolean isDone(){
        //Todo
        return false;
    }

    @Override
    public Object get()  throws InterruptedException, ExecutionException {
        //TODO
        return new Object();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException{
        //TODO
        return new Object();
    }

    static class Sync extends AbstractQueuedSynchronizer{
        private static final long serialVersionUID = 1L;

        //future status
        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        public boolean isDone() {
            getState();
            return getState() == done;
        }
    }


}
