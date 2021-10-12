package io.jaspercloud.proxy.support.socks5;

import com.google.gson.Gson;
import io.jaspercloud.proxy.core.dto.ConnectReqData;
import io.jaspercloud.proxy.support.agent.server.AgentManager;
import io.jaspercloud.proxy.support.tunnel.TunnelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.AttributeKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@ChannelHandler.Sharable
public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private TunnelManager tunnelManager;

    @Value("${tunnel.remote.host}")
    private String tunnelHost;

    @Value("${tunnel.remote.port}")
    private int tunnelPort;

    private Gson gson = new Gson();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest msg) throws Exception {
        String remoteHost = msg.dstAddr();
        int remotePort = msg.dstPort();
        Channel proxyChannel = ctx.channel();
        proxyChannel.attr(AttributeKey.valueOf("dstAddrType")).set(msg.dstAddrType());
        tunnelManager.addProxyClient(proxyChannel);
        Channel agentChannel = agentManager.randomChannel();
        ConnectReqData connectData = new ConnectReqData();
        connectData.setProxyType(ConnectReqData.ProxyType.Socks5);
        connectData.setSessionId(proxyChannel.id().asShortText());
        connectData.setRemoteHost(remoteHost);
        connectData.setRemotePort(remotePort);
        connectData.setTunnelHost(tunnelHost);
        connectData.setTunnelPort(tunnelPort);
        String data = gson.toJson(connectData);
        agentChannel.writeAndFlush(data);
    }
}
