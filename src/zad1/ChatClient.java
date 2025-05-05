/**
 *
 *  @author Wieczorek Tomasz S27161
 *
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatClient {
    private String host;
    private int port;
    private String id;
    private SocketChannel channel;
    private StringBuilder chatView;
    private Thread readerThread;
    private volatile boolean running;
    private BlockingQueue<String> messageQueue;

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
        this.chatView = new StringBuilder();
        this.messageQueue = new LinkedBlockingQueue<>();
    }

    public void login() {
        try {
            channel = SocketChannel.open(new InetSocketAddress(host, port));
            channel.configureBlocking(false);
            running = true;
            readerThread = new Thread(this::readMessages);
            readerThread.start();
            send("LOGIN|" + id);
        } catch (IOException e) {
            handleError(e);
        }
    }

    public void logout() {
        try {
            if (channel != null && channel.isOpen()) {
                send("LOGOUT|");
                running = false;
                channel.close();
                if (readerThread != null) {
                    readerThread.join();
                }
            }
        } catch (IOException | InterruptedException e) {
            handleError(e);
        }
    }

    public void send(String message) {
        try {
            if (!message.startsWith("LOGIN|") && !message.startsWith("LOGOUT|")) {
                message = "MESSAGE|" + message;
            }
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            handleError(e);
        }
    }

    private void readMessages() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuilder messageBuilder = new StringBuilder();

        while (running) {
            try {
                buffer.clear();
                int read = channel.read(buffer);

                if (read > 0) {
                    buffer.flip();
                    String received = StandardCharsets.UTF_8.decode(buffer).toString();
                    messageBuilder.append(received);

                    int newlineIndex;
                    while ((newlineIndex = messageBuilder.indexOf("\n")) != -1) {
                        String message = messageBuilder.substring(0, newlineIndex);
                        messageQueue.put(message);
                        addToChatView(message);
                        messageBuilder.delete(0, newlineIndex + 1);
                    }
                }

                Thread.sleep(10);
            } catch (IOException | InterruptedException e) {
                if (running) {
                    handleError(e);
                }
                break;
            }
        }
    }

    private synchronized void addToChatView(String message) {
        chatView.append(message).append("\n");
    }

    private void handleError(Exception e) {
        String error = "*** " + e.toString();
        addToChatView(error);
    }

    public String getChatView() {
        return "=== " + id + " chat view\n" + chatView.toString();
    }
}