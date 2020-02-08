package test.server;

import com.rpc.registry.RegistryService;
import com.rpc.server.RpcServer;
import test.service.HelloService;
import test.service.HelloServiceImp;

public class RpcServerTest {

    public static void main(String[] args) throws Exception{
        String name = HelloService.class.getName();
        RegistryService registryService = new RegistryService("192.168.0.108","2181");
        RpcServer rpcServer = new RpcServer("127.0.0.1:8899:"+name, registryService);
        HelloService helloService = new HelloServiceImp();
        rpcServer.addService("test.service.HelloService", helloService);
        try {
            rpcServer.start();
        } catch (Exception e){
            System.out.println(e.fillInStackTrace());
        }
    }

}
