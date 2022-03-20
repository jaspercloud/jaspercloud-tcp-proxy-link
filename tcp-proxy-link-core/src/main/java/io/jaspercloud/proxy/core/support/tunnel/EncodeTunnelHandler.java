package io.jaspercloud.proxy.core.support.tunnel;

import com.google.protobuf.ByteString;
import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EncodeTunnelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private String sessionId;
    private String name;
    private Channel dest;

    public EncodeTunnelHandler(String sessionId, String name, Channel dest) {
        this.sessionId = sessionId;
        this.name = name;
        this.dest = dest;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("inactive sessionId={}, name={}", sessionId, name);
        dest.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            logger.error(cause.getMessage());
        } else {
            logger.error("exceptionCaught: " + cause.getMessage(), cause);
        }
        dest.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        TcpProtos.TcpMessage tcpMessage = TcpProtos.TcpMessage.newBuilder()
                .setType(TcpProtos.DataType.TunnelData)
                .setData(ByteString.copyFrom(bytes))
                .build();
        dest.writeAndFlush(tcpMessage);
    }
}
