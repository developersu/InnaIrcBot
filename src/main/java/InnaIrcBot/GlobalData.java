package InnaIrcBot;

public class GlobalData {
    private static final String version = "InnaIrcBot v0.4 \"Карские Ворота\"";
    public static synchronized String getAppVersion(){
        return version;
    }
}
