package InnaIrcBot;

public class GlobalData {
    private static final String version = "InnaIrcBot v0.6 \"Большевик\"";
    public static synchronized String getAppVersion(){
        return version;
    }
}
