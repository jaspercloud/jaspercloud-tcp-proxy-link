package io.jaspercloud.proxy.core.support.agent.client;

import io.jaspercloud.proxy.core.support.agent.DataDecodeHandler;
import io.jaspercloud.proxy.core.support.agent.DataEncodeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetSocketAddress;

public class AgentClient implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${agent.connectTimeout}")
    private int connectTimeout;

    @Value("${agent.heartTime}")
    private long heartTime;

    private InetSocketAddress address;
    private Channel channel;

    public AgentClient(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            while (true) {
                long start = System.currentTimeMillis();
                try {
                    startAgent(address);
                } catch (Exception e) {
                    logger.error("ConnectException: {}", address.toString());
                }
                long end = System.currentTimeMillis();
                try {
                    long sleep = connectTimeout - (end - start);
                    sleep = sleep > 0 ? sleep : connectTimeout;
                    Thread.sleep(sleep);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                logger.info("reconnect proxy agent");
            }
        }).start();
    }

    private void startAgent(InetSocketAddress address) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new DataDecodeHandler());
                            pipeline.addLast(new DataEncodeHandler());
                            pipeline.addLast(new AgentClientHandler(heartTime));
                        }
                    });
            ChannelFuture future = bootstrap.connect(address);
            channel = future.sync().channel();
            channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    logger.info(String.format("ProxyAgent disconnect"));
                }
            });
            logger.info(String.format("ProxyAgent started"));
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
