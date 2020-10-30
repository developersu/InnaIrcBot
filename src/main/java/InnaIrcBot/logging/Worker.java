package InnaIrcBot.logging;

public interface Worker {
    boolean isConsistent();

    void logAdd(String event, String initiator, String message) throws Exception;

    void close();
}
