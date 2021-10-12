package io.jaspercloud.proxy.support.tunnel;

import com.google.gson.Gson;
import io.jaspercloud.proxy.core.dto.ConnectReqData;
import io.jaspercloud.proxy.core.dto.ConnectRespData;
import io.jaspercloud.proxy.core.dto.Data;
import io.jaspercloud.proxy.core.support.tunnel.DataTunnel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@ChannelHandler.Sharable
public class TunnelProcessHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TunnelManager tunnelManager;

    private Gson gson = new Gson();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Data data = gson.fromJson((String) msg, Data.class);
        switch (data.getType()) {
            case Data.Type.ConnectResp: {
                ConnectRespData respData = gson.fromJson((String) msg, ConnectRespData.class);
                processConnectResp(ctx, respData);
                break;
            }
        }
    }

    private void processConnectResp(ChannelHandlerContext ctx, ConnectRespData respData) throws Exception {
        logger.info("processConnectResp: {}", ctx.channel().id().asShortText());
        if (200 != respData.getCode()) {
            Channel proxyClient = tunnelManager.getProxyClient(respData.getSessionId());
            if (null != proxyClient) {
                logger.error("connect error: {}", proxyClient.id().asShortText());
                proxyClient.close();
            }
            return;
        }
        Channel proxyChannel = tunnelManager.getProxyClient(respData.getSessionId());
        switch (respData.getProxyType()) {
            case ConnectReqData.ProxyType.Simple: {
                ChannelPipeline pipeline = ctx.pipeline();
                pipeline.addLast(new DataTunnel(respData.getSessionId(), "tunnel2proxy", proxyChannel));
                proxyChannel.pipeline().remove("init");
                proxyChannel.pipeline().addLast(new DataTunnel(respData.getSessionId(), "proxy2tunnel", ctx.channel()));

                pipeline.remove("init");
                pipeline.remove("decode");
                pipeline.remove("encode");
                proxyChannel.config().setAutoRead(true);
                break;
            }
            case ConnectReqData.ProxyType.Socks5: {
                ChannelPipeline pipeline = ctx.pipeline();
                pipeline.addLast(new DataTunnel(respData.getSessionId(), "tunnel2proxy", proxyChannel));
                proxyChannel.pipeline().addLast(new DataTunnel(respData.getSessionId(), "proxy2tunnel", ctx.channel()));

                pipeline.remove("init");
                pipeline.remove("decode");
                pipeline.remove("encode");

                Socks5AddressType addressType = (Socks5AddressType) proxyChannel.attr(AttributeKey.valueOf("dstAddrType")).get();
                Socks5CommandResponse response = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, addressType);
                proxyChannel.writeAndFlush(response);
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
