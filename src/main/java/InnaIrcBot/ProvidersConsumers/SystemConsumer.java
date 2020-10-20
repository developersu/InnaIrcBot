package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.Commanders.CTCPHelper;
import InnaIrcBot.Commanders.PrivateMsgCommander;
import InnaIrcBot.ReconnectControl;
import InnaIrcBot.config.ConfigurationFile;
import InnaIrcBot.GlobalData;
import InnaIrcBot.IrcChannel;
import InnaIrcBot.LogDriver.BotDriver;
import InnaIrcBot.LogDriver.BotSystemWorker;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class SystemConsumer implements Runnable{
    private final BlockingQueue<String> systemQueue;
    private BotSystemWorker writerWorker;
    private String nick;
    private String serverName;
    private final Map<String, IrcChannel> channels;
    private ConfigurationFile configurationFile;

    private PrivateMsgCommander commander;

    private LocalDateTime lastCTCPReplyTime;

    private ArrayList<Thread> channelThreads;
    private int nickTail = 0;

    SystemConsumer(BlockingQueue<String> systemQueue, String userNick, Map<String, IrcChannel> channels, ConfigurationFile configurationFile) {
        this.systemQueue = systemQueue;
        this.writerWorker = BotDriver.getSystemWorker(configurationFile.getServerName());
        this.nick = userNick;
        this.serverName = configurationFile.getServerName();
        this.channels = channels;
        this.channelThreads = new ArrayList<>();
        this.configurationFile = configurationFile;
        this.commander = new PrivateMsgCommander(serverName, this.configurationFile.getBotAdministratorPassword());

        lastCTCPReplyTime = LocalDateTime.now();
    }

    @Override
    public void run() {
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] THREAD "+serverName+":[system] started");      // TODO:REMOVE DEBUG

        setMainRoutine();

        for (Thread channel : channelThreads) {   //TODO: check, code duplication. see Data provider constructor
            channel.interrupt();
        }

        writerWorker.close();
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] THREAD "+serverName+":[system] ended");       // TODO:REMOVE DEBUG
    }

    private void setMainRoutine(){
        try {
            while (true) {
                String data = systemQueue.take();
                String[] dataStrings = data.split(" ",3);
                //TODO: handle mode change
                switch (dataStrings[0]){
                    case "PRIVMSG":
                        if (dataStrings[2].indexOf("\u0001") < dataStrings[2].lastIndexOf("\u0001")) {
                            replyCTCP(simplifyNick(dataStrings[1]), dataStrings[2].substring(dataStrings[2].indexOf(":") + 1));
                        }
                        else {
                            commander.receiver(dataStrings[1], dataStrings[2].replaceAll("^.+?:", "").trim());
                            writerWorker.logAdd("[system]", "PRIVMSG from "+dataStrings[1]+" received: ",
                                    dataStrings[2].replaceAll("^.+?:", "").trim());
                        }
                        break;
                    case "INNA":
                        String[] splitter;
                        if (dataStrings.length > 2){                                                        // Don't touch 'cuz it's important
                            splitter = dataStrings[2].split(" ", 2);
                            if (splitter.length == 2){
                                handleSpecial(dataStrings[1], splitter[0], splitter[1]);
                            }
                        }
                        break;
                    default:
                        handleNumeric(dataStrings[0], dataStrings[1], dataStrings[2]);
                }
            }
        }
        catch (InterruptedException ie){
            System.out.println("Thread SystemConsumer->run() interrupted.");           // TODO: reconnect OR AT LEAST DIE
        }
        catch (Exception e){
            System.out.println("Internal issue: thread SystemConsumer->run(): "+e.getMessage());           // TODO: DO.. some thing
        }
    }

    private void replyCTCP(String sender, String message) {      // got simplified nick
        // TODO: Consider moving to config file. Now set to 3 sec
        if (lastCTCPReplyTime.isAfter(LocalDateTime.now().minusSeconds(3)))
            return;

        lastCTCPReplyTime = LocalDateTime.now();

        switch (message) {
            case "\u0001VERSION\u0001":
                StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :\u0001VERSION " + GlobalData.getAppVersion() + "\u0001");
                writerWorker.logAdd("[system]", "catch/handled CTCP VERSION from", sender);
                return;
            case "\u0001CLIENTINFO\u0001":
                StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :\u0001CLIENTINFO ACTION PING VERSION TIME CLIENTINFO SOURCE\u0001");
                writerWorker.logAdd("[system]", "catch/handled CTCP CLIENTINFO from", sender);
                return;
            case "\u0001TIME\u0001":
                StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :\u0001TIME " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\u0001");
                writerWorker.logAdd("[system]", "catch/handled CTCP TIME from", sender);
                return;
            case "\u0001SOURCE\u0001":
                StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :\u0001SOURCE https://github.com/developersu/InnaIrcBot\u0001");
                writerWorker.logAdd("[system]", "catch/handled CTCP TIME from", sender);
                return;
        }
        if (message.startsWith("\u0001PING ") && message.endsWith("\u0001")) {
            StreamProvider.writeToStream(serverName, "NOTICE " + sender + " :" + message);
            writerWorker.logAdd("[system]", "catch/handled CTCP PING from", sender);
            return;
        }
        writerWorker.logAdd("[system]", "catch unknown CTCP request \"" + message + "\" from ", sender);
    }

    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }

    private void handleSpecial(String event, String channelName, String message){
        IrcChannel ircChannel = channels.get(channelName);
        if (ircChannel == null)
            return;
        String ircFormatterMessage = event+" "+nick+" "+channelName+" "+message;
        //System.out.println("Formatted: |"+event+"|"+nick+"|"+channelName+" "+message+"|");
        ircChannel.getChannelQueue().add(ircFormatterMessage);
    }
    //todo: handle nickserv messages somehow
    private void handleNumeric(String eventNum, String sender, String message) throws Exception{
        switch (eventNum){
            case "501":                                                             // Notify user about incorrect setup
                writerWorker.logAdd("[system]", "catch/handled:", eventNum
                        + " [MODE message was sent with a nickname parameter and that the a mode flag sent was not recognized.]");
                break;
            case "433":                                                             // TODO: try to use alternative nickname
                writerWorker.logAdd("[system]", "catch/handled:", eventNum
                        + " [nickname already in use and will be changed]");
                break;
            case "353":
                writerWorker.logAdd("[system]", "catch/handled:", eventNum+" [RPL_NAMREPLY]");
                String channelName = message.substring(nick.length()+3).replaceAll("\\s.*$", "");

                IrcChannel ircChannel = channels.get(channelName);
                if (ircChannel == null)
                    return;
                ircChannel.getChannelQueue().add(eventNum+" "+sender+" "+message);
                break;
            case "NICK":
                if (sender.startsWith(nick+"!")) {
                    nick = message.trim();
                    writerWorker.logAdd("[system]", "catch own NICK change:", sender+" to: "+message);
                }
                break;
            case "JOIN":
                if (sender.startsWith(nick+"!")) {
                    IrcChannel newIrcChannel = new IrcChannel(message);

                    channels.put(message, newIrcChannel);
                    // % @ +
                    ChanConsumer consumer = new ChanConsumer(
                            configurationFile.getServerName(),
                            newIrcChannel,
                            nick,
                            channels);
                    Thread newIrcChannelThread = new Thread(consumer);
                    newIrcChannelThread.start();
                    channelThreads.add(newIrcChannelThread);
                    //proxyAList.get(message).add(eventNum+" "+sender+" "+message);       // Add message to array linked
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
            case "001":
                sendUserModes();
                sendNickPassword();
                joinChannels();
                break;
            case "443":
                String newNick = nick+"|"+nickTail++;
                StreamProvider.writeToStream(serverName,"NICK "+newNick);
                break;
            case "464":  // password for server/znc/bnc
                StreamProvider.writeToStream(serverName,"PASS "+configurationFile.getServerPass());
                break;
            case "432":
                System.out.println("Configuration issue: Nickname contains unacceptable characters (432 ERR_ERRONEUSNICKNAME).");
                ReconnectControl.update(serverName, false);

                break;
            case "465":
                ReconnectControl.update(serverName, false);

                break;
            case "QUIT":  // TODO: Do something?
                break;
            default:
                writerWorker.logAdd("[system]", "catch: "+eventNum+" from: "+sender+" :",message);
                break;
            // 431  ERR_NONICKNAMEGIVEN     how can we get this?
            // 436  ERR_NICKCOLLISION
        }
    }

    private void sendUserModes(){
        String modes = configurationFile.getUserMode().replaceAll("[\t\\s]", "");

        if (modes.isEmpty())
            return;

        StringBuilder message = new StringBuilder();
        for(char mode : modes.toCharArray()) {
            message.append("MODE ");
            message.append(nick);
            message.append(" +");
            message.append(mode);
            message.append("\n");
        }

        StreamProvider.writeToStream(serverName, message.toString());
    }

    private void sendNickPassword(){
        if (configurationFile.getUserNickPass().isEmpty())
            return;

        switch (configurationFile.getUserNickAuthStyle()){
            case "freenode":
                StreamProvider.writeToStream(serverName,"PRIVMSG NickServ :IDENTIFY "
                        + configurationFile.getUserNickPass());
                break;
            case "rusnet":
                StreamProvider.writeToStream(serverName,"NickServ IDENTIFY "
                        + configurationFile.getUserNickPass());
        }
    }

    private void joinChannels(){
        StringBuilder joinMessage = new StringBuilder();

        for (String cnl : configurationFile.getChannels()) {       // TODO: add validation of channels.
            joinMessage.append("JOIN ");
            joinMessage.append(cnl);
            joinMessage.append("\n");
        }

        StreamProvider.writeToStream(serverName, joinMessage.toString());
    }
}