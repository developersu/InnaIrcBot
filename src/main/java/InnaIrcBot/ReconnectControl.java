package InnaIrcBot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReconnectControl {
    private static final Map<String, Boolean> serversList = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Integer> serversReconnects = Collections.synchronizedMap(new HashMap<>());

    public static void register(String server){
        serversList.putIfAbsent(server, true);
        serversReconnects.putIfAbsent(server, 0);
    }
    public static void update(String server, boolean needReconnect){
        serversList.replace(server, needReconnect);
    }

    public static synchronized void notify(String server){
        if (! serversList.getOrDefault(server, false))
            return;

        int count = serversReconnects.get(server);

        if (count > 5) {
            serversList.replace(server, false);
            return;
        }
        count++;
        serversReconnects.put(server, count);

        System.out.println("Main thread \"" + server + "\" removed from observable list after unexpected finish.\n");
        ConnectionsBuilder.getConnections().startNewConnection(server);
    }
}
