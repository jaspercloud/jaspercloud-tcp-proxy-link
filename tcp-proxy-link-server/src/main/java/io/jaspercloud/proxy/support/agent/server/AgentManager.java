package io.jaspercloud.proxy.support.agent.server;

import io.jaspercloud.proxy.core.exception.ProcessException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class AgentManager implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();
    private Lock lock = new ReentrantLock();

    @Value("${agent.checkTime}")
    private long checkTime;

    @Value("${agent.timeout}")
    private long timeout;

    @Override
    public void afterPropertiesSet() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                logger.info("scheduleAtFixedRate heart");
                lock.lock();
                logger.info("online agent: {}", channelMap.size());
                Iterator<Map.Entry<String, Channel>> iterator = channelMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Channel> next = iterator.next();
                    Channel channel = next.getValue();
                    long time = (long) channel.attr(AttributeKey.valueOf("heart")).get();
                    if ((System.currentTimeMillis() - time) >= timeout) {
                        logger.info("channel timeout: {}", channel.id().asShortText());
                        iterator.remove();
                        channel.close();
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                lock.unlock();
            }
        }, 0, checkTime, TimeUnit.MILLISECONDS);
    }

    public void addChannel(Channel channel) {
        try {
            lock.lock();
            channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    channelMap.remove(channel.id().asShortText());
                }
            });
            channel.attr(AttributeKey.valueOf("heart"))
                    .set(System.currentTimeMillis());
            channelMap.put(channel.id().asShortText(), channel);
        } finally {
            lock.unlock();
        }
    }

    public void updateHeart(String id) {
        try {
            lock.lock();
            Channel channel = channelMap.get(id);
            if (null == channel) {
                return;
            }
            channel.attr(AttributeKey.valueOf("heart"))
                    .set(System.currentTimeMillis());
        } finally {
            lock.unlock();
        }
    }

    public Channel randomChannel() {
        List<Channel> list = channelMap.values().stream().collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new ProcessException("not found agent channel");
        }
        int rand = RandomUtils.nextInt(list.size());
        Channel channel = list.get(rand);
        return channel;
    }
}
