package com.sunny.cloudstorage.client;

import com.sunny.cloudstorage.common.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class Client {

    private EventLoopGroup workerGroup;


    private ChannelHandlerContext objectDecoderCtx;
    private ChannelHandlerContext objectEncoderCtx;
    private ChannelHandlerContext clientHandlerCtx;

    public ChannelHandlerContext getObjectDecoderCtx() {
        return objectDecoderCtx;
    }

    public ChannelHandlerContext getObjectEncoderCtx() {
        return objectEncoderCtx;
    }

    public ChannelHandlerContext getClientHandlerCtx() {
        return clientHandlerCtx;
    }

    public void start(MainController mainController) throws InterruptedException {
        String host = "localhost";
        int port = 8189;
        workerGroup = new NioEventLoopGroup();

        try {


            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {

                @Override
                public void initChannel(SocketChannel ch)
                {
                    ch.pipeline().addLast(
                            new ObjectDecoder(Constants.FRAME_SIZE, ClassResolvers.cacheDisabled(null)),
                            new ObjectEncoder(),
                            new ClientHandler(mainController));
                }
            });

            ChannelFuture f = b.connect(host, port).sync();

            objectDecoderCtx = f.channel().pipeline().context(ObjectDecoder.class);
            objectEncoderCtx = f.channel().pipeline().context(ObjectEncoder.class);
            clientHandlerCtx = f.channel().pipeline().context(ClientHandler.class);

            f.channel().closeFuture().sync();

        }
        finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void stop() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

    }

}


