package io.jaspercloud.proxy.support.tunnel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TunnelManager {

    private Map<String, Channel> proxyClientMap = new ConcurrentHashMap<>();
    private Lock lock = new ReentrantLock();

    public void addProxyClient(Channel channel) {
        try {
            lock.lock();
            channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    proxyClientMap.remove(channel.id().asShortText());
                }
            });
            proxyClientMap.put(channel.id().asShortText(), channel);
        } finally {
            lock.unlock();
        }
    }

    public Channel getProxyClient(String id) {
        Channel channel = proxyClientMap.get(id);
        return channel;
    }
}
