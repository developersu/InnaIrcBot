package InnaIrcBot.Commanders.talk;

public class TalkZeroHandler implements TalkHandler{
    @Override
    public void nickCame(String newNick) { }

    @Override
    public void joinCame(String who) { }

    @Override
    public void privmsgCame(String who, String what) { }
}
