package InnaIrcBot.LogDriver;

public class BotZeroWorker implements Worker{
    @Override
    public boolean isConsistent() {return true;}

    @Override
    public void logAdd(String event, String initiatorArg, String messageArg) {}

    @Override
    public void close() {}
}
