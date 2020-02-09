package service;

public class HelloServiceImp implements HelloService {
    @Override
    public String hello(String str) {
        System.out.println(str);
        return "hello" + str;
    }

}
