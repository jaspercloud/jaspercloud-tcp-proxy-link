package io.jaspercloud.proxy.support.socks5;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ChannelHandler.Sharable
public class Socks5PasswordAuthRequestHandler extends SimpleChannelInboundHandler<Socks5PasswordAuthRequest> implements Socks5Control {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${socks5.username}")
    private String username;

    @Value("${socks5.password}")
    private String password;

    @Value("${socks5.maxRetry}")
    private int maxRetry;

    private Map<String, AtomicInteger> ctMap = new ConcurrentHashMap<>();

    @Override
    public void cleanMaxRetry() {
        ctMap.clear();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5PasswordAuthRequest msg) throws Exception {
        logger.info("socks5 remote: {}", ctx.channel().remoteAddress().toString());
        InetSocketAddress socketAddress = ((InetSocketAddress) ctx.channel().remoteAddress());
        String hostName = socketAddress.getHostName();
        AtomicInteger counter = ctMap.get(hostName);
        if (null == counter) {
            counter = new AtomicInteger();
            ctMap.put(hostName, counter);
        }
        if (counter.get() >= maxRetry) {
            logger.info("socks5 auth maxRetry fail: {}", ctx.channel().remoteAddress().toString());
            Socks5PasswordAuthResponse response = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            ctx.writeAndFlush(response);
            return;
        }
        if (!(1 == 1
                && StringUtils.equals(username, msg.username())
                && StringUtils.equals(password, msg.password()))) {
            logger.info("socks5 auth fail: {}", ctx.channel().remoteAddress().toString());
            counter.incrementAndGet();
            Socks5PasswordAuthResponse response = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            ctx.writeAndFlush(response);
            return;
        }
        counter.set(0);
        Socks5PasswordAuthResponse response = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
        ctx.writeAndFlush(response);
    }
}
