package io.jaspercloud.proxy.support.proxy;

import io.jaspercloud.proxy.core.support.LogHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;


public class ProxyServer implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ProxyProcessHandler proxyProcessHandler;
    private boolean enableProxy;
    private int port;

    private Channel channel;

    public ProxyServer(ProxyProcessHandler proxyProcessHandler, int port, boolean enableProxy) {
        this.proxyProcessHandler = proxyProcessHandler;
        this.port = port;
        this.enableProxy = enableProxy;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                startServer(port);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }).start();
    }

    public void startServer(int port) throws Exception {
        NioEventLoopGroup parentGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup childGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(parentGroup, childGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                            new WriteBufferWaterMark(32 * 1024, 64 * 1024))
                    .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new LogHandler("proxy"));
                            if (enableProxy) {
                                HaProxyProtocolHandler.create(pipeline);
                            }
                            pipeline.addLast("init", proxyProcessHandler);
                        }
                    });
            channel = serverBootstrap.bind(port).sync().channel();
            logger.info(String.format("ProxyServer started on port(s): %d, isEnableProxy=%s", port, proxyProcessHandler.isEnableProxy()));
            channel.closeFuture().sync();
        } finally {
            childGroup.shutdownGracefully();
            parentGroup.shutdownGracefully();
        }
    }
}
