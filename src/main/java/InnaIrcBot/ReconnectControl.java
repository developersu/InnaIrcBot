package InnaIrcBot;

import java.util.HashMap;

public class ReconnectControl {
    private static final HashMap<String, Boolean> serversList = new HashMap<>();
    public static synchronized void register(String serverName){
        serversList.put(serverName, true);
    }
    public static synchronized void update(String serverName, boolean needReconnect){
        serversList.replace(serverName, needReconnect);
    }

    public static synchronized void notify(String serverName) {
        if (serversList.get(serverName) == null || ! serversList.get(serverName)){
            serversList.remove(serverName);
            return;
        }

        System.out.println("DEBUG: Thread "+serverName+" removed from observable list after unexpected finish.\n\t");
        ConnectionsBuilder.getConnections().startNewConnection(serverName);
    }
}
