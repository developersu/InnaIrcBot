package InnaIrcBot.Commanders.talk;

import InnaIrcBot.Commanders.CTCPHelper;
import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class TalkGenericHandler implements TalkHandler{
    private final String server;
    private final String channel;
    private final HashMap<String, String[]> joinMap;               // Mask(Pattern) ->, Action | Where Action[0] could be: raw
    private final HashMap<String, String[]> msgMap;               // Mask(Pattern) ->, Action | Where Action[0] could be: raw
    private final HashMap<String, String[]> nickMap;               // Mask(Pattern) ->, Action | Where Action[0] could be: raw

    public TalkGenericHandler(String server, String channel,
                              HashMap<String, String[]> joinMap,
                              HashMap<String, String[]> msgMap,
                              HashMap<String, String[]> nickMap)
    {
        this.server = server;
        this.channel = channel;
        this.joinMap = joinMap;
        this.msgMap = msgMap;
        this.nickMap = nickMap;
    }
    @Override
    public void nickCame(String newNick){
        came(nickMap, newNick, newNick);
    }
    @Override
    public void joinCame(String who){
        came(joinMap, who, who);
    }
    @Override
    public void privmsgCame(String who, String what){
        if (what.indexOf(":")+1 < what.length()){
            what = what.substring(what.indexOf(":")+1);
            came(msgMap, what, who);
        }
    }

    private void came(HashMap<String, String[]> map, String arg1, String arg2) {
        for (String pattern : map.keySet()){

            if (! Pattern.matches(pattern, arg1))       // NOTE: validation based on new nick    //TODO: parse here
                continue;

            String[] cmdOrMsg = map.get(pattern);

            ArrayList<String> whatToSendList;
            for (int i = 0; i < cmdOrMsg.length; ) {
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
                    case "\\voice":
                        voiceAction(arg2);
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
                        StringBuilder whatToSend = new StringBuilder();
                        for (i++; (i < cmdOrMsg.length) && !(cmdOrMsg[i].startsWith("\\")); i++)
                            whatToSend.append(cmdOrMsg[i]);
                        StreamProvider.writeToStream(server, whatToSend.toString()); //TODO
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
                        whatToSend = new StringBuilder();

                        for (i++; (i < cmdOrMsg.length) && !(cmdOrMsg[i].startsWith("\\")); i++) {
                            if (objectRegexp == null && !cmdOrMsg[i].trim().isEmpty())
                                objectRegexp = cmdOrMsg[i].trim();
                            else
                                whatToSend.append(cmdOrMsg[i]);
                        }

                        if (objectRegexp == null)
                            break;

                        String objectToCtcp = arg1.trim().replaceAll(objectRegexp, ""); // note: trim() ?

                        if (objectToCtcp.isEmpty())
                            break;

                        if (CTCPType.startsWith("\\c"))
                            registerCTCPforChannel(CTCPType.substring(2).toUpperCase(), objectToCtcp, whatToSend.toString());
                        else
                            registerCTCPforUser(simplifyNick(arg2), CTCPType.substring(2).toUpperCase(), objectToCtcp, whatToSend.toString());

                        break;
                    default:
                        i++;
                }
            }
        }
    }
    private void registerCTCPforChannel(String CTCPType, String object, String message){
        CTCPHelper.getInstance().registerRequest(server, channel, CTCPType, object, message);
    }
    private void registerCTCPforUser(String user, String CTCPType, String object, String message){
        CTCPHelper.getInstance().registerRequest(server, user, CTCPType, object, message);
    }
    private void whoisAction(String who){                                               // TODO: maybe we have to extend functionality to reuse received information.
        StreamProvider.writeToStream(server, "WHOIS "+simplifyNick(who));
    }

    private void msgAction(String[] messages, String who, boolean isToUser){
        StringBuilder executiveStr = new StringBuilder();
        executiveStr.append("PRIVMSG ");
        if(isToUser) {
            executiveStr.append(simplifyNick(who));
            executiveStr.append(" :");
        }
        else {
            executiveStr.append(channel);
            executiveStr.append(" :");
            executiveStr.append(simplifyNick(who));
            executiveStr.append(": ");
        }

        for (String message : messages) {
            if (!message.startsWith("\\"))
                executiveStr.append(message);
            else if (message.equals("\\time"))                                    // TODO: remove this shit
                executiveStr.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        //System.out.println(executiveStr.toString());                               //TODO: debug
        StreamProvider.writeToStream(server, executiveStr.toString());
    }
    private void banAction(String whom){
        StreamProvider.writeToStream(server, "MODE "+ channel +" +b "+simplifyNick(whom)+"*!*@*");
        StreamProvider.writeToStream(server, "MODE "+ channel +" +b "+"*!*@"+whom.replaceAll("^.+@",""));
    }
    private void voiceAction(String whom){
        StreamProvider.writeToStream(server, "MODE "+ channel +" +v "+simplifyNick(whom));
    }
    private void kickAction(String[] messages, String whom){
        StringBuilder executiveStr = new StringBuilder();
        executiveStr.append("KICK ");
        executiveStr.append(channel);
        executiveStr.append(" ");
        executiveStr.append(simplifyNick(whom));
        executiveStr.append(" :");

        for (String message : messages) {
            if (!message.startsWith("\\"))
                executiveStr.append(message);
            else if (message.equals("\\time"))
                executiveStr.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        StreamProvider.writeToStream(server, executiveStr.toString());
    }
    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }
}
