package io.jaspercloud.proxy.core.support.agent.client;

import com.google.gson.Gson;
import io.jaspercloud.proxy.core.dto.ConnectReqData;
import io.jaspercloud.proxy.core.dto.ConnectRespData;
import io.jaspercloud.proxy.core.dto.Data;
import io.jaspercloud.proxy.core.dto.HeartData;
import io.jaspercloud.proxy.core.support.LogHandler;
import io.jaspercloud.proxy.core.support.agent.DataEncodeHandler;
import io.jaspercloud.proxy.core.support.tunnel.DataClient;
import io.jaspercloud.proxy.core.support.tunnel.DataTunnel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
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

    private Gson gson = new Gson();

    private long heartTime;

    public AgentClientHandler(long heartTime) {
        this.heartTime = heartTime;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        fixedRate = ctx.executor().scheduleAtFixedRate(() -> {
            ctx.writeAndFlush(gson.toJson(new HeartData()));
        }, 0, heartTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        fixedRate.cancel(true);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Data data = gson.fromJson((String) msg, Data.class);
        logger.info("channelRead type: {}", data.getType());
        switch (data.getType()) {
            case Data.Type.ConnectReq: {
                logger.info("connectReq: {}", ctx.channel().id().asShortText());
                ConnectReqData reqData = gson.fromJson((String) msg, ConnectReqData.class);
                connectTunnel(ctx.channel(), reqData);
                break;
            }
        }
    }

    private void connectTunnel(Channel agentChannel, ConnectReqData reqData) throws Exception {
        DataClient tunnelClient = new DataClient(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new LogHandler("tunnel"));
                pipeline.addLast("encode", new DataEncodeHandler());
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
                            ConnectRespData respData = new ConnectRespData();
                            respData.setProxyType(reqData.getProxyType());
                            respData.setSessionId(reqData.getSessionId());
                            respData.setCode(404);
                            agentChannel.writeAndFlush(gson.toJson(respData));
                            return;
                        }
                        connectDest(tunnelChannel, reqData);
                    }
                });
    }

    private void connectDest(Channel tunnelChannel, ConnectReqData reqData) throws Exception {
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
                            ConnectRespData respData = new ConnectRespData();
                            respData.setProxyType(reqData.getProxyType());
                            respData.setSessionId(reqData.getSessionId());
                            respData.setCode(404);
                            tunnelChannel.writeAndFlush(gson.toJson(respData));
                            tunnelChannel.close();
                            return;
                        }
                        destChannel.pipeline().addLast(new DataTunnel(reqData.getSessionId(), "dest2tunnel", tunnelChannel));
                        tunnelChannel.pipeline().addLast(new DataTunnel(reqData.getSessionId(), "tunnel2dest", destChannel));
                        ConnectRespData respData = new ConnectRespData();
                        respData.setProxyType(reqData.getProxyType());
                        respData.setSessionId(reqData.getSessionId());
                        respData.setCode(200);
                        tunnelChannel.writeAndFlush(gson.toJson(respData));
                        tunnelChannel.pipeline().remove("encode");
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
