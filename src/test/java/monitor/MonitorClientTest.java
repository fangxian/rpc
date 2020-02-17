package monitor;

import com.monitor.client.MonitorClient;

public class MonitorClientTest {
    public static void main(String[] args) throws Exception {
        MonitorClient monitorClient = new MonitorClient("192.168.0.100:8999");
        monitorClient.start();
        monitorClient.test();
        monitorClient.join();
    }

}
