package org.storck.simplex.networking.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * This is an implementation for handling incoming peer connections. This class
 * provides methods for handling channel activation, channel read, and exception
 * caught events. When a peer connects, the
 * {@link #channelActive(ChannelHandlerContext)} method is called, which prints
 * the remote address of the connecting peer to the console. When a message is
 * received from the peer, the
 * {@link #channelRead(ChannelHandlerContext, Object)} method is called. This
 * method reads the received byte buffer and prints its content to the console.
 * If an exception is raised during the connection, the
 * {@link #exceptionCaught(ChannelHandlerContext, Throwable)} method is called.
 * This method prints the stack trace of the exception and closes the
 * connection.
 */
@Slf4j
@NoArgsConstructor
public class PeerHandler extends ChannelInboundHandlerAdapter {

    /**
     * This method is called when a new peer connection is established. It prints
     * the remote address of the connecting peer to the console.
     *
     * @param ctx the channel handler context
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        log.info("New peer connected: {}", ctx.channel().remoteAddress());
    }

    /**
     * This method is called when a message is received from the peer.
     *
     * @param ctx the channel handler context
     * @param msg the received message, which is expected to be a ByteBuf
     */
    @Override
    public void channelRead(@NotNull final ChannelHandlerContext ctx, @NotNull final Object msg) {
        ByteBuf incomingBuffer = (ByteBuf) msg;
        StringBuilder messageBuilder = new StringBuilder();
        try {
            while (incomingBuffer.isReadable()) {
                messageBuilder.append((char) incomingBuffer.readByte());
            }
            log.info("Received message: {}", messageBuilder);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * This method is called when an exception is raised during the connection. It
     * logs the exception and closes the connection.
     *
     * @param ctx the channel handler context
     * @param cause the exception that was raised
     */
    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        // Close the connection when an exception is raised.
        log.error("Error caught during connection", cause);
        ctx.close();
    }
}