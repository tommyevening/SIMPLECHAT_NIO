/**
 *
 *  @author Wieczorek Tomasz S27161
 *
 */

package zad1;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ChatClientTask implements Runnable {
    private ChatClient client;
    private List<String> messages;
    private int wait;

    private ChatClientTask(ChatClient client, List<String> messages, int wait) {
        this.client = client;
        this.messages = messages;
        this.wait = wait;
    }

    public static ChatClientTask create(ChatClient client, List<String> messages, int wait) {
        return new ChatClientTask(client, messages, wait);
    }

    @Override
    public void run() {
        try {
            client.login();
            if (wait != 0) TimeUnit.MILLISECONDS.sleep(wait);

            for (String message : messages) {
                client.send(message);
                if (wait != 0) TimeUnit.MILLISECONDS.sleep(wait);
            }

            client.logout();
            if (wait != 0) TimeUnit.MILLISECONDS.sleep(wait);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public ChatClient getClient() {
        return client;
    }

    public void get() throws InterruptedException, ExecutionException {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }
}