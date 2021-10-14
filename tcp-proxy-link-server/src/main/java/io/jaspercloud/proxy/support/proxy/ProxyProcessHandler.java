package io.jaspercloud.proxy.support.proxy;

import io.jaspercloud.proxy.core.exception.ProcessException;
import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.jaspercloud.proxy.support.agent.server.AgentManager;
import io.jaspercloud.proxy.support.tunnel.TunnelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

@ChannelHandler.Sharable
public class ProxyProcessHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private TunnelManager tunnelManager;

    @Value("${remote.host}")
    private String remoteHost;

    @Value("${remote.port}")
    private int remotePort;

    @Value("${tunnel.remote.host}")
    private String tunnelHost;

    @Value("${tunnel.remote.port}")
    private int tunnelPort;

    @Value("${proxy.allow.hosts}")
    private String[] allowHosts;

    private boolean enableProxy;

    public boolean isEnableProxy() {
        return enableProxy;
    }

    public ProxyProcessHandler(boolean enableProxy) {
        this.enableProxy = enableProxy;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (enableProxy) {
            return;
        }
        Channel proxyChannel = ctx.channel();
        InetSocketAddress ipAddress = (InetSocketAddress) proxyChannel.remoteAddress();
        checkIpAddress(proxyChannel, ipAddress.getAddress().getHostAddress(), ipAddress.getPort());
        doConnect(proxyChannel);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!enableProxy) {
            return;
        }
        if (evt instanceof HaproxyEvent) {
            HaproxyEvent haproxyEvent = (HaproxyEvent) evt;
            HaProxyProtocolHandler.IpAddress ipAddress = haproxyEvent.getIpAddress();
            checkIpAddress(ctx.channel(), ipAddress.getAddress(), ipAddress.getPort());
            doConnect(ctx.channel());
        }
    }

    private void doConnect(Channel proxyChannel) {
        logger.info("remote address: {}", proxyChannel.remoteAddress().toString());
        tunnelManager.addProxyClient(proxyChannel);
        Channel agentChannel = agentManager.randomChannel();
        agentChannel.writeAndFlush(TcpProtos.TcpMessage.newBuilder()
                .setType(TcpProtos.DataType.ConnectReq)
                .setData(TcpProtos.ConnectReqData.newBuilder()
                        .setProxyType(TcpProtos.ProxyType.Simple)
                        .setSessionId(proxyChannel.id().asShortText())
                        .setRemoteHost(remoteHost)
                        .setRemotePort(remotePort)
                        .setTunnelHost(tunnelHost)
                        .setTunnelPort(tunnelPort).build().toByteString()));
        logger.info("connect dest: {}", String.format("%s:%s", remoteHost, remotePort));
        //wait connect
        proxyChannel.config().setAutoRead(false);
    }

    private void checkIpAddress(Channel proxyChannel, String address, int port) {
        logger.info("checkIpAddress: {}", address);
        List<String> list = Arrays.asList(allowHosts);
        if (!list.isEmpty() && !list.contains(address)) {
            proxyChannel.close();
            throw new ProcessException("unsupport host");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            logger.error(cause.getMessage());
        } else {
            logger.error("exceptionCaught: " + cause.getMessage(), cause);
        }
        ctx.channel().close();
    }

}
