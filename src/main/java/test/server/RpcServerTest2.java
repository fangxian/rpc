package test.server;

import com.rpc.registry.RegistryService;
import com.rpc.server.RpcServer;
import test.service.HelloService;
import test.service.HelloServiceImp;
import test.service.PersonService;
import test.service.PersonServiceImp;

public class RpcServerTest2 {
    public static void main(String[] args) throws Exception{
        String name = PersonService.class.getName();
        RegistryService registryService = new RegistryService("192.168.0.108","2181");
        RpcServer rpcServer = new RpcServer("127.0.0.1:8898:"+name, registryService);
        //TODO 注册服务
        PersonService personService = new PersonServiceImp();
        rpcServer.addService("test.service.PersonService", personService);
        try {
            rpcServer.start();
        } catch (Exception e){
            System.out.println(e.fillInStackTrace());
        }
    }
}
