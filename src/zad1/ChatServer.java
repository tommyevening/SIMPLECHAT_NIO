/**
 * @author Wieczorek Tomasz S27161
 */

package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChatServer extends Thread {

    private String host;
    private int port;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private volatile boolean running;


    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;

    }

    public void startServer() {
        running = true;

        this.start();
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            Selector selector = Selector.open()
        ){
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(host, port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (running){
                if (selector.select() == 0){
                    continue;
                }

                for (SelectionKey key : selector.selectedKeys()){
                    try {
                        if (!key.isValid()) continue;

                        if (key.isAcceptable()) {
                            if (key.channel() instanceof ServerSocketChannel) {
                                SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                                socketChannel.configureBlocking(false);
                                socketChannel.register(selector, SelectionKey.OP_READ);
                            }
                        }

                        if (key.isReadable()) {
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int read = socketChannel.read(buffer);
                            if (read == -1) {
                                socketChannel.close();
                                key.cancel();
                                continue;
                            }
                            buffer.flip();
                            String request = new String(buffer.array(), UTF_8);
                            String response = handleRequest(socketChannel, request);
                            buffer.clear();
                            buffer.put(response.getBytes(UTF_8));
                            buffer.flip();
                            socketChannel.write(buffer);
                        }
                    }catch (IOException e) {
                        key.cancel(); //cancels key in selector, informs that we don't want to monitor that channel anymore
                        try {
                            key.channel().close(); //closes channel associated with key and disposes resources
                        } catch (IOException ignored) {}
                    }

                }
                selector.selectedKeys().clear();
            }

            } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            closeServer();
        }
    }

    private void closeServer() {

        try{
            // Close all channels before closing the selector
            if (selector != null) {
                selector.selectedKeys().forEach(key -> {
                    try {
                        key.channel().close();
                    } catch (IOException ignored) {}
                });
                selector.close();
            }

            if (serverSocketChannel != null) {
                serverSocketChannel.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
        if (selector != null) {
            selector.wakeup();
        }
        try {
            this.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String handleRequest(SocketChannel channel, String request) {


        return request;
    }

    public String getServerLog() {
        return "";
    }
}
