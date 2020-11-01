package InnaIrcBot.linkstitles;

public class LinksTitleRequest {
    private final String server;
    private final String channel;
    private final String message;

    public LinksTitleRequest(String server, String channel, String message){
        this.server = server;
        this.channel = channel;
        this.message = message;
    }

    public String getServer() { return server; }
    public String getChannel() { return channel; }
    public String getMessage() { return message; }
}
