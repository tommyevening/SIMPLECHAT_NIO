/**
 *
 *  @author Wieczorek Tomasz S27161
 *
 */

package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ChatClient {

    private String host;
    private int port;
    private String id;

    private SocketChannel socketChannel;
    private Selector selector;
    private StringBuilder chatHistory;


    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
        chatHistory = new StringBuilder();
    }

    public void connect() {
        try
         {
             socketChannel = SocketChannel.open();
             selector = Selector.open();

            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(host, port));

            while (socketChannel.finishConnect()) {
                if (selector.select() == 0) continue;

                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isConnectable()) {
                        socketChannel.finishConnect();
                        break;
                    }
                }
                selector.selectedKeys().clear();
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(String req) {
        try {
            ByteBuffer writeBuffer = ByteBuffer.wrap((req + "\n").getBytes(StandardCharsets.UTF_8));

            while (writeBuffer.hasRemaining()) {
                socketChannel.write(writeBuffer);
            }

            ByteBuffer readBuffer = ByteBuffer.allocate(1024);

            while (socketChannel.read(readBuffer) > 0) {
                readBuffer.flip();
                String recvedMessage = StandardCharsets.UTF_8.decode(readBuffer).toString();
                chatHistory.append(recvedMessage);
                readBuffer.clear();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getChatView(){

        return chatHistory.toString().trim();
    }
}
