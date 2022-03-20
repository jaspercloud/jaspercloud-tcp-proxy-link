package io.jaspercloud.proxy.support.agent;

import io.jaspercloud.proxy.core.proto.TcpProtos;
import io.jaspercloud.proxy.support.tunnel.TunnelManager;
import io.jaspercloud.proxy.util.AttributeKeys;
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

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        agentManager.addChannel(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TcpProtos.TcpMessage tcpMessage = (TcpProtos.TcpMessage) msg;
        Channel agentChannel = ctx.channel();
        logger.info("channelRead type: {}", tcpMessage.getType());
        switch (tcpMessage.getType().getNumber()) {
            case TcpProtos.DataType.Heart_VALUE: {
                logger.info("updateHeart: {}", agentChannel.id().asShortText());
                TcpProtos.AgentInfo agentInfo = TcpProtos.AgentInfo.parseFrom(tcpMessage.getData());
                AttributeKeys.agentInfo(agentChannel).set(agentInfo);
                agentManager.updateHeart(agentChannel.id().asShortText());
                break;
            }
            case TcpProtos.DataType.ConnectResp_VALUE: {
                logger.info("connectResp: {}", agentChannel.id().asShortText());
                TcpProtos.ConnectRespData respData = TcpProtos.ConnectRespData.parseFrom(tcpMessage.getData());
                Channel proxyClient = tunnelManager.getProxyChannel(respData.getSessionId());
                if (null != proxyClient) {
                    proxyClient.close();
                }
                break;
            }
            default:
                throw new UnsupportedOperationException();
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
