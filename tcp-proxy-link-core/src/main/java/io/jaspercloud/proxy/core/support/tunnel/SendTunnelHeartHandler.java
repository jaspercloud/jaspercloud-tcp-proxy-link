package io.jaspercloud.proxy.core.support.tunnel;

import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SendTunnelHeartHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String sessionId;
    private long heartTime;

    private ScheduledFuture<?> scheduledFuture;

    public SendTunnelHeartHandler(String sessionId, long heartTime) {
        this.sessionId = sessionId;
        this.heartTime = heartTime;
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
                ctx.writeAndFlush(tcpMessage);
                logger.info("send tunnel heart: sessionId={}", sessionId);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }, 0, heartTime, TimeUnit.MILLISECONDS);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        scheduledFuture.cancel(true);
        super.channelInactive(ctx);
    }
}
