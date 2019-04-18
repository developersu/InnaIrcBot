package InnaIrcBot.Commanders;

import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;
                                                            //TODO: FLOOD, JOIN FLOOD
                                                            // TODO: @ configuration level: if in result we have empty string, no need to pass it to server
public class ChanelCommander implements Runnable {
    private BufferedReader reader;
    private String server;
    private String chanel;
                                                            //TODO: add timers
    private HashMap<String, String[]> joinMap;               // Mask(Pattern) ->, Action | Where Action[0] could be: raw
    private HashMap<String, String[]> msgMap;               // Mask(Pattern) ->, Action | Where Action[0] could be: raw
    private HashMap<String, String[]> nickMap;               // Mask(Pattern) ->, Action | Where Action[0] could be: raw

    private boolean joinFloodTrackNeed  = false;
    private JoinFloodHandler jfh;

    private boolean joinCloneTrackNeed  = false;         // todo:fix
    private JoinCloneHandler jch;

    public ChanelCommander(BufferedReader streamReader, String serverName, String chan, String configFilePath){
        this.reader = streamReader;
        this.server = serverName;
        this.chanel = chan;

        this.joinMap = new HashMap<>();
        this.msgMap = new HashMap<>();
        this.nickMap = new HashMap<>();
        readConfing(configFilePath);
    }

    @Override
    public void run() {
        System.out.println("Thread for ChanelCommander started");                                                   // TODO:REMOVE DEBUG
        String data;
        String[] dataStrings;
        try {
            while ((data = reader.readLine()) != null) {
                dataStrings = data.split(" ",3);
                //event initiatorArg messageArg
                switch (dataStrings[0]) {
                    case "NICK":
                        nickCame(dataStrings[2]+dataStrings[1].replaceAll("^.+?!","!"));
                        break;              // todo: need to track join flood
                    case "JOIN":
                        if (joinFloodTrackNeed)
                            jfh.track(simplifyNick(dataStrings[1]));
                        if (joinCloneTrackNeed)
                            jch.track(dataStrings[1]);
                        joinCame(dataStrings[1]);
                        break;
                    case "PRIVMSG":
                        privmsgCame(dataStrings[1], dataStrings[2]);
                        break;
                        /*
                    case "PART":            // todo: need to track join flood? Fuck that. Track using JOIN
                        break;
                    case "QUIT":            // todo: need this?
                        break;
                    case "TOPIC":           // todo: need this?
                        break;
                    case "MODE":            // todo: need this?
                        break;
                    case "KICK":            // todo: need this?
                        break;              */
                    default:
                        break;
                }
            }
        } catch (java.io.IOException e){
            System.out.println("Internal issue: thread ChanelCommander->run() caused I/O exception:\n\t"+e);           // TODO: reconnect
        }
        System.out.println("Thread for ChanelCommander ended");                                                   // TODO:REMOVE DEBUG
    }

    // Do we need old nick?
    private void nickCame(String newNick){
        came(nickMap, newNick, newNick);
    }
    private void joinCame(String who){
        came(joinMap, who, who);
    }
    private void privmsgCame(String who, String what){
        if (what.indexOf(":")+1 < what.length()){
            what = what.substring(what.indexOf(":")+1);
            came(msgMap, what, who);
        }
    }

