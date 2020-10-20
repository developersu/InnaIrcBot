package InnaIrcBot.logging;

public class WorkerZero implements Worker{
    @Override
    public boolean isConsistent() {return true;}

    @Override
    public boolean logAdd(String event, String initiator, String message) { return true; }

    @Override
    public void close() {}
}
