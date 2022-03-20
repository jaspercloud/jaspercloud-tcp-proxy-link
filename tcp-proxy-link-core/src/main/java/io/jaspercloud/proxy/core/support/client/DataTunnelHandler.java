package io.jaspercloud.proxy.core.support.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DataTunnelHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private String name;
    private Channel dest;

    public DataTunnelHandler(String name, Channel dest) {
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
        dest.writeAndFlush(msg);
    }
}
