package io.jaspercloud.proxy.support.tunnel;

import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.jaspercloud.proxy.core.support.tunnel.DecodeTunnelHandler;
import io.jaspercloud.proxy.core.support.tunnel.EncodeTunnelHandler;
import io.jaspercloud.proxy.util.AttributeKeys;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@ChannelHandler.Sharable
public class TunnelProcessHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TunnelManager tunnelManager;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TcpProtos.TcpMessage tcpMessage = (TcpProtos.TcpMessage) msg;
        switch (tcpMessage.getType().getNumber()) {
            case TcpProtos.DataType.ConnectResp_VALUE: {
                TcpProtos.ConnectRespData respData = TcpProtos.ConnectRespData.parseFrom(tcpMessage.getData());
                processConnectResp(ctx, respData);
                break;
            }
        }
    }

    private void processConnectResp(ChannelHandlerContext ctx, TcpProtos.ConnectRespData respData) throws Exception {
        logger.info("processConnectResp: {}", ctx.channel().id().asShortText());
        if (200 != respData.getCode()) {
            Channel tunnelChannel = tunnelManager.getProxyClient(respData.getSessionId());
            if (null != tunnelChannel) {
                logger.error("connect error: {}", tunnelChannel.id().asShortText());
                tunnelChannel.close();
            }
            return;
        }
        Channel tunnelChannel = tunnelManager.getProxyClient(respData.getSessionId());
        switch (respData.getProxyType().getNumber()) {
            case TcpProtos.ProxyType.Socks5_VALUE: {
                ChannelPipeline pipeline = ctx.pipeline();
                pipeline.addLast(new DecodeTunnelHandler(respData.getSessionId(), "tunnel2proxy", tunnelChannel));
                tunnelChannel.pipeline().addLast(new EncodeTunnelHandler(respData.getSessionId(), "proxy2tunnel", ctx.channel()));

                pipeline.remove("init");

                Socks5AddressType dstAddrType = AttributeKeys.dstAddrType(tunnelChannel).get();
                Socks5CommandResponse response = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, dstAddrType);
                tunnelChannel.writeAndFlush(response);
                break;
            }
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
