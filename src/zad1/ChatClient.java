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
    private String clientId;
    private SocketChannel channel;
    private StringBuilder chatView;
    private BlockingQueue<String> messageQueue;
    private Thread readThread;
    private volatile boolean running;

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.clientId = id;
        this.chatView = new StringBuilder();
        this.messageQueue = new LinkedBlockingQueue<>();
    }

    public void login() {
        try {
            channel = SocketChannel.open(new InetSocketAddress(host, port));
            channel.configureBlocking(false);
            send("LOGIN " + clientId);

            running = true;
            readThread = new Thread(this::readMessages);
            readThread.start();
        } catch (IOException e) {
            chatView.append("*** ").append(e.toString()).append("\n");
        }
    }

    public void logout() {
        try {
            send("LOGOUT");
            running = false;
            if (channel != null) {
                channel.close();
            }
            if (readThread != null) {
                readThread.interrupt();
            }
        } catch (IOException e) {
            chatView.append("*** ").append(e.toString()).append("\n");
        }
    }

    public void send(String req) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(req.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            chatView.append("*** ").append(e.toString()).append("\n");
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
                        chatView.append(message).append("\n");
                        messageBuilder.delete(0, newlineIndex + 1);
                    }
                }

                Thread.sleep(100);
            } catch (IOException | InterruptedException e) {
                if (running) {
                    chatView.append("*** ").append(e.toString()).append("\n");
                }
                break;
            }
        }
    }

    public String getChatView() {
        return "=== " + clientId + " chat view\n" + chatView.toString();
    }
}