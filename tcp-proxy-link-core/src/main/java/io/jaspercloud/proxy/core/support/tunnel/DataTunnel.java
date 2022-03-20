package io.jaspercloud.proxy.core.support.tunnel;

import io.jaspercloud.proxy.core.support.NioEventLoopFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class DataTunnel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ChannelInitializer<SocketChannel> initializer;

    public DataTunnel(ChannelInitializer<SocketChannel> initializer) {
        this.initializer = initializer;
    }

    public ChannelFuture connect(InetSocketAddress address, int timeout) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NioEventLoopFactory.WorkerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .handler(initializer);
        ChannelFuture future = bootstrap.connect(address);
        future.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info(String.format("DataTunnel disconnect"));
            }
        });
        logger.info(String.format("DataTunnel started"));
        return future;
    }
}
