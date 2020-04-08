/*
 * Copyright (c) 2020 global.jdd.com.
 * All rights reserved.
 */

package com.jdd.global.shardingsphere.workshop.proxy.frontend;

import com.jdd.global.shardingsphere.workshop.proxy.frontend.command.ComQueryCommandExecutor;
import com.jdd.global.shardingsphere.workshop.proxy.frontend.command.CommandExecutor;
import com.jdd.global.shardingsphere.workshop.proxy.frontend.command.MySQLCommandExecutorFactory;
import com.jdd.global.shardingsphere.workshop.proxy.frontend.auth.MySQLAuthenticationHandler;
import com.jdd.global.shardingsphere.workshop.proxy.transport.MySQLPacketPayload;
import com.jdd.global.shardingsphere.workshop.proxy.transport.packet.MySQLPacket;
import com.jdd.global.shardingsphere.workshop.proxy.transport.packet.error.MySQLErrPacketFactory;
import com.jdd.global.shardingsphere.workshop.proxy.transport.packet.generic.MySQLEofPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

/**
 * Frontend channel inbound handler.
 */
@RequiredArgsConstructor
@Slf4j
public final class FrontendChannelInboundHandler extends ChannelInboundHandlerAdapter {
    
    private final MySQLAuthenticationHandler authHandler = new MySQLAuthenticationHandler();
    
    private boolean authorized;
    
    private final ExecutorService executorService;
    
    
    @Override
    public void channelActive(final ChannelHandlerContext context) {
        authHandler.handshake(context);
    }
    
    @Override
    public void channelRead(final ChannelHandlerContext context, final Object message) {
        if (!authorized) {
            authorized = auth(context, (ByteBuf) message);
            return;
        }
        executorService.execute(new CommandExecutorTask(context, message));
    }
    
    @Override
    public void channelInactive(final ChannelHandlerContext context) {
        context.fireChannelInactive();
    }
    
    private boolean auth(final ChannelHandlerContext context, final ByteBuf message) {
        try (MySQLPacketPayload payload = new MySQLPacketPayload(message)) {
            return authHandler.auth(context, payload);
        } catch (final Exception ex) {
            log.error("Exception occur: ", ex);
            context.write(MySQLErrPacketFactory.newInstance(1, ex));
        }
        return false;
    }
    
    @RequiredArgsConstructor
    private final class CommandExecutorTask implements Runnable {
    
        private final ChannelHandlerContext context;
    
        private final Object message;
    
        @Override
        public void run() {
            try (MySQLPacketPayload payload = new MySQLPacketPayload((ByteBuf) message)) {
                executeCommand(context, payload);
            } catch (final Exception ex) {
                log.error("Exception occur: ", ex);
                context.writeAndFlush(MySQLErrPacketFactory.newInstance(1, ex));
            }
        }
    
        private void executeCommand(final ChannelHandlerContext context, final MySQLPacketPayload payload) throws SQLException {
            CommandExecutor commandExecutor = MySQLCommandExecutorFactory.newInstance(payload);
            Collection<MySQLPacket> responsePackets = commandExecutor.execute();
            for (MySQLPacket each : responsePackets) {
                context.writeAndFlush(each);
            }
            if (commandExecutor instanceof ComQueryCommandExecutor && ((ComQueryCommandExecutor) commandExecutor).isQuery()) {
                writeQueryData(context, (ComQueryCommandExecutor) commandExecutor, responsePackets.size());
            }
        }
    
        private void writeQueryData(final ChannelHandlerContext context, final ComQueryCommandExecutor queryCommandExecutor, final int headerPackagesCount) throws SQLException {
            int currentSequenceId = 0;
            while (queryCommandExecutor.next()) {
                context.writeAndFlush(queryCommandExecutor.getQueryData());
                currentSequenceId++;
            }
            context.writeAndFlush(new MySQLEofPacket(++currentSequenceId + headerPackagesCount));
        }
    }
}