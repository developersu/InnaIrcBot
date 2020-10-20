package InnaIrcBot.Commanders;

import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.time.LocalDateTime;
import java.util.*;

public class JoinFloodHandler {
    private final int joinMaxNumber;                 // How many events should happens before we start validation
    private final int timeFrameInSeconds;             // For which period critical amount of events should happens
    private final String server;
    private final String channel;
    protected final HashMap<String, LinkedList<LocalDateTime>> users;

    public JoinFloodHandler(int joinMaxNumber, int timeFrameInSeconds, String serverName, String channelName){
        this.joinMaxNumber = joinMaxNumber;
        this.timeFrameInSeconds = timeFrameInSeconds;
        this.server = serverName;
        this.channel = channelName;
        this.users = new HashMap<>();
    }

    public void track(String userNickname){
        if (isNewcomer(userNickname)) {
            registerNewUser(userNickname);
            return;
        }

        if(isJoinFlooder(userNickname)){
            kickBanUser(userNickname);
            users.remove(userNickname);
        }
    }

    private boolean isNewcomer(String user){
        return ! users.containsKey(user);
    }

    private void registerNewUser(String user){
        users.put(user, new LinkedList<>());
        users.get(user).addFirst(LocalDateTime.now());
    }

    private boolean isJoinFlooder(String user){
        final LocalDateTime firstJoinTime = getFirstJoinTimeAndUpdate(user);
        return firstJoinTime.isAfter(LocalDateTime.now().minusSeconds(timeFrameInSeconds));
    }

    private LocalDateTime getFirstJoinTimeAndUpdate(String user){
        LinkedList<LocalDateTime> userJoinHistory = users.get(user);
        LocalDateTime fistJoinTime;

        userJoinHistory.addLast(LocalDateTime.now());

        if (userJoinHistory.size() > joinMaxNumber){
            fistJoinTime = userJoinHistory.getFirst();
            userJoinHistory.removeFirst();
        }
        else {
            fistJoinTime = LocalDateTime.MIN;
        }

        return fistJoinTime;
    }
    private void kickBanUser(String user){
        StreamProvider.writeToStream(server,
                "PRIVMSG  "+ channel +" :"+user+": join flood ("+ joinMaxNumber +" connections in "+timeFrameInSeconds+" seconds).\n"+
                "MODE "+ channel +" +b "+user+"!*@*"); // TODO: consider other ban methods
    }
}