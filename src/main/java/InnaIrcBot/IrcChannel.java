package InnaIrcBot;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class IrcChannel {
    private final String name;
    private final BlockingQueue<String> channelQueue;

    public IrcChannel(String channelName){
        this.name = channelName;
        this.channelQueue = new ArrayBlockingQueue<>(GlobalData.CHANNEL_QUEUE_CAPACITY);
    }

    @Override
    public String toString(){
        return name;
    }

    public BlockingQueue<String> getChannelQueue() {
        return channelQueue;
    }
}
