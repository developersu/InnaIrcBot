package InnaIrcBot;

public class GlobalData {
    private static final String version = "InnaIrcBot v0.5.1 \"Шикотан\"";
    public static synchronized String getAppVersion(){
        return version;
    }
}