    private void came(HashMap<String, String[]> map, String arg1, String arg2){
        for (String pattern : map.keySet())
            if (Pattern.matches(pattern, arg1)){       // NOTE: validation based on new nick    //TODO: parse here
                String[] cmdOrMsg = map.get(pattern);

                StringBuilder whatToSendStringBuilder;
                ArrayList<String> whatToSendList;
                for (int i = 0; i<cmdOrMsg.length;) {
                    switch (cmdOrMsg[i]) {
                        case "\\chanmsg":
                            whatToSendList = new ArrayList<>();
                            for (i++; (i < cmdOrMsg.length) && !(cmdOrMsg[i].startsWith("\\")); i++)
                                whatToSendList.add(cmdOrMsg[i]);
                            msgAction(whatToSendList.toArray(new String[0]), arg2, false);
                            break;
                        case "\\privmsg":
                            whatToSendList = new ArrayList<>();
                            for (i++; (i < cmdOrMsg.length) && !(cmdOrMsg[i].startsWith("\\")); i++)
                                whatToSendList.add(cmdOrMsg[i]);
                            msgAction(whatToSendList.toArray(new String[0]), arg2, true);
                            break;
                        case "\\ban":
                            banAction(arg2);
                            i++;
                            break;
                        case "\\kick":
                            whatToSendList = new ArrayList<>();
                            for (i++; (i < cmdOrMsg.length) && !(cmdOrMsg[i].startsWith("\\")); i++)
                                whatToSendList.add(cmdOrMsg[i]);
                            kickAction(whatToSendList.toArray(new String[0]), arg2);
                            break;
                        case "\\kickban":
                            whatToSendList = new ArrayList<>();
                            for (i++; (i < cmdOrMsg.length) && !(cmdOrMsg[i].startsWith("\\")); i++)
                                whatToSendList.add(cmdOrMsg[i]);
                            banAction(arg2);
                            kickAction(whatToSendList.toArray(new String[0]), arg2);
                            break;
                        case "\\raw":
                            whatToSendStringBuilder = new StringBuilder();
                            for (i++; (i < cmdOrMsg.length) && !(cmdOrMsg[i].startsWith("\\")); i++)
                                whatToSendStringBuilder.append(cmdOrMsg[i]);
                            StreamProvider.writeToStream(server, whatToSendStringBuilder.toString()); //TODO
                            break;                                                          //todo: add script
                        case "\\whois":                                                     // result will be noted in 'system' log
                            whoisAction(arg2);
                            i++;
                            break;
                        case "\\cclientinfo":                                             // NOTE: All this handled by CTCPHelper instance
                        case "\\cfinger":                                                 // C - publish request result to chan
                        case "\\cping":
                        case "\\csource":
                        case "\\ctime":
                        case "\\cuserinfo":
                        case "\\cversion":
                        case "\\pclientinfo":                                              // P - reply to privmsg
                        case "\\pfinger":
                        case "\\pping":
                        case "\\psource":
                        case "\\ptime":
                        case "\\puserinfo":
                        case "\\pversion":
                            String CTCPType = cmdOrMsg[i];
                            String objectRegexp = null;
                            whatToSendStringBuilder = new StringBuilder();

                            for (i++; (i < cmdOrMsg.length) && !(cmdOrMsg[i].startsWith("\\")); i++){
                                if (objectRegexp == null && !cmdOrMsg[i].trim().isEmpty())
                                    objectRegexp = cmdOrMsg[i].trim();
                                else
                                    whatToSendStringBuilder.append(cmdOrMsg[i]);
                            }
                            if (objectRegexp != null) {
                                String objectToCtcp = arg1.trim().replaceAll(objectRegexp, "");     // note: trim() ?
                                if (!objectToCtcp.isEmpty()){
                                    if (CTCPType.startsWith("\\c"))
                                        CTCPHelper.getInstance().registerRequest(server, chanel, CTCPType.substring(2).toUpperCase(), objectToCtcp, whatToSendStringBuilder.toString());
                                    else
                                        CTCPHelper.getInstance().registerRequest(server, simplifyNick(arg2), CTCPType.substring(2).toUpperCase(), objectToCtcp, whatToSendStringBuilder.toString());
                                }
                            }

                            break;
                        default:
                            i++;
                    }
                }
            }
    }
    /////////   /////////
    private void whoisAction(String who){                                               // TODO: maybe we have to extend functionality to reuse received information.
        StreamProvider.writeToStream(server, "WHOIS "+simplifyNick(who));
    }

