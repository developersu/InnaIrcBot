package Temporary;

import InnaIrcBot.LogDriver.BotDriver;
import InnaIrcBot.LogDriver.Worker;

public class DriverTest {

    public static void main(String[] args){
        if (BotDriver.setLogDriver("irc.tomsk.net", "MongoDB", new String[]{"192.168.1.5:27017",
                                                                                                "irc",
                                                                                                "loper",
                                                                                                "password"}))
                System.out.println("Successful driver initiation");
        else {
            System.out.println("Failed driver initiation");
            return;
        }

        Worker fw1 = BotDriver.getWorker("irc.tomsk.net","system");
        Worker fw2 = BotDriver.getWorker("irc.tomsk.net","#main");
        Worker fw3 = BotDriver.getWorker("irc.tomsk.net","#lpr");

        if ((fw1 !=null) && (fw2 !=null) && (fw3 !=null)){
            System.out.println("LogFile1: "+fw1.isConsistent());
            System.out.println("LogFile2: "+fw2.isConsistent());
            System.out.println("LogFile3: "+fw3.isConsistent());

            fw1.logAdd("JOIN", "de_su!loper@desktop.lan", "message1");
            fw1.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": some text here");
            fw1.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": more random tests");
            fw1.logAdd("NICK", "de_su!loper@desktop.lan", "developer_su");
            fw1.logAdd("MODE", "de_su!loper@desktop.lan", "+b username");
            fw1.logAdd("PART", "de_su!loper@desktop.lan", "#chan1");

            fw2.logAdd("JOIN", "de_su!loper@desktop.lan", "message2");
            fw2.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": some text here");
            fw2.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": more random tests");
            fw2.logAdd("NICK", "de_su!loper@desktop.lan", "developer_su");
            fw2.logAdd("MODE", "de_su!loper@desktop.lan", "+b username");
            fw2.logAdd("PART", "de_su!loper@desktop.lan", "#chan2");

            fw3.logAdd("JOIN", "de_su!loper@desktop.lan", "message3");
            fw3.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": some text here");
            fw3.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": more random tests");
            fw3.logAdd("NICK", "de_su!loper@desktop.lan", "developer_su");
            fw3.logAdd("MODE", "de_su!loper@desktop.lan", "+b username");
            fw3.logAdd("PART", "de_su!loper@desktop.lan", "#chan3");

            fw1.close();
            fw2.close();
            fw3.close();
        }
    }
}
