package io.jaspercloud.proxy.core.support.agent;

import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;

public class HostAgent implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private AgentProperties agentProperties;
    private Channel channel;

    public HostAgent(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            while (true) {
                long start = System.currentTimeMillis();
                InetSocketAddress address = new InetSocketAddress(agentProperties.getServerHost(), agentProperties.getAgentPort());
                try {
                    startAgent(address);
                } catch (Exception e) {
                    logger.error("ConnectException: {}", address.toString());
                }
                long end = System.currentTimeMillis();
                try {
                    long sleep = agentProperties.getConnectTimeout() - (end - start);
                    sleep = sleep > 0 ? sleep : agentProperties.getConnectTimeout();
                    Thread.sleep(sleep);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                logger.info("reconnect HostAgent");
            }
        }).start();
    }

    private void startAgent(InetSocketAddress address) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, agentProperties.getConnectTimeout())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new ProtobufVarint32FrameDecoder());
                            pipeline.addLast(new ProtobufDecoder(TcpProtos.TcpMessage.getDefaultInstance()));
                            pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                            pipeline.addLast(new ProtobufEncoder());
                            pipeline.addLast(new AgentHandler(agentProperties));
                        }
                    });
            ChannelFuture future = bootstrap.connect(address);
            channel = future.sync().channel();
            channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    logger.info(String.format("HostAgent disconnect"));
                }
            });
            logger.info(String.format("HostAgent started"));
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
