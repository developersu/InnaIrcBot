package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.Commanders.ChanelCommander;
import InnaIrcBot.GlobalData;
import InnaIrcBot.IrcChannel;
import InnaIrcBot.logging.LogDriver;
import InnaIrcBot.logging.Worker;
import InnaIrcBot.config.ConfigurationManager;
import InnaIrcBot.logging.WorkerZero;

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
    private final ArrayList<String> users;
    private Worker logWorker;
    private String nick;
    private final boolean autoRejoin;
    private final Map<String, IrcChannel> channels;

    private Thread channelCommanderThread;
    private BlockingQueue<String> queue;

    private boolean endThread = false;

    private boolean hasBeenKicked;

    ChanConsumer(String serverName,
                 IrcChannel thisIrcChannel,
                 String ownNick,
                 Map<String, IrcChannel> channels) throws Exception
    {
        this.chanConsumerQueue = thisIrcChannel.getChannelQueue();
        this.serverName = serverName;
        this.channelName = thisIrcChannel.toString();
        this.logWorker = LogDriver.getWorker(serverName, channelName);
        this.users = new ArrayList<>();
        this.nick = ownNick;
        this.autoRejoin = ConfigurationManager.getConfiguration(serverName).getRejoinOnKick();
        this.channels = channels;
        getChanelCommander();
    }
    // Create ChanelCommander
    private void getChanelCommander() throws Exception{
        this.queue = new ArrayBlockingQueue<>(GlobalData.CHANNEL_QUEUE_CAPACITY);
        ChanelCommander commander = new ChanelCommander(queue, serverName, channelName);
        this.channelCommanderThread = new Thread(commander);
        this.channelCommanderThread.start();
    }

    public void run(){
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] ChanConsumer thread "+serverName+":"+this.channelName +" started");                                                   // TODO:REMOVE DEBUG
        try {
            while (! endThread) {
                String data = chanConsumerQueue.take();
                String[] dataStrings = data.split(" :?",3);

                if (trackUsers(dataStrings[1], dataStrings[0], dataStrings[2]))
                    continue;
                // Send to channel commander thread
                // TODO: Check and add consistency validation
                queue.add(data);

                if (! logWorker.logAdd(dataStrings[1], dataStrings[0], dataStrings[2])){      // Write logs checks if LogDriver consistent.
                    this.fixLogDriverIssues(dataStrings[1], dataStrings[0], dataStrings[2]);
                }
            }
        } catch (InterruptedException e){
                System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->run(): Interrupted\n\t"+e.getMessage());           // TODO: reconnect?
        }

        close();
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] THREAD "+serverName+":"+this.channelName +" ended"); // TODO:REMOVE DEBUG
    }

    private boolean trackUsers(String event, String initiator, String subject){
        initiator = simplifyNick(initiator);
        switch (event) {
            case "PRIVMSG":                                                        // most common, we don't have to handle anything else
                return false;
            case "JOIN":
                addUser(initiator);
                return false;
            case "PART":
                deleteUser(initiator);
                return false;
            case "QUIT":
                if (users.contains(initiator)) {
                    deleteUser(initiator);
                    return false;
                }
                return true;       // user quit, but he/she is not in this channel
            case "KICK":
                String kickedUser = subject.replaceAll("(^.+?\\s)|(\\s.+$)", "");
                if (nick.equals(kickedUser) && autoRejoin) {     // TODO: FIX
                    hasBeenKicked = true;
                    deleteUser(kickedUser);
                    return true;
                }
                deleteUser(kickedUser);
                return false;
            case "NICK":
                if (users.contains(initiator)) {
                    swapUsers(initiator, subject);
                    return false;
                }
                return true;       // user changed nick, but he/she is not in this channel
            case "353":
                String userOnChanStr = subject.substring(subject.indexOf(":") + 1);
                userOnChanStr = userOnChanStr.replaceAll("[%@+]", "").trim();
                String[] usersOnChanArr = userOnChanStr.split(" ");
                users.addAll(Arrays.asList(usersOnChanArr));
                return true;
            default:
                return false;
        }
    }

    private void addUser(String user){
        if (! users.contains(user))
            users.add(user);
    }
    private void deleteUser(String user){
        if (user.equals(nick)) {
            endThread = true;
        }
        users.remove(user);
    }
    private void swapUsers(String userNickOld, String userNickNew){
        users.remove(userNickOld);
        users.add(userNickNew);
        if (userNickOld.equals(nick))
            nick = userNickNew;
    }
    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }

    private void close(){
        try{
            channels.remove(channelName);
            logWorker.close();
            channelCommanderThread.interrupt(); //kill sub-thread
            channelCommanderThread.join();
            handleAutoRejoin();
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private void handleAutoRejoin(){
        if (hasBeenKicked && autoRejoin) {
            StreamProvider.writeToStream(serverName, "JOIN " + channelName);
        }
    }

    private void fixLogDriverIssues(String a, String b, String c){
        System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->fixLogDriverIssues(): Some issues detected. Trying to fix...");
        logWorker = LogDriver.getWorker(serverName, channelName);       // Reset logDriver and try using the same one
        if (! logWorker.logAdd(a, b, c)){                               // Write to it what was not written (most likely) and if it's still not consistent:
            logWorker = new WorkerZero();
            System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->fixLogDriverIssues(): failed to use defined LogDriver. Using ZeroWorker instead.");
        }
    }
}