    private void msgAction(String[] messages, String who, boolean sendToPrivate){
        StringBuilder executiveStr = new StringBuilder();
        executiveStr.append("PRIVMSG ");
        if(sendToPrivate) {
            executiveStr.append(simplifyNick(who));
            executiveStr.append(" :");
        }
        else {
            executiveStr.append(chanel);
            executiveStr.append(" :");
            executiveStr.append(simplifyNick(who));
            executiveStr.append(": ");
        }

        for (int i = 0; i < messages.length; i++){
            if ( ! messages[i].startsWith("\\"))
                executiveStr.append(messages[i]);
            else if (messages[i].equals("\\time"))                                    // TODO: remove this shit
                executiveStr.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        //System.out.println(executiveStr.toString());                               //TODO: debug
        StreamProvider.writeToStream(server, executiveStr.toString());
    }
    private void banAction(String whom){
        StreamProvider.writeToStream(server, "MODE "+chanel+" +b "+simplifyNick(whom)+"*!*@*");
        StreamProvider.writeToStream(server, "MODE "+chanel+" +b "+"*!*@"+whom.replaceAll("^.+@",""));
    }
    private void kickAction(String[] messages, String whom){
        StringBuilder executiveStr = new StringBuilder();
        executiveStr.append("KICK ");
        executiveStr.append(chanel);
        executiveStr.append(" ");
        executiveStr.append(simplifyNick(whom));
        executiveStr.append(" :");

        for (int i = 0; i < messages.length; i++){
            if ( ! messages[i].startsWith("\\"))
                executiveStr.append(messages[i]);
            else if (messages[i].equals("\\time"))
                executiveStr.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        StreamProvider.writeToStream(server, executiveStr.toString());

    }
    // TSV
    private void parse(String[] directive){
        if (directive.length >= 3 && directive[0] != null && !directive[0].startsWith("#") && directive[1] != null && directive[2] != null){
        // System.out.println(Arrays.toString(directive));         // TODO:debug
            switch (directive[0].toLowerCase()){
                case "join":
                    joinMap.put(directive[1], Arrays.copyOfRange(directive, 2, directive.length));
                    break;
                case "msg":
                    msgMap.put(directive[1], Arrays.copyOfRange(directive, 2, directive.length));
                    break;
                case "nick":
                    nickMap.put(directive[1], Arrays.copyOfRange(directive, 2, directive.length));
                    break;
                case "joinfloodcontrol":
                    if (!directive[1].isEmpty() && !directive[2].isEmpty() && Pattern.matches("^[0-9]+?$", directive[1].trim()) && Pattern.matches("^[0-9]+?$", directive[2].trim())) {
                        int events = Integer.valueOf(directive[1].trim());
                        int timeFrame = Integer.valueOf(directive[2].trim());
                        if (events > 0 && timeFrame > 0) {
                            jfh = new JoinFloodHandler(events, timeFrame, server, chanel);
                            joinFloodTrackNeed = true;
                        } else {
                            System.out.println("Internal issue: thread ChanelCommander->parse(): 'Number of events' and/or 'Time Frame in seconds' should be greater than 0");
                        }
                    }
                    else
                        System.out.println("Internal issue: thread ChanelCommander->parse(): 'Number of events' and/or 'Time Frame in seconds' should be numbers greater than 0");
                    break;
                case "joinclonecontrol":
                    if (!directive[1].isEmpty() && !directive[2].isEmpty() && Pattern.matches("^[0-9]+?$", directive[1].trim())) {
                        int events = Integer.valueOf(directive[1].trim());
                        if (events > 0){
                            jch = new JoinCloneHandler(directive[2], events, server, chanel);       // TODO: REMOVE
                            joinCloneTrackNeed = true;
                        }
                        else {
                            System.out.println("Internal issue: thread ChanelCommander->parse(): 'Number of events' should be greater than 0");
                        }
                    } else {
                        System.out.println("Internal issue: thread ChanelCommander->parse(): 'Number of events' should be greater than 0 and pattern shouldn't be empty.");
                    }
            }
        }
    }
    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }

    private void readConfing(String confFilesPath){
        if (!confFilesPath.endsWith(File.separator))
            confFilesPath += File.separator;
        File file = new File(confFilesPath+server+chanel+".csv");  // TODO: add/search for filename
        if (!file.exists())
            return;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                parse(line.split("\t"));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Internal issue: thread ChanelCommander->readConfig() can't find file:\t\n"+e);
        }  catch (IOException e){
            System.out.println("Internal issue: thread ChanelCommander->readConfig() I/O exception:\t\n"+e);
        }
    }
}