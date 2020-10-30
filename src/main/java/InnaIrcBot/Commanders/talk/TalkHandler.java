package InnaIrcBot.Commanders.talk;

public interface TalkHandler {
    void nickCame(String newNick);
    void joinCame(String who);
    void privmsgCame(String who, String what);
}
