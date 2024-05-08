package org.storck.simplex.networking.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;

/**
 * The Peer class represents a peer that listens for incoming connections on a
 * specified port. It uses Netty's NIO event loop group and server bootstrap to
 * handle the incoming connections.
 */
public class SimplexPeer {

    /**
     * The port variable represents the port number on which the Peer listens for
     * incoming connections.
     */
    private final int port;

    /**
     * Initializes a new instance of the Peer class with the specified port number.
     * The Peer class represents a peer that listens for incoming connections on a
     * specified port.
     *
     * @param port the port number on which the Peer listens for incoming
     *     connections
     */
    public SimplexPeer(final int port) {
        this.port = port;
    }

    /**
     * This method starts the Peer server and listens for incoming connections on
     * the specified port.
     *
     * @throws InterruptedException if the operation is interrupted
     */
    @SuppressWarnings("PMD.DoNotUseThreads")
    public void run() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(@NotNull final SocketChannel channel) {
                            channel.pipeline().addLast(new PeerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture future = bootstrap.bind(port).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}