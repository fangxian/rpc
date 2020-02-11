package com.monitor.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class MonitorClientInitializer extends ChannelInitializer<SocketChannel> {
    private MonitorClient client;
    public MonitorClientInitializer(MonitorClient client) {
        this.client = client;
    }

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new LineBasedFrameDecoder(65536));
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new MonitorClientHandler(client));
    }
}
