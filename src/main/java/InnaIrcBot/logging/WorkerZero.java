package InnaIrcBot.logging;

public class WorkerZero implements Worker{
    @Override
    public boolean isConsistent() {return true;}

    @Override
    public void logAdd(String event, String initiator, String message) {}

    @Override
    public void close() {}
}
