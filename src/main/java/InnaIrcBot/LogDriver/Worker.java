package InnaIrcBot.LogDriver;

public interface Worker {
    boolean consistent = false;

    boolean isConsistent();

    void logAdd(String event,
                String initiatorArg,
                String messageArg);

    void close();
}
