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
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer extends Thread {
    private String host;
    private int port;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private volatile boolean running;
    private StringBuilder serverLog;
    private Map<SocketChannel, String> clients;
    private SimpleDateFormat timeFormat;

    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.serverLog = new StringBuilder();
        this.clients = new ConcurrentHashMap<>();
        this.timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    }

    public void startServer() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(host, port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            running = true;
            start();
            System.out.println("Server started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                selector.select();
                if (!running) break;

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        SocketChannel client = serverChannel.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    }

                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        handleRead(client);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleRead(SocketChannel client) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            int bytesRead = client.read(buffer);
            if (bytesRead == -1) {
                removeClient(client);
                return;
            }

            buffer.flip();
            String message = StandardCharsets.UTF_8.decode(buffer).toString();
            String[] parts = message.split("\\|");
            String command = parts[0];
            String content = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "LOGIN":
                    handleLogin(client, content);
                    break;
                case "LOGOUT":
                    handleLogout(client);
                    break;
                case "MESSAGE":
                    broadcastMessage(client, content);
                    break;
            }
        } catch (IOException e) {
            removeClient(client);
        }
    }

    private void handleLogin(SocketChannel client, String clientId) {
        clients.put(client, clientId);
        String logMessage = clientId + " logged in";
        logServerMessage(logMessage);
        broadcastToAll(logMessage, null);
    }

    private void handleLogout(SocketChannel client) {
        removeClient(client);
    }

    private void removeClient(SocketChannel client) {
        String clientId = clients.remove(client);
        if (clientId != null) {
            try {
                client.close();
                String logMessage = clientId + " logged out";
                logServerMessage(logMessage);
                broadcastToAll(logMessage, client);
            } catch (IOException ignored) {}
        }
    }

    private void broadcastMessage(SocketChannel sender, String message) {
        String clientId = clients.get(sender);
        if (clientId != null) {
            String fullMessage = clientId + ": " + message;
            logServerMessage(fullMessage);
            broadcastToAll(fullMessage, null);
        }
    }

    private void broadcastToAll(String message, SocketChannel exclude) {
        ByteBuffer buffer = ByteBuffer.wrap((message + "\n").getBytes(StandardCharsets.UTF_8));

        for (SocketChannel client : new ArrayList<>(clients.keySet())) {
            if (client.equals(exclude)) continue;

            try {
                buffer.rewind();
                client.write(buffer);
            } catch (IOException e) {
                clients.remove(client);
            }
        }
    }

    private void logServerMessage(String message) {
        String timestamp = timeFormat.format(new Date());
        serverLog.append(timestamp).append(" ").append(message).append("\n");
    }

    public void stopServer() {
        running = false;
        try {
            // Zamykamy połączenia klientów
            for (SocketChannel client : new ArrayList<>(clients.keySet())) {
                removeClient(client);
            }

            if (selector != null) {
                selector.wakeup();
                selector.close();
            }
            if (serverChannel != null) {
                serverChannel.close();
            }
            join();
            System.out.println("\nServer stopped");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getServerLog() {
        return serverLog.toString();
    }
}