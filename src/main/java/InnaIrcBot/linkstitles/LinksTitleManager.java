package InnaIrcBot.linkstitles;

import InnaIrcBot.GlobalData;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class LinksTitleManager {
    private static final BlockingQueue<LinksTitleRequest> queue = new ArrayBlockingQueue<>(GlobalData.CHANNEL_QUEUE_CAPACITY);
    private static final Thread thread = new Thread(new LinksTitleHandler(queue));

    public static synchronized BlockingQueue<LinksTitleRequest> getHandlerQueue(){
        if (! thread.isAlive()){
            thread.start();
        }
        return queue;
    }

    public static void interrupt(){
        thread.interrupt();
    }
}
