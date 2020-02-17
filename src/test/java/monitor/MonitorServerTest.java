package monitor;

import com.monitor.server.MonitorServer;

public class MonitorServerTest {
    public static void main(String[] args) throws Exception {
        MonitorServer monitorServer = new MonitorServer("192.168.0.100:8999");
        monitorServer.startMonitor();
    }
}
