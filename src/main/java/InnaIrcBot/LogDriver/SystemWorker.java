package InnaIrcBot.LogDriver;

public interface SystemWorker {

    void registerInSystemWorker(ThingToCloseOnDie thing);

    void logAdd(String event,
                   String initiatorArg,
                   String messageArg);

    void close();
}
