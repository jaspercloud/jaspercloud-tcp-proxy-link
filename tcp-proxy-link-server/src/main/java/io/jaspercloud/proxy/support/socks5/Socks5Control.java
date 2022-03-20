package io.jaspercloud.proxy.support.socks5;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Socks5Control {

    private Map<String, AtomicInteger> ctMap = new ConcurrentHashMap<>();

    public void increment(String hostName) {
        AtomicInteger counter = ctMap.computeIfAbsent(hostName, e -> new AtomicInteger());
        counter.incrementAndGet();
    }

    public int get(String hostName) {
        AtomicInteger counter = ctMap.computeIfAbsent(hostName, e -> new AtomicInteger());
        int result = counter.get();
        return result;
    }

    public void clean(String hostName) {
        ctMap.remove(hostName);
    }

    public void cleanMaxRetry() {
        ctMap.clear();
    }
}
