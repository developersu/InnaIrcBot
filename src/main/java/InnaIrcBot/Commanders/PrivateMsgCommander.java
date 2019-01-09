package InnaIrcBot.Commanders;

import InnaIrcBot.GlobalData;
import InnaIrcBot.ProvidersConsumers.StreamProvider;
import InnaIrcBot.ReconnectControl;

import java.util.ArrayList;

public class PrivateMsgCommander {                                          // TODO: add black list: add users after failed login queries ; temporary ban
    private String serverName;
    private ArrayList<String> administrators;
    private String password;

    public PrivateMsgCommander(String server, String adminPassword){
        this.serverName = server;
        this.administrators = new ArrayList<>();
        this.password = adminPassword.trim();
    }

    public void receiver(String sender, String message){
        if (!password.isEmpty()) {
            if (administrators.contains(sender) && !message.isEmpty()) {
                String[] cmd = message.split("(\\s)|(\t)+?", 2);
                cmd[0] = cmd[0].toLowerCase();

                switch (cmd[0]){
                    case "tell":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] tellArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            tell(tellArgs[0], tellArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: tell <nick> <message>");
                        break;
                    case "join":
                        if (cmd.length == 2)
                            join(cmd[1]);
                        else
                            tell(simplifyNick(sender), "Pattern: join <channel>");
                        break;
                    case "quit":
                        if (cmd.length == 2)
                            quit(cmd[1]);
                        else
                            quit("");
                        break;
                    case "nick":
                        if (cmd.length == 2)
                            nick(cmd[1]);
                        else
                            tell(simplifyNick(sender), "Pattern: nick <new_Nick>");
                        break;
                    case "part":                                                                                //TODO: update
                        if (cmd.length == 2)
                            part(cmd[1]);
                        else
                            tell(simplifyNick(sender), "Pattern: part <channel> [reason]");
                        break;
                    case "ctcp":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] ctcpArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            ctcp(ctcpArgs[0], ctcpArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: ctcp <object> <CTCP-command>");
                        break;
                    case "notice":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] noticeArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            notice(noticeArgs[0], noticeArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: notice <nick> <message>");
                        break;
                    case "umode":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] modeArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            umode(modeArgs[0], modeArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: umode <object> <[+|-]mode_single_char>");
                        break;
                    case "raw":
                        if (cmd.length == 2)
                            raw(cmd[1]);
                        else
                            tell(simplifyNick(sender), "Pattern: raw <any_text_to_server>");
                        break;
                    case "cmode":
                        if ((cmd.length >= 2) && (cmd[1].split("(\\s)|(\t)+?",3).length >= 2)) {
                            String[] args = cmd[1].split("(\\s)|(\t)+?", 3);
                            if (args.length == 2)
                                cmode(args[0], args[1], null);
                            else if(args.length == 3)
                                cmode(args[0], args[1], args[2]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: cmode <channel> [<mode>|<mode> <pattern_user>]");

                        break;
                    case "k":
                    case "kick":
                        if ((cmd.length >= 2) && (cmd[1].split("(\\s)|(\t)+?",3).length >= 2)) {
                            String[] args = cmd[1].split("(\\s)|(\t)+?", 3);
                            if (args.length == 2)
                                kick(args[0], args[1], null);
                            else if(args.length == 3)
                                kick(args[0], args[1], args[2]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [k|kick] <channel> <user> [reason]");
                        break;
                    case "b":
                    case "ban":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] banArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            ban(banArgs[0], banArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [b|ban] <channel> <user>");
                        break;
                    case "-b":
                    case "unban":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] banArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            unban(banArgs[0], banArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [-b|unban] <channel> <user>");
                        break;
                    case "kb":
                    case "kickban":
                        if ((cmd.length >= 2) && (cmd[1].split("(\\s)|(\t)+?",3).length >= 2)) {
                            String[] args = cmd[1].split("(\\s)|(\t)+?", 3);
                            if (args.length == 2)
                                kickban(args[0], args[1], null);
                            else if(args.length == 3)
                                kickban(args[0], args[1], args[2]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [kb|kickban] <channel> <user>");
                        break;
                    case "v":
                    case "voice":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] voiceArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            voice(voiceArgs[0], voiceArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [v|voice] <channel> <user>");
                        break;
                    case "-v":
                    case "unvoice":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] voiceArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            unvoice(voiceArgs[0], voiceArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [-v|unvoice] <channel> <user>");
                        break;
                    case "h":
                    case "hop":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] hopArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            hop(hopArgs[0], hopArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [h|hop] <channel> <user>");
                        break;
                    case "-h":
                    case "unhop":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] hopArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            unhop(hopArgs[0], hopArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [-h|unhop] <channel> <user>");
                        break;
                    case "o":
                    case "op":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] operatorArgs = cmd[1].split("(\\s)|(\t)+?", 2);
                            op(operatorArgs[0], operatorArgs[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [o|operator] <channel> <user>");
                        break;
                    case "-o":
                    case "unop":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] args = cmd[1].split("(\\s)|(\t)+?", 2);
                            unop(args[0], args[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: [-o|unoperator] <channel> <user>");
                        break;
                    case "topic":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] args = cmd[1].split("(\\s)|(\t)+?", 2);
                            topic(args[0], args[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: topic <channel> <topic>");
                    break;
                    case "invite":
                        if ((cmd.length == 2) && (cmd[1].split("(\\s)|(\t)+?",2).length == 2)) {
                            String[] args = cmd[1].split("(\\s)|(\t)+?", 2);
                            invite(args[0], args[1]);
                        }
                        else
                            tell(simplifyNick(sender), "Pattern: invite <user> <channel>");
                        break;
                    case "login":
                        tell(simplifyNick(sender), "Already logged in.");
                        break;
                    default:
                        tell(simplifyNick(sender), "Unknown command. Could be: join, part, quit, tell, nick, ctcp, notice, umode, cmode, raw, kick[k], ban[b], unban[-b], kickban[kb], voice[v], unvoice[-v], hop[h], unhop[-h], op[o], unop[-o], topic, invite and (login)");
                }       // TODO: chanel limits set/remove
            } else {
                if (!message.isEmpty() && message.startsWith("login ")) {
                    login(sender, message.replaceAll("^([\t\\s]+)?login([\t\\s]+)|([\t\\s]+$)", ""));
                }
            }
        }
    }

    private void join(String channel){
        raw("JOIN "+channel);
    }
    private void part(String channel){
        raw("PART "+channel);
    }
    private void quit(String message){
        if (message.isEmpty()){
            raw("QUIT :"+ GlobalData.getAppVersion());
        }
        else
            raw("QUIT :"+message);
        ReconnectControl.update(serverName, false);
        //ReconnectControl.update(serverName, true);
        //System.exit(0);                                                             // TODO: change to normal exit
    }
    private void tell(String channelUser, String message){
        message = message.trim();
        if (message.startsWith("/me ")){
            message = "\u0001ACTION "+message.substring(3)+"\u0001";
        }
        raw("PRIVMSG "+channelUser+" :"+message);
    }
    private void nick(String newNick){
        raw("NICK "+newNick);
    }
    private void ctcp(String object, String command){
        raw("PRIVMSG "+object+" :\u0001"+command.toUpperCase()+"\u0001");
    }
    private void notice(String channelUser, String message){
        raw("NOTICE "+channelUser+" :"+message);
    }
    private void umode(String object, String mode){
        raw("MODE "+object+" "+mode);
    }
    private void cmode(String object, String mode, String user){
        if (user == null)
            raw("MODE "+object+" "+mode);
        else
            raw("MODE "+object+" "+mode+" "+user);
    }
    private void raw(String rawText){
        StreamProvider.writeToStream(serverName, rawText);
    }
    private void kick(String chanel, String user, String reason){
        if (reason == null)
            raw("KICK "+chanel+" "+simplifyNick(user)+" :requested");
        else
            raw("KICK "+chanel+" "+simplifyNick(user)+" :"+reason);
    }
    private void ban(String chanel, String user){
        cmode(chanel, "+b", simplifyNick(user)+"*!*@*");            // TODO: work on patter.n
        if (user.contains("@")){
            cmode(chanel, "+b", "*!*@"+user.replaceAll("^.+@",""));
        }
    }
    private void unban(String chanel, String user){
        cmode(chanel, "-b", simplifyNick(user)+"*!*@*");
        if (user.contains("@")){
            cmode(chanel, "-b", "*!*@"+user.replaceAll("^.+@",""));
        }
    }
    private void kickban(String chanel, String user, String reason){
        cmode(chanel, "+b", simplifyNick(user)+"*!*@*");
        kick(chanel, user, reason);
    }
    private void voice(String chanel, String user){
        cmode(chanel, "+v", user);            
    }
    private void unvoice(String chanel, String user){
        cmode(chanel, "-v", user);            
    }
    private void hop(String chanel, String user){
        cmode(chanel, "+h", user);
    }
    private void unhop(String chanel, String user){
        cmode(chanel, "-h", user);
    }
    private void op(String chanel, String user){
        cmode(chanel, "+o", user);
    }
    private void unop(String chanel, String user){
        cmode(chanel, "-o", user);
    }
    private void topic(String channel, String topic){ raw("TOPIC "+channel+" :"+topic); }
    private void invite(String user, String chanel){
        raw("INVITE "+user+" "+chanel);
    }

    private void login(String candidate, String key){
        if (key.equals(password)) {
            administrators.add(candidate);
            tell(simplifyNick(candidate), "Granted.");
        }
    }
    private String simplifyNick(String nick){ return nick.replaceAll("!.*$",""); }
}