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

                writerWorker.logAdd(dataStrings[0], dataStrings[1], dataStrings[2]);
                // Send to chanel commander thread
                chanelCommanderPipe.println(data);
                chanelCommanderPipe.flush();
                //System.out.println("|"+dataStrings[0]+"|"+dataStrings[1]+"|"+dataStrings[2]+"|");
                //System.out.println("Thread: "+this.channelName +"\n\tArray:"+ userList);

                if (endThread) {
                    reader.close();
                    chanList.get(channelName).close();
                    chanList.remove(channelName);
                    break;
                }
            }
        } catch (java.io.IOException e){
                System.out.println("Internal issue: thread ChanConsumer->run() caused I/O exception:\n\t"+e);           // TODO: reconnect
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
                addUsers(initiatorArg);
                return true;
            case "PART":
                delUsers(initiatorArg);
                return true;
            case "QUIT":                                               // TODO fix: use regex
                if (userList.contains(initiatorArg.replaceAll("!.+$", ""))) {
                    delUsers(initiatorArg);
                    return true;
                } else
                    return false;       // user quit, but he/she is not in this channel
            case "KICK":
                if (rejoin && nick.equals(subjectArg.substring(subjectArg.indexOf(" ") + 1, subjectArg.indexOf(" :"))))
                    StreamProvider.writeToStream(serverName, "JOIN " + channelName);
                delUsers(subjectArg.substring(subjectArg.indexOf(" ") + 1, subjectArg.indexOf(" :")));
                return true;
            case "NICK":
                if (userList.contains(initiatorArg.replaceAll("!.+$", ""))) {
                    swapUsers(initiatorArg, subjectArg);
                    return true;
                } else {
                    return false;       // user changed nick, but he/she is not in this channel
                }
            default:
                return true;
        }
    }

    private void addUsers(String user){
        if (!userList.contains(user.replaceAll("!.+$", "")))
            userList.add(user.replaceAll("!.+$", ""));
    }
    private void delUsers(String user){
        if (user.replaceAll("!.+$", "").equals(nick)) {
            endThread = true;
        }
        userList.remove(user.replaceAll("!.+$", ""));
    }
    private void swapUsers(String userNickOld, String userNickNew){
        userList.remove(userNickOld.replaceAll("!.+$", ""));
        userList.add(userNickNew);
        if (userNickOld.replaceAll("!.+$", "").equals(nick))
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
            System.out.println("Internal issue: ChanConsumer->getChanelCommander() I/O exception while initialized child objects.");             // caused by Socket
            endThread = true;
            return null;
        }
    }
}
