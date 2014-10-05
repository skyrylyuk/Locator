package com.gunlocator.network;

import android.util.Log;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

/**
 * Created by skyrylyuk on 10/5/14.
 */
public final class QuoteOfTheMomentClient extends Thread {
    public static final String TAG = QuoteOfTheMomentClient.class.getSimpleName();

    static final int PORT = Integer.parseInt(System.getProperty("port", "7686"));
    private final EventLoopGroup group;
    private long time;

    public QuoteOfTheMomentClient(long timeStamp) {
        group = new NioEventLoopGroup();

        time = timeStamp;
    }

    @Override
    public void run() {
        super.run();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Log.e(TAG, "java.lang.InterruptedException ", e);
        }

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new QuoteOfTheMomentClientHandler());

            Channel ch = b.bind(0).sync().channel();

            // Broadcast the request to port 8080.

            ch.writeAndFlush(new DatagramPacket(
                    Unpooled.copyLong(time).writeDouble(3.567),
                    new InetSocketAddress("255.255.255.255", PORT))).sync();

            // QuoteOfTheMomentClientHandler will close the DatagramChannel when a
            // response is received.  If the channel is not closed within 5 seconds,
            // print an error message and quit.
            if (!ch.closeFuture().await(5000)) {
                Log.e(TAG, "Request timed out.");
            }
        } catch (Exception e) {
            Log.e(TAG, "java.lang.Exception ", e);

        } finally {
            group.shutdownGracefully();
        }
    }

    public static class QuoteOfTheMomentClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        public static final String TAG = QuoteOfTheMomentClientHandler.class.getSimpleName();

        @Override
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
            String response = msg.content().toString(CharsetUtil.UTF_8);
            if (response.startsWith("QOTM: ")) {
                System.out.println("Quote of the Moment: " + response.substring(6));
                Log.w(TAG, "Quote of the Moment: " + response.substring(6));
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
