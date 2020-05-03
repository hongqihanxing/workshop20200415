
package shardingsphere.workshop.mysql.proxy.todo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import shardingsphere.workshop.mysql.proxy.fixture.MySQLAuthenticationHandler;
import shardingsphere.workshop.mysql.proxy.fixture.packet.MySQLErrPacketFactory;
import shardingsphere.workshop.mysql.proxy.fixture.packet.MySQLPacketPayload;
import shardingsphere.workshop.mysql.proxy.fixture.packet.constant.MySQLColumnType;
import shardingsphere.workshop.mysql.proxy.todo.packet.MySQLEofPacket;
import shardingsphere.workshop.mysql.proxy.todo.packet.MySQLColumnDefinition41Packet;
import shardingsphere.workshop.mysql.proxy.todo.packet.MySQLFieldCountPacket;
import shardingsphere.workshop.mysql.proxy.todo.packet.MySQLTextResultSetRowPacket;
import shardingsphere.workshop.mysql.proxy.todo.vo.User;
import shardingsphere.workshop.parser.engine.ParseEngine;
import shardingsphere.workshop.parser.statement.segment.SelectSegment;

/**
 * Frontend channel inbound handler.
 */
@RequiredArgsConstructor
@Slf4j
public final class FrontendChannelInboundHandler extends ChannelInboundHandlerAdapter {
    
    private final MySQLAuthenticationHandler authHandler = new MySQLAuthenticationHandler();
    
    private boolean authorized;

    private static List<User> data = new ArrayList<>();

    static {
        data.addAll(Arrays.asList(
            User.builder().id(1).name("user1").build(),
            User.builder().id(2).name("user2").build(),
            User.builder().id(3).name("user3").build(),
            User.builder().id(4).name("user4").build(),
            User.builder().id(5).name("user5").build()
        ));
    }

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
        try (MySQLPacketPayload payload = new MySQLPacketPayload((ByteBuf) message)) {
            executeCommand(context, payload);
        } catch (final Exception ex) {
            log.error("Exception occur: ", ex);
            context.writeAndFlush(MySQLErrPacketFactory.newInstance(1, ex));
        }
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
    
    private void executeCommand(final ChannelHandlerContext context, final MySQLPacketPayload payload) {
        Preconditions.checkState(0x03 == payload.readInt1(), "only support COM_QUERY command type");
        //查询sql：select * from t_user where id=1
        String sql = payload.readStringEOF();
        log.info("查询Sql:" + sql);
        SelectSegment selectSegment = (SelectSegment) ParseEngine.parse(sql);
        Integer id = Integer.parseInt(selectSegment.getConditionSegment().getColValueSegment().getIdentifier().getValue());
        Optional<User> user = data.stream().filter(item->item.getId().equals(id)).findFirst();


        context.write(new MySQLFieldCountPacket(1, 2));
        context.write(new MySQLColumnDefinition41Packet(2, 0, "sharding_db", "t_user", "t_user", "id", "id", 100, MySQLColumnType.MYSQL_TYPE_INT24,0));
        context.write(new MySQLColumnDefinition41Packet(3, 0, "sharding_db", "t_user", "t_user", "name", "name", 100, MySQLColumnType.MYSQL_TYPE_STRING,0));
        context.write(new MySQLEofPacket(4));
        if(user.isPresent()){
            context.write(new MySQLTextResultSetRowPacket(5, Arrays.asList(user.get().getId(),user.get().getName())));
            context.write(new MySQLEofPacket(6));
        }else {
            context.write(new MySQLEofPacket(5));
        }
        context.flush();
    }
}
