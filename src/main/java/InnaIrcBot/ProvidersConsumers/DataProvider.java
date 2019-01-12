package InnaIrcBot.ProvidersConsumers;

import InnaIrcBot.Config.StorageFile;
import InnaIrcBot.LogDriver.BotDriver;
import InnaIrcBot.ReconnectControl;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class DataProvider implements Runnable {
    private StorageFile configFile;
    private String serverName;
    private String userNick;
    private BufferedReader rawStreamReader;

    private boolean ableToRun = true;
    /**
     * Initiate connection and prepare input/output streams for run()
     * */
    public DataProvider(StorageFile storageFile){
        this.configFile = storageFile;
        this.serverName = storageFile.getServerName();

        int port = storageFile.getServerPort();

        try {
            InetAddress inetAddress = InetAddress.getByName(serverName);
            Socket socket = new Socket();              // TODO: set timeout?
            for (int i=0; i<5; i++) {
                socket.connect(new InetSocketAddress(inetAddress, port), 5000);    // 10sec = 10000
                if (socket.isConnected())
                    break;
            }
            if (socket.isConnected())
                System.out.println("Socket connected");
            else {
                System.out.println("Unable to connect to remote server after 5 retry.");
                this.ableToRun = false;
                return;
            }

            if (!StreamProvider.setStream(serverName, socket)) {
                this.ableToRun = false;
                return;
            }
            InputStream inStream = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(inStream, StandardCharsets.UTF_8);       //TODO set charset in options;
            this.rawStreamReader = new BufferedReader(isr);
        } catch(UnknownHostException e){
            this.ableToRun = false;
            System.out.println("Internal issue: DataProvider->constructor caused unknown host exception:\n\t"+e);   // caused by InetAddress
        } catch (IOException e){
            this.ableToRun = false;
            System.out.println("Internal issue: DataProvider->constructor caused I/O exception\n\t"+e);             // caused by Socket
        }
    }
    //HANDLE ALWAYS in case thread decided to die
    private void close(){
        StreamProvider.delStream(serverName);
    }

    public void run(){
        if (!ableToRun || !this.initConnection(rawStreamReader)
                || !BotDriver.setLogDriver(serverName, configFile.getLogDriver(), configFile.getLogDriverParameters())) {   //Prepare logDriver for using in threads.
            this.close();
            return;
        }

        /* Used for sending data into consumers objects*/
        Map<String, PrintWriter> channelsMap = Collections.synchronizedMap(new HashMap<String, PrintWriter>());
        try {
            PipedOutputStream streamOut = new PipedOutputStream();              // K-K-K-KOMBO \m/

            BufferedReader streamBufferedReader = new BufferedReader(
                    new InputStreamReader(
                            new PipedInputStream(streamOut), StandardCharsets.UTF_8)
            );

            Runnable systemConsumer = new SystemConsumer(streamBufferedReader, userNick, channelsMap, this.configFile);
            new Thread(systemConsumer).start();
            PrintWriter systemConsumerWriter = new PrintWriter(streamOut);

            StreamProvider.setSysConsumer(serverName, systemConsumerWriter);    // Register system consumer at StreamProvider

            channelsMap.put("", systemConsumerWriter);        // Not sure that PrintWriter is thread-safe..
        } catch (IOException e){
            System.out.println("Internal issue: DataProvider->run() I/O exception while initialized child objects.\n\t"+e);             // caused by Socket
            this.close();
            return;
        }
        ////////////////////////////////////// Start loop //////////////////////////////////////////////////////////////

        try {
            String rawMessage;
            String[] rawStrings;    // prefix[0] command[1] command-parameters\r\n[2]
            //if there is no prefix, you should assume the message came from your client.
            String chan;
            // Say 'yes, we need reconnect if connection somehow died'
            ReconnectControl.register(serverName);

            while ((rawMessage = rawStreamReader.readLine()) != null) {
                //System.out.println(rawMessage);
                if (rawMessage.startsWith(":")) {
                    rawStrings = rawMessage
                            .substring(1)
                            .split(" :?", 3);                        // Removing ':'


                    chan = rawStrings[2].replaceAll("(\\s.?$)|(\\s.+?$)", "");

                    //System.out.println("\tChannel: "+chan+"\n\tAction: "+rawStrings[1]+"\n\tSender: "+rawStrings[0]+"\n\tMessage: "+rawStrings[2]+"\n");

                    if (rawStrings[1].equals("QUIT") || rawStrings[1].equals("NICK")) { // replace regex
                        for (PrintWriter value : channelsMap.values()) {
                            value.println(rawStrings[1] + " " + rawStrings[0] + " " + rawStrings[2]);
                            value.flush();
                        }
                    } else if (channelsMap.containsKey(chan)) {
                        channelsMap.get(chan).println(rawStrings[1] + " " + rawStrings[0] + " " + rawStrings[2]);
                        channelsMap.get(chan).flush();
                    } else {
                        channelsMap.get("").println(rawStrings[1] + " " + rawStrings[0] + " " + rawStrings[2]);
                        channelsMap.get("").flush();
                    }
                } else if (rawMessage.startsWith("PING :")) {
                    pingSrvResponse(rawMessage);
                } else {
                    System.out.println("Not a valid response=" + rawMessage);
                }
            }
        } catch (IOException e){
            System.out.println("Socket issue: I/O exception");                  //Connection closed. TODO: MAYBE try reconnect
        }
        finally {
            for (PrintWriter p :channelsMap.values()) {
                p.close();
            }
            this.close();
        }
    }

    private void pingSrvResponse(String rawData){
        StreamProvider.writeToStream(serverName,"PONG :" + rawData.replace("PING :", ""));
    }
    /**
     * Initiate connection before starting main routine.
     * */
    private boolean initConnection(BufferedReader genericStreamReader){
        int nickTail = 0;   // handle appendix to nickname

        this.userNick = configFile.getUserNick();

        if (this.userNick == null || this.userNick.isEmpty()) {
            System.out.println("Configuration issue: no nickname specified.");
            return false;
        }
        String rawMessage;

        StreamProvider.writeToStream(serverName,"NICK "+this.userNick);
        StreamProvider.writeToStream(serverName,"USER "+configFile.getUserIdent()+" 8 * :"+configFile.getUserRealName());       // TODO: Add usermode 4 rusnet

        try {
            // 431  ERR_NONICKNAMEGIVEN     how can we get this?
            // 432  ERR_ERRONEUSNICKNAME    covered
            // 433  ERR_NICKNAMEINUSE       covered
            // 436  ERR_NICKCOLLISION
            // 464  ERR_PASSWDMISMATCH      (password for server/znc/bnc)
            while ((rawMessage = genericStreamReader.readLine()) != null){
                System.out.println(rawMessage);
                if (rawMessage.startsWith("PING :")) {
                    pingSrvResponse(rawMessage);
                }
                if (rawMessage.contains(" 001 ")) {

                    StringBuilder message = new StringBuilder();

                    if (!configFile.getUserMode().trim().isEmpty()){
                        String modes = configFile.getUserMode();
                        modes = modes.replaceAll("[\t\\s]", "");

                        for(char c :modes.toCharArray()) {
                            message.append("MODE ");
                            message.append(userNick);
                            message.append(" +");
                            message.append(c);
                            message.append("\n");
                        }
                    }
                    StreamProvider.writeToStream(serverName,message.toString());

                    return true;
                }
                else if (rawMessage.contains(" 433 ")) {
                    if (this.userNick.substring(userNick.length()-1, userNick.length()-1).equals("|"))
                        this.userNick = this.userNick.substring(0,userNick.length()-1)+Integer.toString(nickTail++);
                    else
                        this.userNick = this.userNick+"|"+Integer.toString(nickTail++);

                    StreamProvider.writeToStream(serverName,"NICK "+this.userNick);
                }
                else if (rawMessage.contains(" 432 ")) {
                    System.out.println("Configuration issue: Nickname contains unacceptable characters (432 ERR_ERRONEUSNICKNAME).");
                    return false;
                }
                else if (rawMessage.contains(" 464 ")) {
                    StreamProvider.writeToStream(serverName,"PASS "+configFile.getServerPass());
                }
            }

            if (!configFile.getUserNickPass().isEmpty() && (!configFile.getUserNickAuthStyle().isEmpty() && configFile.getUserNickAuthStyle().toLowerCase().equals("freenode")))
                StreamProvider.writeToStream(serverName,"PRIVMSG NickServ :IDENTIFY "+configFile.getUserNickPass());
            else if (!configFile.getUserNickPass().isEmpty() && (!configFile.getUserNickAuthStyle().isEmpty() && configFile.getUserNickAuthStyle().toLowerCase().equals("rusnet")))
                StreamProvider.writeToStream(serverName,"NickServ IDENTIFY "+configFile.getUserNickPass());
            else if (!configFile.getUserNickPass().isEmpty())
                System.out.println("Configuration issue: Unable to determinate method of user nick identification (by password): could be \"freenode\" or \"rusnet\"\nSee \"userNickAuthStyle\".");

        } catch (IOException e){
            System.out.println("Internal issue: DataProvider->initConnection() caused I/O exception.");
            return false;
        }
        return false;
    }
}
