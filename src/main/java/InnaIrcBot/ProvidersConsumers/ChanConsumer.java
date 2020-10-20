package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.Commanders.ChanelCommander;
import InnaIrcBot.GlobalData;
import InnaIrcBot.IrcChannel;
import InnaIrcBot.LogDriver.BotDriver;
import InnaIrcBot.LogDriver.Worker;
import InnaIrcBot.config.ConfigurationManager;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class ChanConsumer implements Runnable {
    private final BlockingQueue<String> chanConsumerQueue;
    private final String serverName;
    private final String channelName;
    private Worker writerWorker;
    private ArrayList<String> userList;
    private String nick;
    private final boolean rejoin;
    private final Map<String, IrcChannel> channels;

    private Thread channelCommanderThread;
    private BlockingQueue<String> queue;

    private boolean endThread = false;

    ChanConsumer(String serverName,
                 IrcChannel thisIrcChannel,
                 String ownNick,
                 Map<String, IrcChannel> channels) throws Exception{
        this.chanConsumerQueue = thisIrcChannel.getChannelQueue();
        this.serverName = serverName;
        this.channelName = thisIrcChannel.toString();
        this.writerWorker = BotDriver.getWorker(serverName, channelName);
        this.userList = new ArrayList<>();
        this.nick = ownNick;
        this.rejoin = ConfigurationManager.getConfiguration(serverName).getRejoinOnKick();
        this.channels = channels;
        // Create chanel commander thread, get pipe
        getChanelCommander(
                ConfigurationManager.getConfiguration(serverName).getChanelConfigurationsPath()
        );
    }
    // Create ChanelCommander
    private void getChanelCommander(String chanelConfigurationsPath){
        this.queue = new ArrayBlockingQueue<>(GlobalData.CHANNEL_QUEUE_CAPACITY);
        ChanelCommander commander = new ChanelCommander(queue, serverName, channelName, chanelConfigurationsPath);
        this.channelCommanderThread = new Thread(commander);
        this.channelCommanderThread.start();
    }

    public void run(){
        String data;
        String[] dataStrings;
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] ChanConsumer thread "+serverName+":"+this.channelName +" started");                                                   // TODO:REMOVE DEBUG
        try {
            while (! endThread) {
                data = chanConsumerQueue.take();
                dataStrings = data.split(" ",3);

                if (! trackUsers(dataStrings[0], dataStrings[1], dataStrings[2]))
                    continue;
                // Send to chanel commander thread
                queue.add(data);                    // TODO: Check and add consistency validation

                if (!writerWorker.logAdd(dataStrings[0], dataStrings[1], dataStrings[2])){      // Write logs, check if LogDriver consistent. If not:
                    this.fixLogDriverIssues(dataStrings[0], dataStrings[1], dataStrings[2]);
                }
            }
            channels.remove(channelName);
        } catch (InterruptedException e){
                System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->run(): Interrupted\n\t"+e);           // TODO: reconnect?
        }
        writerWorker.close();
        //Kill sub-thread
        channelCommanderThread.interrupt();
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] THREAD "+serverName+":"+this.channelName +" ended");                                                   // TODO:REMOVE DEBUG
    }

    private boolean trackUsers(String event, String initiatorArg, String subjectArg){
        switch (event) {
            case "PRIVMSG":                                                        // most common, we don't have to handle anything else
                return true;
            case "JOIN":
                addUser(simplifyNick(initiatorArg));
                return true;
            case "PART":
                deleteUser(simplifyNick(initiatorArg)); // nick non-simple
                return true;
            case "QUIT":
                if (userList.contains(simplifyNick(initiatorArg))) {
                    deleteUser(simplifyNick(initiatorArg)); // nick non-simple
                    return true;
                }
                else
                    return false;       // user quit, but he/she is not in this channel
            case "KICK":
                if (rejoin && nick.equals(subjectArg.replaceAll("(^.+?\\s)|(\\s.+$)", "")))     // if it's me and I have rejoin policy 'Auto-Rejoin on kick'.
                    StreamProvider.writeToStream(serverName, "JOIN " + channelName);
                deleteUser(subjectArg.replaceAll("(^.+?\\s)|(\\s.+$)", ""));      // nick already simplified
                return true;
            case "NICK":
                if (userList.contains(simplifyNick(initiatorArg))) {
                    swapUsers(simplifyNick(initiatorArg), subjectArg);
                    return true;
                }
                else {
                    return false;       // user changed nick, but he/she is not in this channel
                }
            case "353":
                String userOnChanStr = subjectArg.substring(subjectArg.indexOf(":") + 1);
                userOnChanStr = userOnChanStr.replaceAll("[%@+]", "").trim();
                String[] usersOnChanArr = userOnChanStr.split(" ");
                userList.addAll(Arrays.asList(usersOnChanArr));
                return true;
            default:
                return true;
        }
    }

    private void addUser(String user){
        if (!userList.contains(user))
            userList.add(user);
    }
    private void deleteUser(String user){
        if (user.equals(nick)) {
            endThread = true;
        }
        userList.remove(user);
    }
    private void swapUsers(String userNickOld, String userNickNew){
        userList.remove(userNickOld);
        userList.add(userNickNew);
        if (userNickOld.equals(nick))
            this.nick = userNickNew;
    }
    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }

    private void fixLogDriverIssues(String a, String b, String c){
        System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->fixLogDriverIssues(): Some issues detected. Trying to fix...");
        this.writerWorker = BotDriver.getWorker(serverName, channelName);       // Reset logDriver and try using the same one
        if (! writerWorker.logAdd(a, b, c)){                                       // Write to it what was not written (most likely) and if it's still not consistent:
            this.writerWorker = BotDriver.getZeroWorker();
            System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->fixLogDriverIssues(): failed to use defined LogDriver. Using ZeroWorker instead.");
        }
    }
}
