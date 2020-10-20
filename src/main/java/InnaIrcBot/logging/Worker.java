package InnaIrcBot.logging;

public interface Worker {
    boolean consistent = false;

    boolean isConsistent();

    boolean logAdd(String event,
                String initiator,
                String message);

    void close();
}
