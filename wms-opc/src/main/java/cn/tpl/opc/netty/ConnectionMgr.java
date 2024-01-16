package cn.tpl.opc.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/7
 * Netty连接管理器
 */
@Slf4j
@Component("connectionMgr")
public class ConnectionMgr {
//    private static final String CONNECTION_KEY_DIVIDER = "_";

    /**
     * 用于保存已连接的通道对象
     */
    // TODO: 2023/4/6 前期先这样用于测试，后面优化存放
    private static final ConcurrentHashMap<Long, Connection> CONNECTIONS = new ConcurrentHashMap<>();

    private static final EventLoopGroup WORKER = new NioEventLoopGroup();


//    public String targetKey(String ip, Integer port) {
//        return ip + CONNECTION_KEY_DIVIDER + port;
//    }

    public ConcurrentHashMap<Long, Connection> getConnections() {
        return CONNECTIONS;
    }

    public List<Connection> getConnectionsByWorkLine(int workLine) {
        List<Connection> connByWorkLine = new ArrayList<>();
        for (Connection conn : CONNECTIONS.values()) {
            if (conn.getWorkLine() == workLine) connByWorkLine.add(conn);
        }
        return connByWorkLine;
    }

    public boolean connectionExists(Long key) {
        return CONNECTIONS.containsKey(key);
    }

    public Connection getConnection(Long key) {
        return CONNECTIONS.get(key);
    }

    public void saveConnection(Connection connection) {
        CONNECTIONS.put(connection.getId(), connection);
    }

    public void removeConnection(Long id) {
        disconnect(id);
        CONNECTIONS.remove(id);
    }

//    public void removeAllConnections() {
//        disconnectAll();
//        CONNECTIONS.clear();
//    }

    public void disconnect(Long id) {
        Connection connection = getConnection(id);
        if (null == connection) return;
        connection.nowDead();
    }

//    public void disconnectAll() {
//        for (Connection connection : CONNECTIONS.values()) {
//            if (null == connection) continue;
//            connection.nowDead();
//        }
//    }

    public EventLoopGroup getWorker() {
        return WORKER;
    }
}
