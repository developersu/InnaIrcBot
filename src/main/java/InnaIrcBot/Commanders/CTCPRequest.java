package InnaIrcBot.Commanders;

import java.time.LocalDateTime;

class CTCPRequest {
    private final String requesterChanelOrUser;
    private final String userResponding;
    private final LocalDateTime initiatedTime;
    private final String notFoundMessage;
    private final String CTCPtype;

    CTCPRequest(String whoIsAsking, String userResponding, String notFoundMessage, String CTCPType){
        this.initiatedTime = LocalDateTime.now();
        this.requesterChanelOrUser = whoIsAsking;
        this.userResponding = userResponding;
        this.notFoundMessage = notFoundMessage;
        this.CTCPtype = CTCPType;
    }

    String getRequester(String userResponds){      // return channel name
        if (userResponding.equals(userResponds))
            return requesterChanelOrUser;
        return null;
    }

    boolean isValid(LocalDateTime currentTime){
        return currentTime.isBefore(initiatedTime.plusSeconds(5));
    }

    String getType(){ return CTCPtype; }

    LocalDateTime getCreationTime(){ return initiatedTime; }

    String getNotFoundMessage(String userResponds){
        if (userResponding.equals(userResponds) && ! notFoundMessage.isEmpty())
            return notFoundMessage;
        return null;
    }
}