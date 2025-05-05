/**
 *
 *  @author Wieczorek Tomasz S27161
 *
 */

package zad1;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ChatClientTask implements Runnable{

    private ChatClient client;
    private List<String> messages;
    private int wait;

    public ChatClientTask(ChatClient client, List<String> msgs, int wait){
        this.client = client;
        this.messages = msgs;
        this.wait = wait;
    }

    public static ChatClientTask create(ChatClient client, List<String> msgs, int wait) {
        return new ChatClientTask(client, msgs, wait);
    }

    @Override
    public void run() {
        try {
            client.connect();
            messages.forEach(msg -> {
                client.send(msg);
                try {
                    Thread.sleep(wait);
                    
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public void get() throws InterruptedException, ExecutionException {
        try{
            Thread.sleep(wait);
        }catch (InterruptedException e) {
            throw new ExecutionException(e);
        }

    }

    public ChatClient getClient() {
        return client;
    }
}
