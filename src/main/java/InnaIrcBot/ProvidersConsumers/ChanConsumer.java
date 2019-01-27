package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.Commanders.ChanelCommander;
import InnaIrcBot.LogDriver.BotDriver;
import InnaIrcBot.LogDriver.Worker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


public class ChanConsumer implements Runnable {
    private BufferedReader reader;
    private String serverName;
    private String channelName;
    private Worker writerWorker;
    private ArrayList<String> userList;
    private String nick;
    private boolean rejoin;
    private Map<String, PrintWriter>  chanList;
    private String configFilePath;

    private PrintWriter chanelCommanderPipe;

    private boolean endThread = false;

    ChanConsumer(BufferedReader streamReader, String serverName, String channelName, String ownNick, String[] usersOnChan, boolean rejoinAlways, Map<String, PrintWriter> map, String configFilePath){
        this.reader = streamReader;
        this.serverName = serverName;
        this.channelName = channelName;
        this.writerWorker = BotDriver.getWorker(serverName, channelName);
        this.userList = new ArrayList<>();
        this.userList.addAll(Arrays.asList(usersOnChan));
        this.nick = ownNick;
        this.rejoin = rejoinAlways;
        this.chanList = map;
        this.configFilePath = configFilePath;
        // Create chanel commander thread, get pipe
        this.chanelCommanderPipe = getChanelCommander();
    }

    public void run(){
        String data;
        String[] dataStrings;
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] THREAD "+serverName+":"+this.channelName +" started");                                                   // TODO:REMOVE DEBUG
        try {
            while ((data = reader.readLine()) != null) {
                dataStrings = data.split(" ",3);

                if (!trackUsers(dataStrings[0], dataStrings[1], dataStrings[2]))
                    continue;

                // Send to chanel commander thread
                chanelCommanderPipe.println(data);
                chanelCommanderPipe.flush();
                //System.out.println("|"+dataStrings[0]+"|"+dataStrings[1]+"|"+dataStrings[2]+"|");
                //System.out.println("Thread: "+this.channelName +"\n\tArray:"+ userList);

                if (!writerWorker.logAdd(dataStrings[0], dataStrings[1], dataStrings[2])){      // Write logs, check if LogDriver consistent. If not:
                    this.fixLogDriverIssues(dataStrings[0], dataStrings[1], dataStrings[2]);
                }

                if (endThread) {
                    reader.close();
                    chanList.get(channelName).close();
                    chanList.remove(channelName);
                    break;
                }
            }
        } catch (java.io.IOException e){
                System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->run(): Internal issue in thread: caused I/O exception:\n\t"+e);           // TODO: reconnect
        }
        writerWorker.close();
        //Chanel commander thread's pipe should be closed
        chanelCommanderPipe.close();
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] THREAD "+serverName+":"+this.channelName +" ended");                                                   // TODO:REMOVE DEBUG
    }

    private boolean trackUsers(String event, String initiatorArg, String subjectArg){
        switch (event) {
            case "PRIVMSG":                                                        // most common, we don't have to handle anything else
                return true;
            case "JOIN":
                addUsers(simplifyNick(initiatorArg));
                return true;
            case "PART":
                delUsers(simplifyNick(initiatorArg)); // nick non-simple
                return true;
            case "QUIT":
                if (userList.contains(simplifyNick(initiatorArg))) {
                    delUsers(simplifyNick(initiatorArg)); // nick non-simple
                    return true;
                } else
                    return false;       // user quit, but he/she is not in this channel
            case "KICK":
                if (rejoin && nick.equals(subjectArg.replaceAll("(^.+?\\s)|(\\s.+$)", "")))     // if it's me and I have rejoin policy 'Auto-Rejoin on kick'.
                    StreamProvider.writeToStream(serverName, "JOIN " + channelName);
                delUsers(subjectArg.replaceAll("(^.+?\\s)|(\\s.+$)", ""));      // nick already simplified
                return true;
            case "NICK":
                if (userList.contains(simplifyNick(initiatorArg))) {
                    swapUsers(simplifyNick(initiatorArg), subjectArg);
                    return true;
                } else {
                    return false;       // user changed nick, but he/she is not in this channel
                }
            default:
                return true;
        }
    }

    private void addUsers(String user){
        if (!userList.contains(user))
            userList.add(user);
    }
    private void delUsers(String user){
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
    // Create ChanelCommander
    private PrintWriter getChanelCommander(){
        PipedOutputStream streamOut = new PipedOutputStream();
        try {
            BufferedReader streamBufferedReader = new BufferedReader(
                    new InputStreamReader(
                            new PipedInputStream(streamOut), StandardCharsets.UTF_8)
            );

            ChanelCommander commander = new ChanelCommander(streamBufferedReader, serverName, channelName, configFilePath);

            new Thread(commander).start();

            return new PrintWriter(streamOut);

        } catch (IOException e) {
            System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->getChanelCommander(): Internal issue: I/O exception while initialized child objects:\n\t"+e); // caused by Socket
            endThread = true;
            return null;
        }
    }
    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }

    private void fixLogDriverIssues(String a, String b, String c){
        System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->fixLogDriverIssues(): Some issues detected. Trying to fix...");
        this.writerWorker = BotDriver.getWorker(serverName, channelName);       // Reset logDriver and try using the same one
        if (!writerWorker.logAdd(a,b,c)){                                       // Write to it what was not written (most likely) and if it's still not consistent:
            this.writerWorker = BotDriver.getZeroWorker();
            System.out.println("ChanConsumer (@"+serverName+"/"+channelName+")->fixLogDriverIssues(): failed to use defined LogDriver. Using ZeroWorker instead.");
        }
    }
}
