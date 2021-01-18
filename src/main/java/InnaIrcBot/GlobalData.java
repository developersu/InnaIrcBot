package InnaIrcBot;

public class GlobalData {
    private static final String version = "InnaIrcBot v0.9 \"Литке\"";
    public static synchronized String getAppVersion(){
        return String.format("%s, %s %s %s", version,
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
    }
    public static final String applicationHomePage = "https://github.com/developersu/InnaIrcBot";
    public static final int CHANNEL_QUEUE_CAPACITY = 500;
    public static final int LINKS_COOLDOWN_FRAME = 3;
}
