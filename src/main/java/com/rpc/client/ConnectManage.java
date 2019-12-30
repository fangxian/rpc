package com.rpc.client;

import java.util.List;

public class ConnectManage {
    private volatile static ConnectManage connectManage;
    public static ConnectManage getInstance(){
        if(connectManage == null){
            synchronized (ConnectManage.class){
                if(connectManage == null){
                    connectManage = new ConnectManage();
                }
            }
        }
        return connectManage;
    }

    private ConnectManage(){

    }


    public void updateConnectedServer(List<String> dataList){

    }


}
