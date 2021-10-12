package io.jaspercloud.proxy.support.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class HaProxyProtocolHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final String HaProxyDelimiterBasedFrameDecoder = "HaProxyDelimiterBasedFrameDecoder";
    private static final String HaProxyProtocolHandler = "HaProxyProtocolHandler";

    public static final AttributeKey SrcAddressKey = AttributeKey.valueOf("srcAddress");

    private boolean parse = true;

    public static void create(ChannelPipeline pipeline) {
        pipeline.addLast(HaProxyDelimiterBasedFrameDecoder, new DelimiterBasedFrameDecoder(1024, Unpooled.copiedBuffer("\r\n".getBytes())));
        pipeline.addLast(HaProxyProtocolHandler, new HaProxyProtocolHandler());
    }

    public static IpAddress getIpAddress(Channel channel) {
        Attribute<IpAddress> srcAddressAttr = channel.attr(SrcAddressKey);
        IpAddress ipAddress = srcAddressAttr.get();
        if (null != ipAddress) {
            return ipAddress;
        }
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        ipAddress = new IpAddress(address.getHostName(), address.getPort());
        return ipAddress;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (!parse) {
                super.channelRead(ctx, msg);
                return;
            }

            ByteBuf byteBuf = (ByteBuf) msg;
            int len = byteBuf.readableBytes();
            byte[] bytes = new byte[len];
            byteBuf.readBytes(bytes);
            String line = new String(bytes);
            if (!line.startsWith("PROXY")) {
                logger.error("haproxy proxy protocol error");
                ctx.channel().close();
                return;
            }
            String[] split = line.split(" ");
            String srcAddress = split[2];
            String srcPort = split[4];
            logger.info("haproxy address: {}:{}", srcAddress, srcPort);
            Attribute<IpAddress> srcAddressAttr = ctx.channel().attr(SrcAddressKey);
            IpAddress ipAddress = new IpAddress(srcAddress, Integer.parseInt(srcPort));
            srcAddressAttr.set(ipAddress);
            parse = false;

            ctx.pipeline().remove(HaProxyDelimiterBasedFrameDecoder);
            ctx.pipeline().remove(HaProxyProtocolHandler);
            ctx.fireUserEventTriggered(new HaproxyEvent(ipAddress));
        } catch (Exception e) {
            logger.error(String.format("haproxy proxy protocol error: %s", e.getMessage()), e);
            ctx.channel().close();
        }
    }

    public static class IpAddress {

        private String address;
        private int port;

        public String getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public IpAddress(String address, int port) {
            this.address = address;
            this.port = port;
        }
    }
}
