/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package fun.codec.eprofiler.runner.netty;

import com.intellij.openapi.diagnostic.Logger;
import fun.codec.eprofiler.runner.ProfilerExecutor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class HttpStaticFileServer {

    private static final Logger logger = Logger.getInstance(ProfilerExecutor.class);

    private static final int PORT = 23333;

    private static Channel channel = null;

    public static void main(String[] args) {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpStaticFileServerInitializer());

            channel = b.bind(PORT).sync().channel();

            System.err.println("Open your web browser and navigate to "
                    + "http://127.0.0.1:" + PORT + '/');

            channel.closeFuture().sync();
        } catch (Exception e) {
            logger.error("start netty file server error. ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void close() {
        if (channel != null) {
            channel.close();
        }
    }
}
