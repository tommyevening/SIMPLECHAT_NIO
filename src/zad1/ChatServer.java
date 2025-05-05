/**
 * @author Wieczorek Tomasz S27161
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private String host;
    private int port;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ExecutorService executor;
    private volatile boolean running;
    private StringBuilder serverLog;
    private Map<SocketChannel, String> clients;
    private SimpleDateFormat timeFormat;

    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.serverLog = new StringBuilder();
        this.clients = new HashMap<>();
        this.timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    }

    public void startServer() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(host, port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            running = true;
            executor = Executors.newSingleThreadExecutor();
            executor.submit(this::serverLoop);

            System.out.println("Server started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void serverLoop() {
        while (running) {
            try {
                if (selector.select() == 0) continue;

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = 0;

        try {
            read = channel.read(buffer);
        } catch (IOException e) {
            key.cancel();
            channel.close();
            return;
        }

        if (read == -1) {
            handleLogout(channel);
            key.cancel();
            channel.close();
            return;
        }

        buffer.flip();
        String message = StandardCharsets.UTF_8.decode(buffer).toString();
        handleMessage(channel, message);
    }

    private void handleMessage(SocketChannel channel, String message) {
        if (message.startsWith("LOGIN ")) {
            String clientId = message.substring(6);
            clients.put(channel, clientId);
            logAndBroadcast(clientId + " logged in");
        } else if (message.equals("LOGOUT")) {
            handleLogout(channel);
        } else {
            String clientId = clients.get(channel);
            if (clientId != null) {
                logAndBroadcast(clientId + ": " + message);
            }
        }
    }

    private void handleLogout(SocketChannel channel) {
        String clientId = clients.get(channel);
        if (clientId != null) {
            logAndBroadcast(clientId + " logged out");
            clients.remove(channel);
        }
    }

    private void logAndBroadcast(String message) {
        String timestamp = timeFormat.format(new Date());
        String logMessage = timestamp + " " + message;
        serverLog.append(logMessage).append("\n");

        ByteBuffer buffer = ByteBuffer.wrap((message + "\n").getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<SocketChannel, String> entry : clients.entrySet()) {
            try {
                buffer.rewind();
                entry.getKey().write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (selector != null) selector.close();
            if (serverChannel != null) serverChannel.close();
            if (executor != null) executor.shutdown();
            System.out.println("\nServer stopped");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getServerLog() {
        return serverLog.toString();
    }
}