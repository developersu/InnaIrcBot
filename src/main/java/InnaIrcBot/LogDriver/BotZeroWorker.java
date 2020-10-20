package InnaIrcBot.LogDriver;

public class BotZeroWorker implements Worker{
    @Override
    public boolean isConsistent() {return true;}

    @Override
    public boolean logAdd(String event, String initiator, String message) { return true; }

    @Override
    public void close() {}
}
