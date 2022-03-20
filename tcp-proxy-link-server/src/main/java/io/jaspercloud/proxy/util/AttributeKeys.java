package io.jaspercloud.proxy.util;

import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.netty.channel.Channel;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public final class AttributeKeys {

    private AttributeKeys() {

    }

    public static Attribute<TcpProtos.AgentInfo> agentInfo(Channel channel) {
        Attribute<TcpProtos.AgentInfo> attr = channel.attr(AttributeKey.valueOf("agentInfo"));
        return attr;
    }

    public static Attribute<String> bindAgentId(Channel channel) {
        Attribute<String> attr = channel.attr(AttributeKey.valueOf("bindAgentId"));
        return attr;
    }

    public static Attribute<Socks5AddressType> dstAddrType(Channel channel) {
        Attribute<Socks5AddressType> attr = channel.attr(AttributeKey.valueOf("dstAddrType"));
        return attr;
    }

    public static Attribute<Long> lastHeartTime(Channel channel) {
        Attribute<Long> attr = channel.attr(AttributeKey.valueOf("lastHeartTime"));
        return attr;
    }
}
