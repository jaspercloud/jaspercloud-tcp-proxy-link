package io.jaspercloud.proxy.core.support.tunnel;

import com.google.protobuf.ByteString;
import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EncodeTunnelHandler extends ChannelInboundHandlerAdapter {

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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        try {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            TcpProtos.TcpMessage tcpMessage = TcpProtos.TcpMessage.newBuilder()
                    .setType(TcpProtos.DataType.TunnelData)
                    .setData(ByteString.copyFrom(bytes))
                    .build();
            dest.writeAndFlush(tcpMessage);
        } finally {
            ReferenceCountUtil.release(buf);
        }
    }
}
