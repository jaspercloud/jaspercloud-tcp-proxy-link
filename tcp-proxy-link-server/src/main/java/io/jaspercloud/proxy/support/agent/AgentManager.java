package io.jaspercloud.proxy.support.agent;

import io.jaspercloud.proxy.config.ProxyServerProperties;
import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.jaspercloud.proxy.util.AttributeKeys;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentManager implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    @Autowired
    private ProxyServerProperties serverProperties;

    @Override
    public void afterPropertiesSet() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            logger.info("online agent: {}", channelMap.size());
            try {
                Iterator<Map.Entry<String, Channel>> iterator = channelMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Channel> next = iterator.next();
                    Channel channel = next.getValue();
                    Long lastTime = AttributeKeys.lastHeartTime(channel).get();
                    if ((System.currentTimeMillis() - lastTime) >= serverProperties.getAgentTimeout()) {
                        logger.info("channel timeout: {}", channel.id().asShortText());
                        channel.close();
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }, 0, serverProperties.getAgentCheckTime(), TimeUnit.MILLISECONDS);
    }

    public void addChannel(Channel channel) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                channelMap.remove(channel.id().asShortText());
            }
        });
        AttributeKeys.lastHeartTime(channel).set(System.currentTimeMillis());
        channelMap.put(channel.id().asShortText(), channel);
    }

    public void updateHeart(String id) {
        Channel channel = channelMap.get(id);
        if (null == channel) {
            return;
        }
        AttributeKeys.lastHeartTime(channel).set(System.currentTimeMillis());
    }

    public Channel queryChannel(String shortId) {
        Channel channel = channelMap.get(shortId);
        return channel;
    }

    public Channel queryChannel(String username, String password) {
        for (Channel channel : channelMap.values()) {
            TcpProtos.AgentInfo agentInfo = AttributeKeys.agentInfo(channel).get();
            if (null == agentInfo) {
                continue;
            }
            if (1 == 1
                    && StringUtils.equals(agentInfo.getUsername(), username)
                    && StringUtils.equals(agentInfo.getPassword(), password)) {
                return channel;
            }
        }
        return null;
    }
}
