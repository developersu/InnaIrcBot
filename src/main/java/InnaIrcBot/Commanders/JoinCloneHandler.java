package InnaIrcBot.Commanders;

import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.time.LocalDateTime;

public class JoinCloneHandler {

    private String pattern;
    private String server;
    private String chanel;
    private int timeFrameInSeconds;

    private LocalDateTime lastCame;
    private String prevUserNick;

    public JoinCloneHandler(String pattern, int timeFrameInSeconds, String serverName, String chanelName){
        this.pattern = pattern;
        this.timeFrameInSeconds = timeFrameInSeconds;
        this.server = serverName;
        this.chanel = chanelName;
        prevUserNick = "";
        lastCame = LocalDateTime.now().minusDays(1L);
    }

    public void track(String userNick){
        if (userNick.matches(pattern)){
            if (lastCame.isAfter(LocalDateTime.now().minusSeconds(timeFrameInSeconds)) && !prevUserNick.equals(userNick)){
                StreamProvider.writeToStream(server,
                        "MODE "+chanel+" +b *!*"+getIdentHost(userNick)+"\n"+
                                "MODE "+chanel+" +b *!*"+getIdentHost(prevUserNick)+"\n"+
                                "KICK "+chanel+" "+getNickOnly(userNick)+" :clone\n"+
                                "KICK "+chanel+" "+getNickOnly(prevUserNick)+" :clone"
                );
            }
            prevUserNick = userNick;
            lastCame = LocalDateTime.now();
        }
    }
    private String getIdentHost(String fullNick){
        return fullNick.replaceAll("^.*@","@");
    }
    private String getNickOnly(String fullNick){
        return fullNick.replaceAll("!.*$","");
    }
}
