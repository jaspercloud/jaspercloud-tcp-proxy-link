package io.jaspercloud.proxy.core.support.client;

import io.jaspercloud.proxy.core.exception.ProcessException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5Message;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;

public class Socks5ClientHandler extends SimpleChannelInboundHandler<Socks5Message> {

    private ClientProperties clientProperties;
    private ChannelPromise promise;

    public Socks5ClientHandler(ClientProperties clientProperties, ChannelPromise promise) {
        this.clientProperties = clientProperties;
        this.promise = promise;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5Message msg) throws Exception {
        Channel channel = ctx.channel();
        if (!msg.decoderResult().isSuccess()) {
            channel.close();
            return;
        }
        if (msg instanceof Socks5InitialResponse) {
            Socks5PasswordAuthRequest authRequest = new DefaultSocks5PasswordAuthRequest(
                    clientProperties.getUsername(),
                    clientProperties.getPassword()
            );
            channel.writeAndFlush(authRequest);
        } else if (msg instanceof Socks5PasswordAuthResponse) {
            Socks5PasswordAuthResponse response = (Socks5PasswordAuthResponse) msg;
            if (!Socks5PasswordAuthStatus.SUCCESS.equals(response.status())) {
                channel.close();
                return;
            }
            Socks5CommandRequest request = new DefaultSocks5CommandRequest(
                    Socks5CommandType.CONNECT,
                    Socks5AddressType.IPv4,
                    clientProperties.getDstHost(),
                    clientProperties.getDstPort()
            );
            channel.writeAndFlush(request);
        } else if (msg instanceof Socks5CommandResponse) {
            Socks5CommandResponse response = (Socks5CommandResponse) msg;
            if (!Socks5CommandStatus.SUCCESS.equals(response.status())) {
                channel.close();
                promise.setFailure(new ProcessException("connection failed"));
                return;
            }
            promise.setSuccess();
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
