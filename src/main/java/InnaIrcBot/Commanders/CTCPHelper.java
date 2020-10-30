package InnaIrcBot.Commanders;

import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

//TODO: Consider using as thread

public class CTCPHelper {
    private static final CTCPHelper instance = new CTCPHelper();
    public static CTCPHelper getInstance(){ return instance; }

    private final HashMap<String, List<CTCPRequest>> waitersQueue = new HashMap<>();
    private CTCPHelper(){}

    public void registerRequest(String server, String requesterChanelOrUser, String ctcpType, String target, String notFoundMessage){
        if (! waitersQueue.containsKey(server)){                // TODO: meeeeeeeehh.. looks bad
            waitersQueue.put(server, new ArrayList<>());
        }

        switch (ctcpType){
            case "VERSION":
            case "CLIENTINFO":
            case "FINGER":
            case "SOURCE":
            case "TIME":
            case "USERINFO":
                waitersQueue.get(server).add(new CTCPRequest(requesterChanelOrUser, target, notFoundMessage, ctcpType));
                StreamProvider.writeToStream(server, "PRIVMSG "+target+" :\u0001"+ctcpType+"\u0001");
                break;
            case "PING":                                                                                                                // TODO
                waitersQueue.get(server).add(new CTCPRequest(requesterChanelOrUser, target, notFoundMessage, ctcpType));
                StreamProvider.writeToStream(server, "PRIVMSG "+target+" :\u0001PING inna\u0001");
                break;
        }
    }

    public void handleCtcpReply(String serverReplied, String whoReplied, String whatReplied){
        LocalDateTime currentTime = LocalDateTime.now();

        if (! waitersQueue.containsKey(serverReplied))
            return;

        ListIterator<CTCPRequest> iterator = waitersQueue.get(serverReplied).listIterator();

        while (iterator.hasNext()){
            CTCPRequest current = iterator.next();
            if (current.isValid(currentTime)){
                String channelOrUser = current.getRequester(whoReplied);

                if (channelOrUser == null || ! current.getType().equals(whatReplied.replaceAll("\\s.*$", "")))
                    continue;

                if (whatReplied.equals("PING inna"))
                    StreamProvider.writeToStream(serverReplied, "PRIVMSG " + channelOrUser + " :" + whoReplied + ": " +
                            Duration.between(current.getCreationTime(), currentTime).toMillis()+"ms");
                else
                    StreamProvider.writeToStream(serverReplied, "PRIVMSG " + channelOrUser + " :" + whoReplied + ": " + whatReplied);
            }
            iterator.remove();

        }
    }

    public void handleErrorReply(String serverReplied, String whoNotFound){
        //System.out.println("Reply serv:|"+serverReplied+"|\nwho:|"+whoNotFound+"|\n");
        LocalDateTime currentTime = LocalDateTime.now();
        if (! waitersQueue.containsKey(serverReplied))
            return;

        ListIterator<CTCPRequest> iterator = waitersQueue.get(serverReplied).listIterator();

        while (iterator.hasNext()){
            CTCPRequest current = iterator.next();
            if (! current.isValid(currentTime))
                iterator.remove();
            String channelOrUser = current.getRequester(whoNotFound);

            if (channelOrUser == null)
                continue;

            String notFoundMessage = current.getNotFoundMessage(whoNotFound);

            if (notFoundMessage == null)
                continue;

            System.out.println(serverReplied + " PRIVMSG " + channelOrUser + " :" + notFoundMessage + whoNotFound);
            StreamProvider.writeToStream(serverReplied, "PRIVMSG " + channelOrUser + " :" + notFoundMessage + whoNotFound);
        }
    }
}
