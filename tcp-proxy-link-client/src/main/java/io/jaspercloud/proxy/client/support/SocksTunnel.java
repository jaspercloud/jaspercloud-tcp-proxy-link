package io.jaspercloud.proxy.client.support;

import io.jaspercloud.proxy.client.config.ClientProperties;
import io.jaspercloud.proxy.core.support.LogHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponseDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SocksTunnel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ClientProperties clientProperties;

    public SocksTunnel(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    public ChannelFuture connect(InetSocketAddress address) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientProperties.getConnectTimeout())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new LogHandler("socks5"));
                        pipeline.addLast(Socks5ClientEncoder.DEFAULT);
                        pipeline.addLast(new Socks5InitialResponseDecoder());
                        pipeline.addLast(new Socks5PasswordAuthResponseDecoder());
                        pipeline.addLast(new Socks5CommandResponseDecoder());
                        pipeline.addLast(new ChannelDuplexHandler() {
                            @Override
                            public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
                                ChannelPromise connectPromise = ctx.newPromise();
                                super.connect(ctx, remoteAddress, localAddress, connectPromise);
                                connectPromise.addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) throws Exception {
                                        if (!future.isSuccess()) {
                                            future.channel().close();
                                            promise.setFailure(future.cause());
                                            return;
                                        }
                                        Channel channel = future.channel();
                                        ChannelPipeline pipeline = channel.pipeline();
                                        pipeline.addLast(new Socks5ClientHandler(clientProperties, promise));
                                        Socks5InitialRequest request = new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH);
                                        channel.writeAndFlush(request);
                                    }
                                });
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.connect(address);
        future.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info(String.format("SocksTunnel disconnect"));
                group.shutdownGracefully();
            }
        });
        return future;
    }
}