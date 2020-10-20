package InnaIrcBot;

import InnaIrcBot.logging.LogDriver;

import java.util.ArrayList;
import java.util.List;

public class GlobalData {
    private static final String version = "InnaIrcBot v0.8 \"Коммунарка\"";
    public static synchronized String getAppVersion(){
        return version;
    }
    public static final int CHANNEL_QUEUE_CAPACITY = 500;
}
