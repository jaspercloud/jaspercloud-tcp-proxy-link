package io.jaspercloud.proxy.support.socks5;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;

public class Socks5InitialRequestHandler extends SimpleChannelInboundHandler<Socks5InitialRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5InitialRequest msg) throws Exception {
        if (!msg.decoderResult().isSuccess()) {
            ctx.channel().close();
            return;
        }
        if (!SocksVersion.SOCKS5.equals(msg.version())) {
            ctx.channel().close();
            return;
        }
        Socks5InitialResponse response = new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD);
        ctx.writeAndFlush(response);
    }
}
