package io.jaspercloud.proxy.core.support.client;

import io.jaspercloud.proxy.core.support.LogHandler;
import io.jaspercloud.proxy.core.support.NioEventLoopFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;

public class LocalServer implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ClientProperties clientProperties;

    public LocalServer(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                startServer(clientProperties.getLocalPort());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }).start();
    }

    private void startServer(int port) throws Exception {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(NioEventLoopFactory.BossGroup, NioEventLoopFactory.WorkerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(new LogHandler("localServer"));
                        channel.config().setAutoRead(false);
                        SocksTunnel socksTunnel = new SocksTunnel(clientProperties);
                        InetSocketAddress address = new InetSocketAddress(clientProperties.getServerHost(), clientProperties.getSocks5Port());
                        ChannelFuture future = socksTunnel.connect(address);
                        future.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (!future.isSuccess()) {
                                    future.channel().close();
                                    return;
                                }
                                Channel socksChannel = future.channel();
                                socksChannel.pipeline().addLast(new DataTunnelHandler("socks2local", channel));
                                channel.pipeline().addLast(new DataTunnelHandler("local2socks", socksChannel));
                                channel.config().setAutoRead(true);
                            }
                        });
                    }
                });
        Channel channel = serverBootstrap.bind(port).sync().channel();
        logger.info(String.format("LocalServer started on port(s): %d", port));
        channel.closeFuture().sync();
    }
}
