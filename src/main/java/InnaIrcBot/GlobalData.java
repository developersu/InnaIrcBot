package InnaIrcBot;

public class GlobalData {
    private static final String version = "InnaIrcBot v0.7 \"Комсомолец\"";
    public static synchronized String getAppVersion(){
        return version;
    }
}
