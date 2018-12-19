package InnaIrcBot;

import java.util.HashMap;

public class ReconnectControl {
    private static HashMap<String, Boolean> serversList = new HashMap<>();
    public static synchronized void register(String serverName){
        serversList.put(serverName, true);
    }
    public static synchronized void update(String serverName, boolean needReconnect){
        serversList.replace(serverName, needReconnect);
    }
    public static synchronized boolean get(String serverName){  // could be null and it should be considered as false
        if (serversList.get(serverName) == null)
            return false;
        else
            return serversList.get(serverName);
    }
    public static synchronized void delete(String serverName){
        serversList.remove(serverName);
    }

}
