package io.jaspercloud.proxy.support.agent.server;

import com.google.gson.Gson;
import io.jaspercloud.proxy.core.dto.ConnectRespData;
import io.jaspercloud.proxy.core.dto.Data;
import io.jaspercloud.proxy.support.tunnel.TunnelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@ChannelHandler.Sharable
public class AgentProcessHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private TunnelManager tunnelManager;

    private Gson gson = new Gson();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        agentManager.addChannel(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Data data = gson.fromJson((String) msg, Data.class);
        logger.info("channelRead type: {}", data.getType());
        switch (data.getType()) {
            case Data.Type.Heart: {
                logger.info("updateHeart: {}", ctx.channel().id().asShortText());
                agentManager.updateHeart(ctx.channel().id().asShortText());
                break;
            }
            case Data.Type.ConnectResp: {
                logger.info("connectResp: {}", ctx.channel().id().asShortText());
                ConnectRespData respData = gson.fromJson((String) msg, ConnectRespData.class);
                Channel proxyClient = tunnelManager.getProxyClient(respData.getSessionId());
                if (null != proxyClient) {
                    proxyClient.close();
                }
                break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            logger.error(cause.getMessage());
        } else {
            logger.error("exceptionCaught: " + cause.getMessage(), cause);
        }
        ctx.channel().close();
    }
}
