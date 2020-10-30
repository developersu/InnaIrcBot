package InnaIrcBot.Commanders;

import InnaIrcBot.Commanders.flood.EventHandler;
import InnaIrcBot.Commanders.flood.JoinCloneHandler;
import InnaIrcBot.Commanders.flood.JoinFloodHandler;
import InnaIrcBot.Commanders.talk.TalkGenericHandler;
import InnaIrcBot.Commanders.talk.TalkHandler;
import InnaIrcBot.Commanders.talk.TalkZeroHandler;
import InnaIrcBot.config.ConfigurationChannel;
import InnaIrcBot.config.ConfigurationManager;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ChanelCommander implements Runnable {
    private final BlockingQueue<String> commanderQueue;
    private final String server;
    private final String channel;
    //TODO: add timers, flood
    private TalkHandler talkHandler;
    private final List<EventHandler> eventHandlers;

    public ChanelCommander(BlockingQueue<String> commanderQueue, String server, String channel) throws Exception{
        this.commanderQueue = commanderQueue;
        this.server = server;
        this.channel = channel;
        this.eventHandlers = new ArrayList<>();
        readConfig();
    }
    private void readConfig() throws Exception{
        ConfigurationChannel configChannel = ConfigurationManager.getConfiguration(server).getChannelConfig(channel);

        if (configChannel == null){
            talkHandler = new TalkZeroHandler();
            return;
        }

        talkHandler = new TalkGenericHandler(
                server, channel,
                configChannel.getJoinMap(),
                configChannel.getMsgMap(),
                configChannel.getNickMap()
        );

        if (configChannel.isJoinFloodControl()) {
            JoinFloodHandler jfh = new JoinFloodHandler(
                    server, channel,
                    configChannel.getJoinFloodControlEvents(),
                    configChannel.getJoinFloodControlTimeframe());
            eventHandlers.add(jfh);
        }

        if (configChannel.isJoinCloneControl()) {
            JoinCloneHandler jch = new JoinCloneHandler(
                    server, channel,
                    configChannel.getJoinCloneControlPattern(),
                    configChannel.getJoinCloneControlTimeframe());
            eventHandlers.add(jch);
        }
    }

    @Override
    public void run() {
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] ChanelCommander thread "
                +server+":"+this.channel +" started");// TODO:REMOVE DEBUG
        runRoutine();
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] ChanelCommander thread "
                +server+":"+this.channel +" ended");// TODO:REMOVE DEBUG
    }

    private void runRoutine(){
        try {
            while (true) {
                parse();
            }
        }
        catch (InterruptedException ie){
            System.out.println("ChanelCommander interrupted.");
        }
    }

    private void parse() throws InterruptedException{
        String data = commanderQueue.take();
        String[] dataStrings = data.split(" :?",3);

        switch (dataStrings[1]) {
            case "NICK":
                talkHandler.nickCame(dataStrings[2]+dataStrings[0].replaceAll("^.+?!","!"));
                break;              // todo: need to track join flood
            case "JOIN":
                for (EventHandler handler : eventHandlers){
                    handler.track(dataStrings[0]);
                }
                talkHandler.joinCame(dataStrings[0]);
                break;
            case "PRIVMSG":
                talkHandler.privmsgCame(dataStrings[0], dataStrings[2]);
                break;
                /*  case "PART":
                    case "QUIT":
                    case "TOPIC":
                    case "MODE":
                    case "KICK":   */
            default:
                break;
        }
    }
}