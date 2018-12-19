package InnaIrcBot.Commanders;

import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
//TODO: implement method to
public class JoinFloodHandler {
    private int eventBufferSize;                        // How many events should happens before we start validation
    private int timeFrameInSeconds;                     // For which period critical amount of events should happens
    private String server;
    private String chanel;
    private HashMap<String, LprFIFOQueue> usersOnChanel;

    public JoinFloodHandler(int events, int timeFrameInSeconds, String serverName, String chanelName){
        this.eventBufferSize = events;
        this.timeFrameInSeconds = timeFrameInSeconds;
        this.server = serverName;
        this.chanel = chanelName;
        this.usersOnChanel = new HashMap<>();
    }

    public void track(String userNick){
        if(usersOnChanel.containsKey(userNick)){
            LocalDateTime timeOfFirstEvent = usersOnChanel.get(userNick).addLastGetFirst();
            if (timeOfFirstEvent.isAfter(LocalDateTime.now().minusSeconds(timeFrameInSeconds))) {               // If first event in the queue happened after 'timeFrameSeconds ago from now'
                StreamProvider.writeToStream(server, "PRIVMSG  "+chanel+" :"+userNick+": join flood ("+eventBufferSize+" connections in "+timeFrameInSeconds+"seconds).\n"+
                                            "MODE "+chanel+" +b "+userNick+"!*@*");                         // Shut joins-liker down. By nick TODO: consider other ban methods
                usersOnChanel.remove(userNick);                                                                 // Delete viewing history (in case op/hop decided to unban)
            }
        }
        else {
            usersOnChanel.put(userNick, new LprFIFOQueue(eventBufferSize));             // Create buffer for tracking new user joined
            usersOnChanel.get(userNick).addLastGetFirst();                          // Write his/her first join time
        }
    }
}

class LprFIFOQueue {
    private int size;
    private ArrayList<LocalDateTime> dateTimes;// todo stack or deque

    LprFIFOQueue(int size){
        this.size = size;
        this.dateTimes = new ArrayList<>();
    }

    LocalDateTime addLastGetFirst(){        // FIFO-like
        // set
        if (dateTimes.size() >= size)
            dateTimes.remove(0);
        dateTimes.add(LocalDateTime.now()); // add current time
        // get
        if (dateTimes.size() < size)
            return LocalDateTime.MIN;       // todo: check if null is better
        else
            return dateTimes.get(0);
    }
}