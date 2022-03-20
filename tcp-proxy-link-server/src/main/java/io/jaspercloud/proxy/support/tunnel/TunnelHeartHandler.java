package io.jaspercloud.proxy.support.tunnel;

import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelHeartHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TcpProtos.TcpMessage tcpMessage = (TcpProtos.TcpMessage) msg;
        switch (tcpMessage.getType().getNumber()) {
            case TcpProtos.DataType.Heart_VALUE: {
                TcpProtos.TunnelHeart tunnelHeart = TcpProtos.TunnelHeart.parseFrom(tcpMessage.getData());
                logger.info("tunnelHeart: sessionId={}", tunnelHeart.getSessionId());
                break;
            }
            default: {
                super.channelRead(ctx, msg);
                break;
            }
        }
    }
}
