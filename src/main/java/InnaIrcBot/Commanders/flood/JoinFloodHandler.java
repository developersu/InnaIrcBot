package InnaIrcBot.Commanders.flood;

import InnaIrcBot.Commanders.EventHandler;
import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.time.LocalDateTime;
import java.util.*;

public class JoinFloodHandler implements EventHandler {
    private final int joinMaxNumber;                 // How many events should happens before we start validation
    private final int timeFrameInSeconds;             // For which period critical amount of events should happens
    private final String server;
    private final String channel;
    protected final HashMap<String, LinkedList<LocalDateTime>> users;

    public JoinFloodHandler(String server, String channel, int joinMaxNumber, int timeFrameInSeconds){
        this.server = server;
        this.channel = channel;
        this.joinMaxNumber = joinMaxNumber;
        this.timeFrameInSeconds = timeFrameInSeconds;
        this.users = new HashMap<>();
    }
    @Override
    public void track(String nick){
        nick = simplifyNick(nick);

        if (isNewcomer(nick)) {
            registerNewUser(nick);
            return;
        }

        if(isJoinFlooder(nick)){
            kickBanUser(nick);
            users.remove(nick);
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
        StreamProvider.writeToStream(server,"PRIVMSG "+ channel +" :"+user+": join flood ("+ joinMaxNumber +" connections in "+timeFrameInSeconds+" seconds).");
        StreamProvider.writeToStream(server,"MODE "+ channel +" +b "+user+"!*@*"); // TODO: consider other ban methods
    }

    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }
}