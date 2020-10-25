package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.GlobalData;
import InnaIrcBot.logging.WorkerSystem;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SystemCTCP {
    private final String server;
    private LocalDateTime lastReplyTime;
    private final int cooldownTime;
    private final WorkerSystem writerWorker;
    
    SystemCTCP(String server, int cooldownTime, WorkerSystem writerWorker){
        this.server = server;
        this.lastReplyTime = LocalDateTime.now();
        this.cooldownTime = cooldownTime;
        this.writerWorker = writerWorker;
    }
    
    void replyCTCP(String sender, String message) {
        if (isTooManyRequests())
            return;

        lastReplyTime = LocalDateTime.now();

        switch (message) {
            case "\u0001VERSION\u0001":
                replyVersion(sender);
                log("CTCP VERSION from", sender);
                return;
            case "\u0001CLIENTINFO\u0001":
                replyClientInfo(sender);
                log("CTCP CLIENTINFO from", sender);
                return;
            case "\u0001TIME\u0001":
                replyTime(sender);
                log( "CTCP TIME from", sender);
                return;
            case "\u0001SOURCE\u0001":
                replySource(sender);
                log( "CTCP TIME from", sender);
                return;
        }
        if (message.startsWith("\u0001PING ") && message.endsWith("\u0001")) {
            replyPing(sender, message);
            log( "CTCP PING from", sender);
            return;
        }
        log( "CTCP not supported: \"" + message + "\" from ", sender);
    }
    
    private boolean isTooManyRequests(){
        return lastReplyTime.isAfter(LocalDateTime.now().minusSeconds(cooldownTime));
    }
    
    private void replyVersion(String sender){
        reply("NOTICE " + sender + " :\u0001VERSION " + GlobalData.getAppVersion() + "\u0001");
    }
    private void replyClientInfo(String sender){
        reply("NOTICE " + sender + " :\u0001CLIENTINFO ACTION PING VERSION TIME CLIENTINFO SOURCE\u0001");
    }
    private void replyTime(String sender){
        reply("NOTICE " + sender + " :\u0001TIME " + timeStamp() + "\u0001");
    }
    private void replySource(String sender){
        reply("NOTICE " + sender + " :\u0001SOURCE " + GlobalData.applicationHomePage + "\u0001");
    }
    private void replyPing(String sender, String message){
        reply("NOTICE " + sender + " :" + message);
    }

    private void reply(String message){
        StreamProvider.writeToStream(server, message);
    }
    
    private void log(String event, String sender){
        writerWorker.log(event, sender);
    }
    
    private String timeStamp(){
        return ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
