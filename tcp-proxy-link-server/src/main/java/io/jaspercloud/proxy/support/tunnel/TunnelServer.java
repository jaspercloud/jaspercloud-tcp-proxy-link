package io.jaspercloud.proxy.support.tunnel;

import io.jaspercloud.proxy.config.ProxyServerProperties;
import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.jaspercloud.proxy.core.support.LogHandler;
import io.jaspercloud.proxy.core.support.NioEventLoopFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class TunnelServer implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private InitTunnelHandler initTunnelHandler;

    private ProxyServerProperties serverProperties;

    public TunnelServer(ProxyServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                startServer(serverProperties.getTunnelPort());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }).start();
    }

    public void startServer(int port) throws Exception {
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
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new LogHandler("tunnelServer"));
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        pipeline.addLast(new ProtobufDecoder(TcpProtos.TcpMessage.getDefaultInstance()));
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipeline.addLast(new ProtobufEncoder());
                        pipeline.addLast(new TunnelHeartHandler());
                        pipeline.addLast(InitTunnelHandler.InitHandler, initTunnelHandler);
                    }
                });
        Channel channel = serverBootstrap.bind(port).sync().channel();
        logger.info(String.format("TunnelServer started on port(s): %d", port));
        channel.closeFuture().sync();
    }
}
