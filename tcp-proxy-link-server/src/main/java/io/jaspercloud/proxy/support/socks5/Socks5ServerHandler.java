package io.jaspercloud.proxy.support.socks5;

import io.jaspercloud.proxy.config.ProxyServerProperties;
import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.jaspercloud.proxy.support.agent.AgentManager;
import io.jaspercloud.proxy.support.tunnel.TunnelManager;
import io.jaspercloud.proxy.util.AttributeKeys;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5Message;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;


@ChannelHandler.Sharable
public class Socks5ServerHandler extends SimpleChannelInboundHandler<Socks5Message> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private TunnelManager tunnelManager;

    @Autowired
    private Socks5Control socks5Control;

    @Autowired
    private ProxyServerProperties serverProperties;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5Message msg) throws Exception {
        Channel channel = ctx.channel();
        if (!msg.decoderResult().isSuccess()) {
            channel.close();
            return;
        }
        if (msg instanceof Socks5InitialRequest) {
            if (!SocksVersion.SOCKS5.equals(msg.version())) {
                channel.close();
                return;
            }
            Socks5InitialResponse response = new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD);
            channel.writeAndFlush(response);
        } else if (msg instanceof Socks5PasswordAuthRequest) {
            processAuthRequest(ctx, (Socks5PasswordAuthRequest) msg);
        } else if (msg instanceof Socks5CommandRequest) {
            processCommandRequest(ctx, (Socks5CommandRequest) msg);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void processCommandRequest(ChannelHandlerContext ctx, Socks5CommandRequest msg) {
        String dstHost = msg.dstAddr();
        int dstPort = msg.dstPort();
        Channel proxyChannel = ctx.channel();
        AttributeKeys.dstAddrType(proxyChannel).set(msg.dstAddrType());
        tunnelManager.addProxyChannel(proxyChannel);
        String agentId = AttributeKeys.bindAgentId(ctx.channel()).get();
        Channel agentChannel = agentManager.queryChannel(agentId);
        if (null == agentChannel) {
            logger.error("not found agentChannel bindAgentId={}", agentId);
            return;
        }
        agentChannel.writeAndFlush(TcpProtos.TcpMessage.newBuilder()
                .setType(TcpProtos.DataType.ConnectReq)
                .setData(TcpProtos.ConnectReqData.newBuilder()
                        .setProxyType(TcpProtos.ProxyType.Socks5)
                        .setSessionId(proxyChannel.id().asShortText())
                        .setDestHost(dstHost)
                        .setDestPort(dstPort)
                        .build().toByteString()));
    }

    private void processAuthRequest(ChannelHandlerContext ctx, Socks5PasswordAuthRequest msg) {
        logger.info("socks5 remote: {}", ctx.channel().remoteAddress().toString());
        InetSocketAddress socketAddress = ((InetSocketAddress) ctx.channel().remoteAddress());
        String hostName = socketAddress.getHostName();
        int ct = socks5Control.get(hostName);
        if (ct >= serverProperties.getSocks5MaxRetry()) {
            logger.info("socks5 auth maxRetry fail: {}", ctx.channel().remoteAddress().toString());
            Socks5PasswordAuthResponse response = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            ctx.writeAndFlush(response);
            return;
        }
        Channel channel = agentManager.queryChannel(msg.username(), msg.password());
        if (null == channel) {
            logger.info("socks5 auth fail: {}", ctx.channel().remoteAddress().toString());
            socks5Control.increment(hostName);
            Socks5PasswordAuthResponse response = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            ctx.writeAndFlush(response);
            return;
        }
        AttributeKeys.bindAgentId(ctx.channel()).set(channel.id().asShortText());
        socks5Control.clean(hostName);
        Socks5PasswordAuthResponse response = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
        ctx.writeAndFlush(response);
    }
}
