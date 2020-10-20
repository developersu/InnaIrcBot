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
    public void track(String userNick){
        if (userNick.matches(pattern)){
            if (lastCame.isAfter(LocalDateTime.now().minusSeconds(timeFrameInSeconds)) && !prevUserNick.equals(userNick)){
                if (getNickOnly(userNick).replaceAll("[0-9].*", "").length() > 2){
                    StreamProvider.writeToStream(server,
                            "MODE "+chanel+" +b "+userNick.replaceAll("[0-9].*", "*!*@*")+"\n"+
                                    "MODE "+chanel+" +b *!*@"+getIdentHost(userNick)+"*\n"+
                                    "MODE "+chanel+" +b "+prevUserNick.replaceAll("[0-9].*", "*!*@*")+"\n"+
                                    "MODE "+chanel+" +b *!*@"+getIdentHost(prevUserNick)+"*\n"+
                                    "KICK "+chanel+" "+getNickOnly(userNick)+" :clone\n"+
                                    "KICK "+chanel+" "+getNickOnly(prevUserNick)+" :clone"
                    );
                }
                else {
                    StreamProvider.writeToStream(server,
                            "MODE "+chanel+" +b *!*@"+getIdentHost(userNick)+"*\n"+
                                    "MODE "+chanel+" +b *!*@"+getIdentHost(prevUserNick)+"*\n"+
                                    "KICK "+chanel+" "+getNickOnly(userNick)+" :clone\n"+
                                    "KICK "+chanel+" "+getNickOnly(prevUserNick)+" :clone"
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
