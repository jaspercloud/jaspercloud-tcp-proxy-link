package io.jaspercloud.proxy.core.support.agent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class DataDecodeHandler extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();
        if (in.readableBytes() < Integer.BYTES) {
            in.resetReaderIndex();
            return;
        }
        int size = in.readInt();
        if (in.readableBytes() < size) {
            in.resetReaderIndex();
            return;
        }
        byte[] bytes = new byte[size];
        in.readBytes(bytes);
        String data = new String(bytes, "utf-8");
        out.add(data);
    }
}
