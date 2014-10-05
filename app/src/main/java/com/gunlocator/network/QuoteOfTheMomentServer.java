package com.gunlocator.network;

/**
 *
 * Created by skyrylyuk on 10/5/14.
 */

import android.os.Handler;
import android.util.Log;

import com.gunlocator.MainActivity;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * A UDP server that responds to the QOTM (quote of the moment) request to a {@link QuoteOfTheMomentClient}.
 * <p>
 * Inspired by <a href="http://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html">the official
 * Java tutorial</a>.
 */
public final class QuoteOfTheMomentServer extends Thread {

    public static final String TAG = QuoteOfTheMomentServer.class.getSimpleName();

    private static final int PORT = Integer.parseInt(System.getProperty("port", "7686"));

    private static QuoteOfTheMomentServer instance;
    private Handler handler;


    public static QuoteOfTheMomentServer getInstance(Handler handler) {
        if (instance == null) {
            instance = new QuoteOfTheMomentServer();
            instance.start();
        }
        instance.handler = handler;
        return instance;
    }

    @Override
    public void run() {
        super.run();

        Log.w(TAG, "QuoteOfTheMomentServer.run");
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new QuoteOfTheMomentServerHandler());

            b.bind(PORT).sync().channel().closeFuture().await();
        } catch (InterruptedException x) {
            Log.e(TAG, x.getMessage());
        } finally {
            group.shutdownGracefully();
        }
    }

    public class QuoteOfTheMomentServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {

            handler.sendMessage(handler.obtainMessage(MainActivity.REMOTE_MESSAGE, packet.sender().toString()));

            System.err.println(packet);
            ByteBuf content = packet.content();
            Log.w(TAG, "========================================================================");
            Log.w(TAG, "content.readLong() = " + content.readLong());
            Log.w(TAG, "========================================================================");
            Log.w(TAG, "content.readDouble() = " + content.readDouble());
            Log.w(TAG, "========================================================================");
/*
            if ("QOTM?".equals(packet.content().toString(CharsetUtil.UTF_8))) {
                ctx.write(new DatagramPacket(
                        Unpooled.copiedBuffer("QOTM: " + nextQuote(), CharsetUtil.UTF_8), packet.sender()));
            }
*/
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            // We don't close the channel because we can keep serving requests.
        }
    }
}