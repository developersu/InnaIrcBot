package InnaIrcBot.Commanders;

import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

//TODO: Consider using as thread

public class CTCPHelper {
    private static final CTCPHelper instance = new CTCPHelper();
    public static CTCPHelper getInstance(){ return instance; }


    private HashMap<String, List<CtcpRequest>> waitersQueue = new HashMap<>();

    void registerRequest(String requesterServer, String requesterChanelOrUser, String ctcpType, String targetObject, String notFoundMessage){
        /*
        System.out.println("Server:|"+requesterServer+"|");
        System.out.println("Chanel:|"+requesterChanelOrUser+"|");
        System.out.println("Type  :|"+ctcpType+"|");
        System.out.println("Regexp:|"+targetObject+"|");
        System.out.println("NF_mes:|"+notFoundMessage+"|\n");       // could be empty
        */
        if (!waitersQueue.containsKey(requesterServer)){                // TODO: meeeeeeeehh.. looks bad
            waitersQueue.put(requesterServer, new ArrayList<>());
        }

        switch (ctcpType){
            case "VERSION":
            case "CLIENTINFO":
            case "FINGER":
            case "SOURCE":
            case "TIME":
            case "USERINFO":
                waitersQueue.get(requesterServer).add(new CtcpRequest(requesterChanelOrUser, targetObject, notFoundMessage, ctcpType));
                StreamProvider.writeToStream(requesterServer, "PRIVMSG "+targetObject+" :\u0001"+ctcpType+"\u0001");
                break;
            case "PING":                                                                                                                // TODO
                waitersQueue.get(requesterServer).add(new CtcpRequest(requesterChanelOrUser, targetObject, notFoundMessage, ctcpType));
                StreamProvider.writeToStream(requesterServer, "PRIVMSG "+targetObject+" :\u0001PING inna\u0001");
                break;
        }
    }

    public void handleCtcpReply(String serverReplied, String whoReplied, String whatReplied){
        //System.out.println("Reply serv:|"+serverReplied+"|\nwho:|"+whoReplied+"|\nwhat:|"+whatReplied+"|");
        LocalDateTime currentTime = LocalDateTime.now();
        if (waitersQueue.containsKey(serverReplied)){
            ListIterator<CtcpRequest> iterator = waitersQueue.get(serverReplied).listIterator();
            CtcpRequest current;
            String chanelOrUser;
            while (iterator.hasNext()){
                current = iterator.next();
                if (current.isValid(currentTime)){
                    chanelOrUser = current.getRequesterChanelOrUser(whoReplied);
                    if ( chanelOrUser != null && current.getType().equals(whatReplied.replaceAll("\\s.*$", ""))) {
                        if (whatReplied.equals("PING inna"))
                            StreamProvider.writeToStream(serverReplied, "PRIVMSG " + chanelOrUser + " :" + whoReplied + ": " + Duration.between(current.getCreationTime(), currentTime).toMillis()+"ms");
                        else
                            StreamProvider.writeToStream(serverReplied, "PRIVMSG " + chanelOrUser + " :" + whoReplied + ": " + whatReplied);
                        iterator.remove();
                    }
                }
                else{
                    //System.out.println("Drop outdated user");
                    iterator.remove();
                }
            }
        }
    }

    public void handleErrorReply(String serverReplied, String whoNotFound){
        //System.out.println("Reply serv:|"+serverReplied+"|\nwho:|"+whoNotFound+"|\n");
        LocalDateTime currentTime = LocalDateTime.now();
        if (waitersQueue.containsKey(serverReplied)){
            ListIterator<CtcpRequest> iterator = waitersQueue.get(serverReplied).listIterator();
            CtcpRequest current;
            String chanelOrUser;
            String notFoundMessage;
            while (iterator.hasNext()){
                current = iterator.next();
                if (current.isValid(currentTime)){
                    chanelOrUser = current.getRequesterChanelOrUser(whoNotFound);
                    if ( chanelOrUser != null) {
                        notFoundMessage = current.getNotFoundMessage(whoNotFound);
                        if ( notFoundMessage != null) {
                            System.out.println(serverReplied + " PRIVMSG " + chanelOrUser + " :" + notFoundMessage + whoNotFound);
                            StreamProvider.writeToStream(serverReplied, "PRIVMSG " + chanelOrUser + " :" + notFoundMessage + whoNotFound);
                        }
                        iterator.remove();
                    }
                }
                else{
                    //System.out.println("Drop outdated user: 401");
                    iterator.remove();
                }
            }
        }
    }
}

class CtcpRequest {
    private String requesterChanelOrUser;
    private String userResponding;
    private LocalDateTime initiatedTime;
    private String notFoundMessage;
    private String CTCPtype;
    CtcpRequest (String whoIsAsking, String userResponding, String notFoundMessage, String CTCPType){
        this.initiatedTime = LocalDateTime.now();
        this.requesterChanelOrUser = whoIsAsking;
        this.userResponding = userResponding;
        this.notFoundMessage = notFoundMessage;
        this.CTCPtype = CTCPType;
    }
    String getRequesterChanelOrUser(String userResponds){      // return channel name
        if (userResponding.equals(userResponds))
            return requesterChanelOrUser;
        else
            return null;
    }
    boolean isValid(LocalDateTime currentTime){
        return currentTime.isBefore(initiatedTime.plusSeconds(5));
    }

    String getType(){ return CTCPtype; }

    LocalDateTime getCreationTime(){ return initiatedTime; }

    String getNotFoundMessage(String userResponds){
        if (this.userResponding.equals(userResponds))
            if (notFoundMessage.isEmpty())
                return null;
            else
                return notFoundMessage;
        else
            return null;
    }
}
