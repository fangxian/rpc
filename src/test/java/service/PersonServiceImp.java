package service;

public class PersonServiceImp implements PersonService {
    @Override
    public String getPersonInfo(String name) {
        return "hello " + name;
    }
}
