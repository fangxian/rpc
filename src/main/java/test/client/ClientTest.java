package test.client;

import com.rpc.client.IAsyncObjectProxy;
import com.rpc.client.ObjectProxy;
import com.rpc.client.RpcClient;
import com.rpc.client.RpcFuture;
import com.rpc.registry.DiscoveryService;
import test.service.HelloService;

import java.util.concurrent.TimeUnit;

public class ClientTest {
    public static void main(String[] args) throws Exception{
        DiscoveryService discoveryService = new DiscoveryService("192.168.0.108", "2181");
        RpcClient rpcClient = new RpcClient(discoveryService);
        IAsyncObjectProxy client = rpcClient.createAsync(HelloService.class);
        String serviceName = HelloService.class.getName();
        RpcFuture rpcFuture = client.call(serviceName, "hello", "world");

        String result = (String)rpcFuture.get(3000, TimeUnit.MILLISECONDS);
        //personFuture.get(3000, TimeUnit.MILLISECONDS);
        System.out.println(result);
    }
}
