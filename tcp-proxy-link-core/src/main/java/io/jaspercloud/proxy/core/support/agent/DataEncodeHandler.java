package io.jaspercloud.proxy.core.support.agent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class DataEncodeHandler extends MessageToByteEncoder<String> {

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
        byte[] bytes = msg.getBytes("utf-8");
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
