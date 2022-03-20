package io.jaspercloud.proxy.core.support.agent;

import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.jaspercloud.proxy.core.support.LogHandler;
import io.jaspercloud.proxy.core.support.tunnel.DataTunnel;
import io.jaspercloud.proxy.core.support.tunnel.DecodeTunnelHandler;
import io.jaspercloud.proxy.core.support.tunnel.EncodeTunnelHandler;
import io.jaspercloud.proxy.core.support.tunnel.TunnelHeartHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;


public class AgentHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private AgentProperties agentProperties;
    private ScheduledFuture<?> fixedRate;

    public AgentHandler(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Runnable runnable = () -> {
            try {
                ctx.channel().writeAndFlush(TcpProtos.TcpMessage.newBuilder()
                        .setType(TcpProtos.DataType.Heart)
                        .setData(TcpProtos.AgentInfo.newBuilder()
                                .setUsername(agentProperties.getUsername())
                                .setPassword(agentProperties.getPassword())
                                .build().toByteString())
                        .build());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        };
        runnable.run();
        fixedRate = ctx.executor().scheduleAtFixedRate(runnable, agentProperties.getHeartTime(), agentProperties.getHeartTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        fixedRate.cancel(true);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TcpProtos.TcpMessage tcpMessage = (TcpProtos.TcpMessage) msg;
        logger.info("channelRead type: {}", tcpMessage.getType());
        switch (tcpMessage.getType().getNumber()) {
            case TcpProtos.DataType.ConnectReq_VALUE: {
                logger.info("connectReq: id={}", ctx.channel().id().asShortText());
                TcpProtos.ConnectReqData reqData = TcpProtos.ConnectReqData.parseFrom(tcpMessage.getData());
                connectProxyTunnel(ctx.channel(), reqData);
                break;
            }
        }
    }

    private void connectProxyTunnel(Channel agentChannel, TcpProtos.ConnectReqData reqData) throws Exception {
        logger.info("connect ProxyTunnel: {}", String.format("%s:%s", agentProperties.getServerHost(), agentProperties.getTunnelPort()));
        DataTunnel proxyTunnel = new DataTunnel(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new LogHandler("proxyTunnel"));
                pipeline.addLast(new ProtobufVarint32FrameDecoder());
                pipeline.addLast(new ProtobufDecoder(TcpProtos.TcpMessage.getDefaultInstance()));
                pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                pipeline.addLast(new ProtobufEncoder());
                pipeline.addLast(new TunnelHeartHandler(reqData.getSessionId(), agentProperties));
            }
        });
        ChannelFuture future = proxyTunnel.connect(new InetSocketAddress(agentProperties.getServerHost(), agentProperties.getTunnelPort()), agentProperties.getConnectTimeout());
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel proxyChannel = future.channel();
                logger.info("proxyTunnel isActive={}", proxyChannel.isActive());
                if (!future.isSuccess()) {
                    proxyChannel.close();
                    return;
                }
                if (!proxyChannel.isActive()) {
                    proxyChannel.close();
                    TcpProtos.TcpMessage tcpMessage = TcpProtos.TcpMessage.newBuilder()
                            .setType(TcpProtos.DataType.ConnectResp)
                            .setData(TcpProtos.ConnectRespData.newBuilder()
                                    .setProxyType(reqData.getProxyType())
                                    .setSessionId(reqData.getSessionId())
                                    .setCode(404)
                                    .build().toByteString())
                            .build();
                    agentChannel.writeAndFlush(tcpMessage);
                    return;
                }
                connectDestTunnel(proxyChannel, reqData);
            }
        });
    }

    private void connectDestTunnel(Channel proxyChannel, TcpProtos.ConnectReqData reqData) throws Exception {
        logger.info("connect destTunnel: {}", String.format("%s:%s", reqData.getDestHost(), reqData.getDestPort()));
        DataTunnel destTunnel = new DataTunnel(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new LogHandler("destTunnel"));
            }
        });
        ChannelFuture future = destTunnel.connect(new InetSocketAddress(reqData.getDestHost(), reqData.getDestPort()), agentProperties.getConnectTimeout());
        future.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                logger.info("destTunnel closed dest={}", String.format("%s:%s", reqData.getDestHost(), reqData.getDestPort()));
            }
        });
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel destChannel = future.channel();
                logger.info("destTunnel isActive={}", destChannel.isActive());
                if (!future.isSuccess()) {
                    destChannel.close();
                    return;
                }
                if (!destChannel.isActive()) {
                    destChannel.close();
                    TcpProtos.TcpMessage tcpMessage = TcpProtos.TcpMessage.newBuilder()
                            .setType(TcpProtos.DataType.ConnectResp)
                            .setData(TcpProtos.ConnectRespData.newBuilder()
                                    .setProxyType(reqData.getProxyType())
                                    .setSessionId(reqData.getSessionId())
                                    .setCode(404)
                                    .build().toByteString())
                            .build();
                    proxyChannel.writeAndFlush(tcpMessage);
                    proxyChannel.close();
                    return;
                }
                destChannel.pipeline().addLast(new EncodeTunnelHandler(reqData.getSessionId(), "dest2tunnel", proxyChannel));
                proxyChannel.pipeline().addLast(new DecodeTunnelHandler(reqData.getSessionId(), "tunnel2dest", destChannel));
                TcpProtos.TcpMessage tcpMessage = TcpProtos.TcpMessage.newBuilder()
                        .setType(TcpProtos.DataType.ConnectResp)
                        .setData(TcpProtos.ConnectRespData.newBuilder()
                                .setProxyType(reqData.getProxyType())
                                .setSessionId(reqData.getSessionId())
                                .setCode(200)
                                .build().toByteString())
                        .build();
                proxyChannel.writeAndFlush(tcpMessage);
            }
        });
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
