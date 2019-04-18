package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.Commanders.CTCPHelper;
import InnaIrcBot.Commanders.PrivateMsgCommander;
import InnaIrcBot.Config.StorageFile;
import InnaIrcBot.GlobalData;
import InnaIrcBot.LogDriver.BotDriver;
import InnaIrcBot.LogDriver.BotSystemWorker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SystemConsumer implements Runnable{
    private BufferedReader reader;
    private BotSystemWorker writerWorker;
    private String nick;
    private String serverName;
    private Map<String, PrintWriter> channelsMap;
    private boolean proxyRequired;
    private HashMap<String, ArrayList<String>> proxyAList;
    private StorageFile storageFile;

    private PrivateMsgCommander commander;

    private LocalDateTime lastCTCPReplyTime;

    SystemConsumer(BufferedReader streamReader, String userNick, Map<String, PrintWriter>  map, StorageFile storage) {
        this.writerWorker = BotDriver.getSystemWorker(storage.getServerName());
        this.nick = userNick;
        this.serverName = storage.getServerName();
        this.channelsMap = map;
        this.reader = streamReader;

        this.proxyRequired = false;
        this.proxyAList = new HashMap<>();
        this.storageFile = storage;

        this.commander = new PrivateMsgCommander(serverName, storageFile.getBotAdministratorPassword());
        // Start pre-set channels
        StringBuilder message = new StringBuilder();
        for (String cnl : storageFile.getChannels()) {       // TODO: add validation of channels.
            message.append("JOIN ");
            message.append(cnl);
            message.append("\n");
        }
        StreamProvider.writeToStream(serverName,message.toString());

        lastCTCPReplyTime = LocalDateTime.now();
    }

    @Override
    public void run() {
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] THREAD "+serverName+":[system] started");      // TODO:REMOVE DEBUG

        setMainRoutine();

        for (PrintWriter p :channelsMap.values())                                                        //TODO: check, code duplication. see Data provider constructor
            p.close();

        writerWorker.close();
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] THREAD "+serverName+":[system] ended");       // TODO:REMOVE DEBUG
    }

    private void setMainRoutine(){
        String data;
        String[] dataStrings;
        try {
            while ((data = reader.readLine()) != null) {
                dataStrings = data.split(" ",3);

                if (proxyRequired)
                    if (getProxy(dataStrings[0], dataStrings[1], dataStrings[2]))
                        continue;                                                                       // TODO: check this. Continue is fair?

                if (Pattern.matches("(^[0-9]{3}$)|(^NICK$)|(^JOIN$)|(^QUIT$)|(^NOTICE$)", dataStrings[0])){
                    handleNumeric(dataStrings[0], dataStrings[1], dataStrings[2]);
                }
                else if (dataStrings[0].equals("PRIVMSG")) {
                    if (dataStrings[2].indexOf("\u0001") < dataStrings[2].lastIndexOf("\u0001")) {
                        replyCTCP(simplifyNick(dataStrings[1]), dataStrings[2].substring(dataStrings[2].indexOf(":") + 1));
                        //System.out.println("|"+dataStrings[1]+"|"+dataStrings[2].substring(dataStrings[2].indexOf(":") + 1)+"|");
                    }
                    else {
                        commander.receiver(dataStrings[1], dataStrings[2].replaceAll("^.+?:", "").trim());
                        writerWorker.logAdd("[system]", "PRIVMSG from "+dataStrings[1]+" received: ", dataStrings[2].replaceAll("^.+?:", "").trim());
                    }
                }
                else if (dataStrings[0].equals("INNA")) {
                    String[] splitter;
                    if (dataStrings.length > 2){                                                        // Don't touch 'cuz it's important
                        splitter = dataStrings[2].split(" ", 2);
                        if (splitter.length == 2){
                            handleSpecial(dataStrings[1], splitter[0], splitter[1]);
                        }
                    }
                }
                else
                    writerWorker.logAdd(dataStrings[0], dataStrings[1], dataStrings[2]);                            // TODO: Track users
                //System.out.println("System: "+"|"+dataStrings[0]+"|"+dataStrings[1]+"|"+dataStrings[2]+"|");
            }
        } catch (java.io.IOException e){
                System.out.println("Internal issue: thread SystemConsumer->run() caused I/O exception.");           // TODO: reconnect OR AT LEAST DIE
                StreamProvider.writeToStream(serverName, "QUIT :Internal issue: thread ChanConsumer->run() caused I/O exception");
        }
    }
    private boolean getProxy(String eventNum, String sender, String message){                              //TODO: if can't join: like channel with password
        if (eventNum.equals("353")) {
            //writerWorker.logAdd("[proxy]", "catch: "+eventNum+" from: "+sender+" :",message);
            return false;               // never mind and let it flows as usual.
        }
        else {
            //writerWorker.logAdd("[proxy]", "catch: "+eventNum+" from: "+sender+" :",message);
            String chan = message.replaceAll("(\\s.?$)|(\\s.+?$)", "");

            if (eventNum.equals("QUIT") || eventNum.equals("NICK")) {
                for (ArrayList<String> key : proxyAList.values())
                    key.add(eventNum + " " + sender + " " + message);
                return false;
            } else if (chan.equals(nick))
                return false;
            else if (proxyAList.keySet().contains(chan)) {
                proxyAList.get(chan).add(eventNum + " " + sender + " " + message);
                return true;
            } else
                return false;
        }
    }

    private void replyCTCP(String sender, String message){      // got simplified nick
        if (lastCTCPReplyTime.isBefore(LocalDateTime.now().minusSeconds(3))) {                                                 // TODO: Consider moving to config file. Now set to 3 sec
            lastCTCPReplyTime = LocalDateTime.now();
            if (message.equals("\u0001VERSION\u0001")) {
                StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :\u0001VERSION " + GlobalData.getAppVersion() + "\u0001");
                writerWorker.logAdd("[system]", "catch/handled CTCP VERSION from", sender);
                //System.out.println("NOTICE "+sender+" \u0001VERSION "+ GlobalData.getAppVersion()+"\u0001");
            } else if (message.startsWith("\u0001PING ") && message.endsWith("\u0001")) {
                StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :" + message);
                writerWorker.logAdd("[system]", "catch/handled CTCP PING from", sender);
                //System.out.println(":"+sender+" NOTICE "+sender.substring(0,sender.indexOf("!"))+" "+message);
            } else if (message.equals("\u0001CLIENTINFO\u0001")) {
                StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :\u0001CLIENTINFO ACTION PING VERSION TIME CLIENTINFO SOURCE\u0001");
                writerWorker.logAdd("[system]", "catch/handled CTCP CLIENTINFO from", sender);
                //System.out.println(":"+sender+" NOTICE "+sender.substring(0,sender.indexOf("!"))+" \u0001CLIENTINFO ACTION PING VERSION TIME CLIENTINFO\u0001");
            } else if (message.equals("\u0001TIME\u0001")) {
                StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :\u0001TIME " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\u0001");
                writerWorker.logAdd("[system]", "catch/handled CTCP TIME from", sender);
                //System.out.println(":"+sender+" NOTICE "+sender.substring(0,sender.indexOf("!"))+" \u0001TIME "+ ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)+"\u0001");
            } else if (message.equals("\u0001SOURCE\u0001")) {
                StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :\u0001SOURCE https://github.com/developersu/InnaIrcBot\u0001");
                writerWorker.logAdd("[system]", "catch/handled CTCP TIME from", sender);
                //System.out.println(":"+sender+" NOTICE "+sender.substring(0,sender.indexOf("!"))+" \u0001SOURCE "+ ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)+"\u0001");
            } else
                writerWorker.logAdd("[system]", "catch unknown CTCP request \"" + message + "\" from ", sender);
        }
    }

    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }

    private void handleSpecial(String event, String chanel, String message){
        //System.out.println("|"+event+"|"+chanel+"|"+message+"|");
        if (channelsMap.containsKey(chanel)){
            channelsMap.get(chanel).println(event+" "+nick+" "+chanel+" "+message);         // WTF ><
            channelsMap.get(chanel).flush();
            //System.out.println("Formatted: |"+event+"|"+nick+"|"+chanel+" "+message+"|");
        }
    }
    //todo: nandle nickserv messages
    private void handleNumeric(String eventNum, String sender, String message){
        switch (eventNum){
            case "433":                                                             // TODO: try to use alternative nickname
                writerWorker.logAdd("[system]", "catch/handled:", eventNum+" [nickname already in use]");
                break;
            case "353":
                writerWorker.logAdd("[system]", "catch/handled:", eventNum+" [RPL_NAMREPLY]");
                String chan = message.substring(message.indexOf(" ")+3);
                chan = chan.substring(0, chan.indexOf(" "));
                if (proxyAList.containsKey(chan)) {
                    String userOnChanStr = message.substring(message.indexOf(":") + 1);
                    userOnChanStr = userOnChanStr.replaceAll("[%@+]", "").trim();
                    String[] usersOnChanArr = userOnChanStr.split(" ");

                    PipedOutputStream streamOut = new PipedOutputStream();
                    try {
                        BufferedReader streamBufferedReader = new BufferedReader(
                                new InputStreamReader(
                                        new PipedInputStream(streamOut), StandardCharsets.UTF_8)
                        );

                        channelsMap.put(chan, new PrintWriter(streamOut));
                        // % @ +
                        ChanConsumer consumer = new ChanConsumer(streamBufferedReader, storageFile.getServerName(), chan, nick, usersOnChanArr, storageFile.getRejoinOnKick(), channelsMap, storageFile.getChanelConfigurationsPath());
                        new Thread(consumer).start();

                        for (String msgStored : proxyAList.get(chan)) {
                            channelsMap.get(chan).println(msgStored);
                            channelsMap.get(chan).flush();
                        }

                        proxyAList.remove(chan);
                        if (proxyAList.isEmpty()) {
                            proxyRequired = false;
                        }
                    } catch (IOException e) {
                        System.out.println("Internal issue: SystemConsumer->handleNumeric() @ JOIN: I/O exception while initialized child objects.");             // caused by Socket
                        return;                                                                                                                             //TODO: QA
                    }
                }
                else
                    System.out.println("Some internal shit happens that shouldn't happens never ever. Take your cat, call scientists and wait for singularity. Panic allowed. Log: \nEvent:|"+eventNum+"| sender:|"+sender+"| message|"+message+"|");
                break;
            case "NICK":
                if (sender.startsWith(nick+"!")) {
                    nick = message.trim();
                    writerWorker.logAdd("[system]", "catch own NICK change:", sender+" to: "+message);
                }
                break;
            case "JOIN":
                if (sender.startsWith(nick+"!")) {
                    proxyAList.put(message, new ArrayList<>());                         // Add new channel name to proxy watch-list
                    proxyAList.get(message).add(eventNum+" "+sender+" "+message);       // Add message to array linked
                    this.proxyRequired = true;                                          // Ask for proxy validators
                    writerWorker.logAdd("[system]", "joined to channel ", message);
                }
                break;
            case "401":     // No such nick/channel
                //System.out.println("|"+message.replaceAll("^(\\s)?.+?(\\s)|((\\s)?:No such nick/channel)","")+"|");
                CTCPHelper.getInstance().handleErrorReply(serverName,  message.replaceAll("^(\\s)?.+?(\\s)|((\\s)?:No such nick/channel)",""));
                writerWorker.logAdd("[system]", "catch: "+eventNum+" from: "+sender+" :",message+" [ok]");
                break;
            case "NOTICE":
                CTCPHelper.getInstance().handleCtcpReply(serverName, simplifyNick(sender), message.replaceAll("^.+?:", "").trim());
                writerWorker.logAdd("[system]", "NOTICE from "+sender+" received: ", message.replaceAll("^.+?:", "").trim());
                break;
            case "QUIT":
                break;
            default:
                writerWorker.logAdd("[system]", "catch: "+eventNum+" from: "+sender+" :",message);  // TODO: QUIT comes here. Do something?
                break;
        }
    }
}