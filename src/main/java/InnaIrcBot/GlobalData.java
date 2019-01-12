package InnaIrcBot;

public class GlobalData {
    private static final String version = "InnaIrcBot v0.3 \"Карелия\"";
    public static synchronized String getAppVersion(){
        return version;
    }
}
