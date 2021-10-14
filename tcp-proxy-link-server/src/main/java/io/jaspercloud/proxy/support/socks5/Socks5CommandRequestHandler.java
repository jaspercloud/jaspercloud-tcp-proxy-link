package io.jaspercloud.proxy.support.socks5;

import io.jaspercloud.proxy.core.proto.TcpProtos;
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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest msg) throws Exception {
        String remoteHost = msg.dstAddr();
        int remotePort = msg.dstPort();
        Channel proxyChannel = ctx.channel();
        proxyChannel.attr(AttributeKey.valueOf("dstAddrType")).set(msg.dstAddrType());
        tunnelManager.addProxyClient(proxyChannel);
        Channel agentChannel = agentManager.randomChannel();
        agentChannel.writeAndFlush(TcpProtos.TcpMessage.newBuilder()
                .setType(TcpProtos.DataType.ConnectReq)
                .setData(TcpProtos.ConnectReqData.newBuilder()
                        .setProxyType(TcpProtos.ProxyType.Socks5)
                        .setSessionId(proxyChannel.id().asShortText())
                        .setRemoteHost(remoteHost)
                        .setRemotePort(remotePort)
                        .setTunnelHost(tunnelHost)
                        .setTunnelPort(tunnelPort).build().toByteString()));
    }
}
