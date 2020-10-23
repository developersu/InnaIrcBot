package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.config.ConfigurationFile;
import InnaIrcBot.IrcChannel;
import InnaIrcBot.logging.LogDriver;
import InnaIrcBot.ReconnectControl;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;


public class DataProvider implements Runnable {
    private final ConfigurationFile configurationFile;
    private final String server;
    private final String nickName;
    private BufferedReader mainReader;

    /**
     * Initiate connection and prepare input/output streams for run()
     * */
    public DataProvider(ConfigurationFile configurationFile){
        this.configurationFile = configurationFile;
        this.server = configurationFile.getServerName();
        this.nickName = configurationFile.getUserNick();
    }

    public void run(){
        try {
            connect();
        } catch (Exception e){
            System.out.println("Internal issue: DataProvider->run() caused exception:\n\t"+e.getMessage());
            e.printStackTrace();

            close();
            return;
        }

        ReconnectControl.register(server);

        LogDriver.setLogDriver(server);

        /* Used for sending data into consumers objects*/
        Map<String, IrcChannel> ircChannels = Collections.synchronizedMap(new HashMap<>());

        IrcChannel systemConsumerChannel = new IrcChannel("");
        BlockingQueue<String> systemConsumerQueue = systemConsumerChannel.getChannelQueue();

        Thread SystemConsumerThread = new Thread(
                new SystemConsumer(systemConsumerQueue, nickName, ircChannels, this.configurationFile));
        SystemConsumerThread.start();

        StreamProvider.setSysConsumer(server, systemConsumerQueue);    // Register system consumer at StreamProvider

        ircChannels.put(systemConsumerChannel.toString(), systemConsumerChannel);        // Not sure that PrintWriter is thread-safe..
        ////////////////////////////////////// Start loop //////////////////////////////////////////////////////////////
        StreamProvider.writeToStream(server,"NICK "+this.nickName);
        StreamProvider.writeToStream(server,"USER "+ configurationFile.getUserIdent()+" 8 * :"+ configurationFile.getUserRealName());       // TODO: Add usermode 4 rusnet

        try {
            String rawMessage;
            String[] rawStrings;    // prefix[0] command[1] command-parameters\r\n[2]
            //if there is no prefix, you should assume the message came from your client.

            while ((rawMessage = mainReader.readLine()) != null) {
                System.out.println(rawMessage);
                if (rawMessage.startsWith(":")) {
                    rawStrings = rawMessage
                            .substring(1)
                            .split(" :?", 3);                        // Removing ':'

                    String chan = rawStrings[2].replaceAll("(\\s.?$)|(\\s.+?$)", "");

                    if (rawStrings[1].equals("QUIT") || rawStrings[1].equals("NICK")) { // replace regex
                        for (IrcChannel ircChannel : ircChannels.values()) {
                            ircChannel.getChannelQueue().add(rawStrings[1] + " " + rawStrings[0] + " " + rawStrings[2]);
                        }
                    }
                    else if (ircChannels.containsKey(chan)) {
                        IrcChannel chnl = ircChannels.get(chan);
                        chnl.getChannelQueue().add(rawStrings[1] + " " + rawStrings[0] + " " + rawStrings[2]);
                    }
                    else {
                        systemConsumerQueue.add(rawStrings[1] + " " + rawStrings[0] + " " + rawStrings[2]);
                    }
                }
                else if (rawMessage.startsWith("PING :")) {
                    sendPingReply(rawMessage);
                }
                else {
                    System.out.println("Not a valid response=" + rawMessage);
                }
            }
        } catch (IOException e){
            System.out.println("Socket issue: I/O exception: "+e.getMessage());                  //Connection closed. TODO: MAYBE try reconnect
        }
        finally {
            SystemConsumerThread.interrupt();
            close();
        }
    }

    private void connect() throws Exception{
        final int port = configurationFile.getServerPort();
        InetAddress inetAddress = InetAddress.getByName(server);
        Socket socket = new Socket();              // TODO: set timeout?
        for (int i = 0; i < 5; i++) {
            socket.connect(new InetSocketAddress(inetAddress, port), 5000); // 5sec
            if (socket.isConnected())
                break;
        }
        if (! socket.isConnected())
            throw new Exception("Unable to connect server.");

        StreamProvider.setStream(server, socket);

        InputStream inStream = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(inStream, StandardCharsets.UTF_8);  //TODO set charset in options;
        mainReader = new BufferedReader(isr);
    }

    private void sendPingReply(String rawData){
        StreamProvider.writeToStream(server,"PONG :" + rawData.replace("PING :", ""));
    }

    private void close(){
        StreamProvider.delStream(server);
        ReconnectControl.notify(server);
    }
}
