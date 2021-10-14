package io.jaspercloud.proxy.core.support.agent.client;

import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.jaspercloud.proxy.core.support.LogHandler;
import io.jaspercloud.proxy.core.support.tunnel.DataClient;
import io.jaspercloud.proxy.core.support.tunnel.DecodeTunnelHandler;
import io.jaspercloud.proxy.core.support.tunnel.EncodeTunnelHandler;
import io.jaspercloud.proxy.core.support.tunnel.SendTunnelHeartHandler;
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
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;


public class AgentClientHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${agent.connectTimeout}")
    private int connectTimeout;

    private ScheduledFuture<?> fixedRate;

    private long heartTime;

    public AgentClientHandler(long heartTime) {
        this.heartTime = heartTime;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        fixedRate = ctx.executor().scheduleAtFixedRate(() -> {
            ctx.writeAndFlush(TcpProtos.TcpMessage.newBuilder()
                    .setType(TcpProtos.DataType.Heart)
                    .build());
        }, 0, heartTime, TimeUnit.MILLISECONDS);
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
                logger.info("connectReq: {}", ctx.channel().id().asShortText());
                TcpProtos.ConnectReqData reqData = TcpProtos.ConnectReqData.parseFrom(tcpMessage.getData());
                connectTunnel(ctx.channel(), reqData);
                break;
            }
        }
    }

    private void connectTunnel(Channel agentChannel, TcpProtos.ConnectReqData reqData) throws Exception {
        DataClient tunnelClient = new DataClient(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new LogHandler("tunnel"));
                pipeline.addLast(new ProtobufVarint32FrameDecoder());
                pipeline.addLast(new ProtobufDecoder(TcpProtos.TcpMessage.getDefaultInstance()));
                pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                pipeline.addLast(new ProtobufEncoder());
                pipeline.addLast(new SendTunnelHeartHandler(reqData.getSessionId(), heartTime));
            }
        });
        tunnelClient.connect(new InetSocketAddress(reqData.getTunnelHost(), reqData.getTunnelPort()), connectTimeout)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        Channel tunnelChannel = future.channel();
                        logger.info("tunnelChannel isActive={}", tunnelChannel.isActive());
                        if (!tunnelChannel.isActive()) {
                            tunnelChannel.close();
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
                        connectDest(tunnelChannel, reqData);
                    }
                });
    }

    private void connectDest(Channel tunnelChannel, TcpProtos.ConnectReqData reqData) throws Exception {
        logger.info("connect dest: {}", String.format("%s:%s", reqData.getRemoteHost(), reqData.getRemotePort()));
        DataClient destClient = new DataClient(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new LogHandler("dest"));
            }
        });
        destClient.connect(new InetSocketAddress(reqData.getRemoteHost(), reqData.getRemotePort()), connectTimeout)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        Channel destChannel = future.channel();
                        logger.info("destChannel isActive={}", destChannel.isActive());
                        destChannel.closeFuture().addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                logger.info("dest closed: sessionId={}", reqData.getSessionId());
                            }
                        });
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
                            tunnelChannel.writeAndFlush(tcpMessage);
                            tunnelChannel.close();
                            return;
                        }
                        destChannel.pipeline().addLast(new EncodeTunnelHandler(reqData.getSessionId(), "dest2tunnel", tunnelChannel));
                        tunnelChannel.pipeline().addLast(new DecodeTunnelHandler(reqData.getSessionId(), "tunnel2dest", destChannel));

                        TcpProtos.TcpMessage tcpMessage = TcpProtos.TcpMessage.newBuilder()
                                .setType(TcpProtos.DataType.ConnectResp)
                                .setData(TcpProtos.ConnectRespData.newBuilder()
                                        .setProxyType(reqData.getProxyType())
                                        .setSessionId(reqData.getSessionId())
                                        .setCode(200)
                                        .build().toByteString())
                                .build();
                        tunnelChannel.writeAndFlush(tcpMessage);
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
