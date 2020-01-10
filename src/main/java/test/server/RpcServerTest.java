package test.server;

import com.rpc.server.RpcServer;

public class RpcServerTest {

    public static void main(String[] args) {
        RpcServer rpcServer = new RpcServer("127.0.0.1:8899", null);
        try {
            rpcServer.start();
        } catch (Exception e){
            System.out.println(e.fillInStackTrace());
        }
    }

}
