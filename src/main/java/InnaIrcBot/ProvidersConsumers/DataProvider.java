package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.config.ConfigurationFile;
import InnaIrcBot.IrcChannel;
import InnaIrcBot.ReconnectControl;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class DataProvider implements Runnable {
    private final ConfigurationFile configuration;
    private final String server;
    private final String nick;
    private BufferedReader mainReader;

    private Thread systemConsumerThread;
    private Map<String, IrcChannel> ircChannels;
    private IrcChannel systemConsumerChannel;

    /**
     * Initiate connection and prepare input/output streams for run()
     * */
    public DataProvider(ConfigurationFile configuration){
        this.configuration = configuration;
        this.server = configuration.getServerName();
        this.nick = configuration.getUserNick();
    }

    public void run(){
        try {
            connectSocket();

            ReconnectControl.register(server);

            ircChannels = Collections.synchronizedMap(new HashMap<>());
            systemConsumerChannel = new IrcChannel("");
            ircChannels.put(systemConsumerChannel.toString(), systemConsumerChannel);

            startSystemConsumer();
            sendUserNickAndIdent();

            startLoop();
            System.out.println("ENDED");
        } catch (Exception e){
            System.out.println("DataProvider exception: "+e.getMessage());
        }
        close();
    }

    private void connectSocket() throws Exception{
        final int port = configuration.getServerPort();
        InetAddress inetAddress = InetAddress.getByName(server);
        Socket socket = new Socket();
        for (int i = 0; i < 5; i++) {
            socket.connect(new InetSocketAddress(inetAddress, port), 5000); // 5sec
            if (socket.isConnected())
                break;
        }
        if (! socket.isConnected())
            throw new Exception("Unable to connect server.");

        StreamProvider.setStream(server, socket);
        //TODO set charset in options;
        mainReader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );
    }
    private void sendUserNickAndIdent(){
        StreamProvider.writeToStream(server,"NICK " + nick);
        StreamProvider.writeToStream(server,"USER " + configuration.getUserIdent()+" 8 * :"+ configuration.getUserRealName()); // TODO: Add usermode 4 rusnet
    }
    private void startSystemConsumer(){
        systemConsumerThread = new Thread(
                new SystemConsumer(nick, ircChannels, configuration));
        systemConsumerThread.start();
    }

    private void startLoop() throws Exception{
        String rawMessage;
        while ((rawMessage = mainReader.readLine()) != null) {
            System.out.println(rawMessage);
            if (rawMessage.startsWith(":")) {
                handleRegular(rawMessage.substring(1));
            }
            else if (rawMessage.startsWith("PING :")) {
                sendPingReply(rawMessage);
            }
            else {
                System.out.println(rawMessage);
            }
        }
    }
    private void handleRegular(String rawMessage){
        //System.out.println(rawMessage);
        String[] rawStrings = rawMessage.split(" :?", 3);

        if (rawStrings[1].equals("QUIT") || rawStrings[1].equals("NICK")) {
            for (IrcChannel ircChannel : ircChannels.values()) {
                ircChannel.getChannelQueue().add(rawMessage);
            }
            return;
        }

        String channel = rawStrings[2].replaceAll("(\\s.?$)|(\\s.+?$)", "");

        IrcChannel ircChannel = ircChannels.getOrDefault(channel, systemConsumerChannel);
        ircChannel.getChannelQueue().add(rawMessage);
    }

    private void sendPingReply(String message){
        StreamProvider.writeToStream(server, message.replaceFirst("PING", "PONG"));
    }

    private void close(){
        try {
            if (systemConsumerThread != null) {
                systemConsumerThread.interrupt();
                systemConsumerThread.join();
            }
            StreamProvider.delStream(server);
            ReconnectControl.notify(server);
        }
        catch (InterruptedException ignored){}
    }
}
