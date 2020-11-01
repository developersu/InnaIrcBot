package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.Commanders.CTCPHelper;
import InnaIrcBot.Commanders.PrivateMsgCommander;
import InnaIrcBot.ReconnectControl;
import InnaIrcBot.config.ConfigurationFile;
import InnaIrcBot.IrcChannel;
import InnaIrcBot.logging.WorkerSystem;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class SystemConsumer implements Runnable{
    private final BlockingQueue<String> systemQueue;
    private final WorkerSystem writerWorker;
    private String nick;
    private final String server;
    private final Map<String, IrcChannel> channels;
    private final ConfigurationFile configurationFile;

    private final PrivateMsgCommander commander;

    private final ArrayList<Thread> channelThreads;
    private int nickTail = 0;
    private final SystemCTCP systemCTCP;

    private static final HashMap<String, BlockingQueue<String>> systemConsumers = new HashMap<>();
    public static synchronized BlockingQueue<String> getSystemConsumer(String server){
        return systemConsumers.get(server);
    }

    SystemConsumer(String userNick, Map<String, IrcChannel> channels, ConfigurationFile configurationFile) {
        this.systemQueue = channels.get("").getChannelQueue();
        this.nick = userNick;
        this.server = configurationFile.getServerName();
        WorkerSystem.setLogDriver(server);
        this.writerWorker = WorkerSystem.getSystemWorker(server);
        this.channels = channels;
        this.channelThreads = new ArrayList<>();
        this.configurationFile = configurationFile;
        this.commander = new PrivateMsgCommander(server, this.configurationFile.getBotAdministratorPassword());
        this.systemCTCP = new SystemCTCP(server, configurationFile.getCooldownTime(), writerWorker);

        systemConsumers.put(server, systemQueue);
    }

    @Override
    public void run() {
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                +"] Thread SystemConsumer \""+ server +"\": started");      // TODO:REMOVE

        startMainRoutine();
        close();

        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                +"] Thread SystemConsumer \""+ server +"\": ended");       // TODO:REMOVE
    }

    private void startMainRoutine(){
        try {
            while (true) {
                String data = systemQueue.take();
                String[] dataStrings = data.split(" :?",3);
                //TODO: handle mode change
                switch (dataStrings[1]){
                    case "PRIVMSG":
                        if (dataStrings[2].indexOf("\u0001") < dataStrings[2].lastIndexOf("\u0001")) {
                            String sender = simplifyNick(dataStrings[0]);
                            String message = dataStrings[2].substring(dataStrings[2].indexOf(":") + 1);
                            systemCTCP.replyCTCP(sender, message);
                        }
                        else {
                            commander.receiver(dataStrings[0], dataStrings[2].replaceAll("^.+?:", "").trim());
                            writerWorker.log(dataStrings[1]+" "+dataStrings[0]+" :", dataStrings[2].replaceAll("^.+?:", "").trim());
                        }
                        break;
                    case "INNA":
                        String[] splitter;
                        if (dataStrings.length > 2){                                 // Don't touch 'cuz it's important
                            splitter = dataStrings[2].split(" ", 2);
                            if (splitter.length == 2){
                                handleSpecial(dataStrings[0], splitter[0], splitter[1]);
                            }
                        }
                        break;
                    default:
                        handleNumeric(dataStrings[0], dataStrings[1], dataStrings[2]);
                }
            }
        }
        catch (InterruptedException ie){
            System.out.println("Thread SystemConsumer interrupted.");           // TODO: reconnect OR AT LEAST DIE
        }
        catch (Exception e){
            System.out.println("Internal issue: SystemConsumer: "+e.getMessage());           // TODO: DO.. some thing
            e.printStackTrace();
        }
    }

    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }

    private void handleSpecial(String event, String channelName, String message){
        IrcChannel ircChannel = channels.get(channelName);
        if (ircChannel == null)
            return;
        String ircFormatterMessage = event+" "+nick+" "+channelName+" "+message;

        ircChannel.getChannelQueue().add(ircFormatterMessage);
    }
    //todo: handle nickserv messages somehow
    private void handleNumeric(String sender, String eventNum, String message) throws Exception{
        switch (eventNum){
            case "501":                                                             // Notify user about incorrect setup
                writerWorker.log("catch/handled:", eventNum
                        + " [MODE message was sent with a nickname parameter and that the a mode flag sent was not recognized.]");
                break;
            case "433":                                                             // TODO: try to use alternative nickname
                writerWorker.log("catch/handled:", eventNum
                        + " [nickname already in use and will be changed]");
                break;
            case "353":
                writerWorker.log("catch/handled:", eventNum+" [RPL_NAMREPLY]");
                String channelName = message.substring(nick.length()+3).replaceAll("\\s.*$", "");

                IrcChannel ircChannel = channels.get(channelName);
                if (ircChannel == null)
                    return;
                ircChannel.getChannelQueue().add(sender+" "+eventNum+" "+message);
                break;
            case "NICK":
                if (sender.startsWith(nick+"!")) {
                    nick = message.trim();
                    writerWorker.log("catch own NICK change:", sender+" to: "+message);
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
                    writerWorker.log("joined channel ", message);
                }
                break;
            case "401":     // No such nick/channel
                //System.out.println("|"+message.replaceAll("^(\\s)?.+?(\\s)|((\\s)?:No such nick/channel)","")+"|");
                CTCPHelper.getInstance().handleErrorReply(server,  message.replaceAll("^(\\s)?.+?(\\s)|((\\s)?:No such nick/channel)",""));
                writerWorker.log("catch: "+eventNum+" from: "+sender+" :",message+" [ok]");
                break;
            case "NOTICE":
                CTCPHelper.getInstance().handleCtcpReply(server, simplifyNick(sender), message.replaceAll("^.+?:", "").trim());
                writerWorker.log("NOTICE from "+sender+" received: ", message.replaceAll("^.+?:", "").trim());
                break;
            case "001":
                sendUserModes();
                sendNickPassword();
                joinChannels();
                writerWorker.log(eventNum, message);
                break;
            case "443":
                String newNick = nick+"|"+nickTail++;
                StreamProvider.writeToStream(server,"NICK "+newNick);
                break;
            case "464":  // password for server/znc/bnc
                StreamProvider.writeToStream(server,"PASS "+configurationFile.getServerPass());
                writerWorker.log(eventNum, message);
                break;
            case "432":
                writerWorker.log(eventNum, message);
                System.out.println("Configuration issue: Nickname contains unacceptable characters (432 ERR_ERRONEUSNICKNAME).");
            case "465":
                ReconnectControl.update(server, false);
                writerWorker.log(eventNum, message);
                break;
            case "QUIT":  // TODO: Do something?
                writerWorker.log(eventNum, message);
                break;
            case "375":
                writerWorker.log("MOTD Start:", message.replaceAll("^.+?:", ""));
                break;
            case "372":
                writerWorker.log("MOTD:", message.replaceAll("^.+?:", ""));
                break;
            case "376":
                writerWorker.log("MOTD End:", message.replaceAll("^.+?:", ""));
                break;
            default:
                writerWorker.log("catch: "+eventNum+" from: "+sender+" :", message);
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

        StreamProvider.writeToStream(server, message.toString());
    }

    private void sendNickPassword(){
        if (configurationFile.getUserNickPass().isEmpty())
            return;

        switch (configurationFile.getUserNickAuthStyle()){
            case "freenode":
                StreamProvider.writeToStream(server,"PRIVMSG NickServ :IDENTIFY "
                        + configurationFile.getUserNickPass());
                break;
            case "rusnet":
                StreamProvider.writeToStream(server,"NickServ IDENTIFY "
                        + configurationFile.getUserNickPass());
        }
    }

    private void joinChannels(){
        StringBuilder joinMessage = new StringBuilder();

        for (String channel : configurationFile.getChannels()) {       // TODO: add validation of channels.
            joinMessage.append("JOIN ");
            joinMessage.append(channel);
            joinMessage.append("\n");
        }

        StreamProvider.writeToStream(server, joinMessage.toString());
    }

    private void close(){
        for (Thread channel : channelThreads) {   //TODO: check, code duplication. see Data provider constructor
            channel.interrupt();
        }

        writerWorker.close();
        systemConsumers.remove(server);
    }
}