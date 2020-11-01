package InnaIrcBot.Commanders.flood;

import InnaIrcBot.Commanders.EventHandler;
import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.time.LocalDateTime;

public class JoinCloneHandler implements EventHandler {

    private final String pattern;
    private final String server;
    private final String channel;
    private final int timeFrameInSeconds;

    private LocalDateTime lastCame;
    private String prevUserNick;

    public JoinCloneHandler(String server, String channel, String pattern, int timeFrameInSeconds){
        this.server = server;
        this.channel = channel;
        this.pattern = pattern;
        this.timeFrameInSeconds = timeFrameInSeconds;
        prevUserNick = "";
        lastCame = LocalDateTime.now().minusDays(1L);
    }
    /*
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
    */
    // RUSNET
    @Override
    public void track(String userNick){
        if (userNick.matches(pattern)){
            if (lastCame.isAfter(LocalDateTime.now().minusSeconds(timeFrameInSeconds)) && !prevUserNick.equals(userNick)){
                if (getNickOnly(userNick).replaceAll("[0-9].*", "").length() > 2){
                    StreamProvider.writeToStream(server,
                            "MODE "+ channel +" +b "+userNick.replaceAll("[0-9].*", "*!*@*")+"\n"+
                                    "MODE "+ channel +" +b *!*@"+getIdentHost(userNick)+"*\n"+
                                    "MODE "+ channel +" +b "+prevUserNick.replaceAll("[0-9].*", "*!*@*")+"\n"+
                                    "MODE "+ channel +" +b *!*@"+getIdentHost(prevUserNick)+"*\n"+
                                    "KICK "+ channel +" "+getNickOnly(userNick)+" :clone\n"+
                                    "KICK "+ channel +" "+getNickOnly(prevUserNick)+" :clone"
                    );
                }
                else {
                    StreamProvider.writeToStream(server,
                            "MODE "+ channel +" +b *!*@"+getIdentHost(userNick)+"*\n"+
                                    "MODE "+ channel +" +b *!*@"+getIdentHost(prevUserNick)+"*\n"+
                                    "KICK "+ channel +" "+getNickOnly(userNick)+" :clone\n"+
                                    "KICK "+ channel +" "+getNickOnly(prevUserNick)+" :clone"
                    );
                }

            }
            prevUserNick = userNick;
            lastCame = LocalDateTime.now();
        }
    }
    private String getIdentHost(String fullNick){
        String id = fullNick.replaceAll("^.*@","");
        if (id.contains(":"))
            return id.replaceAll("^([A-Fa-f0-9]{1,4}:[A-Fa-f0-9]{1,4}:)(.+)$", "$1")+"*";
        else
            return id;
    }
    private String getNickOnly(String fullNick){
        return fullNick.replaceAll("!.*$","");
    }
}
