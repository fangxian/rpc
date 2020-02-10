package com.rpc.kafa;

public class InterfaceStatisticMsg {
    private String interfaceName;
    private int count;

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "InterfaceStatisticMsg{" +
                "interfaceName='" + interfaceName + '\'' +
                ", count=" + count +
                '}';
    }
}
