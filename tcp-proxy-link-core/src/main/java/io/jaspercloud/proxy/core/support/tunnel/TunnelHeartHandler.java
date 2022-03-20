package io.jaspercloud.proxy.core.support.tunnel;

import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.jaspercloud.proxy.core.support.agent.AgentProperties;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TunnelHeartHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private AgentProperties agentProperties;

    private String sessionId;

    private ScheduledFuture<?> scheduledFuture;

    public TunnelHeartHandler(String sessionId, AgentProperties agentProperties) {
        this.sessionId = sessionId;
        this.agentProperties = agentProperties;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        scheduledFuture = ctx.executor().scheduleAtFixedRate(() -> {
            try {
                TcpProtos.TcpMessage tcpMessage = TcpProtos.TcpMessage.newBuilder()
                        .setType(TcpProtos.DataType.Heart)
                        .setData(TcpProtos.TunnelHeart.newBuilder()
                                .setSessionId(sessionId)
                                .build().toByteString())
                        .build();
                logger.info("tunnelHeart: sessionId={}", sessionId);
                ctx.writeAndFlush(tcpMessage);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }, 0, agentProperties.getHeartTime(), TimeUnit.MILLISECONDS);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        scheduledFuture.cancel(true);
        super.channelInactive(ctx);
    }
}
