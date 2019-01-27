package Temporary;

import InnaIrcBot.LogDriver.BotDriver;
import InnaIrcBot.LogDriver.Worker;

public class DriverTestFiles {

    public static void main(String[] args){
        if (BotDriver.setLogDriver("irc.tomsk.net", "files", new String[]{"/tmp/logs/"}, "/tmp/appLogs/"))
            System.out.println("DRVT_Files: Successful driver initiation");
        else {
            System.out.println("DRVT_Files: Failed driver initiation");
            return;
        }

        Worker fw1 = BotDriver.getWorker("irc.tomsk.net","system");
        Worker fw2 = BotDriver.getWorker("irc.tomsk.net","#main");
        Worker fw3 = BotDriver.getWorker("irc.tomsk.net","#lpr");

        if ((fw1 !=null) && (fw2 !=null) && (fw3 !=null)){
            System.out.println("DRVT_Files:LogFile1: "+fw1.isConsistent());
            System.out.println("DRVT_Files:LogFile2: "+fw2.isConsistent());
            System.out.println("DRVT_Files:LogFile3: "+fw3.isConsistent());
            boolean res;

            res = fw1.logAdd("JOIN", "de_su!loper@desktop.lan", "message1");
            System.out.println("DRVT_Files:fw1 exec result: "+res);
            res = fw1.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": some text here");
            System.out.println("DRVT_Files:fw1 exec result: "+res);
            res = fw1.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": more random tests");
            System.out.println("DRVT_Files:fw1 exec result: "+res);
            res = fw1.logAdd("NICK", "de_su!loper@desktop.lan", "developer_su");
            System.out.println("DRVT_Files:fw1 exec result: "+res);
            res = fw1.logAdd("MODE", "de_su!loper@desktop.lan", "+b username");
            System.out.println("DRVT_Files:fw1 exec result: "+res);
            res = fw1.logAdd("PART", "de_su!loper@desktop.lan", "#chan1");
            System.out.println("DRVT_Files:fw1 exec result: "+res);

            res = fw2.logAdd("JOIN", "de_su!loper@desktop.lan", "message2");
            System.out.println("DRVT_Files:fw2 exec result: "+res);
            res = fw2.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": some text here");
            System.out.println("DRVT_Files:fw2 exec result: "+res);
            res = fw2.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": more random tests");
            System.out.println("DRVT_Files:fw2 exec result: "+res);

            res = fw2.logAdd("NICK", "de_su!loper@desktop.lan", "developer_su");
            System.out.println("DRVT_Files:fw2 exec result: "+res);
            res = fw2.logAdd("MODE", "de_su!loper@desktop.lan", "+b username");
            System.out.println("DRVT_Files:fw2 exec result: "+res);
            res = fw2.logAdd("PART", "de_su!loper@desktop.lan", "#chan2");
            System.out.println("DRVT_Files:fw2 exec result: "+res);

            res = fw3.logAdd("JOIN", "de_su!loper@desktop.lan", "message3");
            System.out.println("DRVT_Files:fw3 exec result: "+res);
            res = fw3.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": some text here");
            System.out.println("DRVT_Files:fw3 exec result: "+res);
            res = fw3.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": more random tests");
            System.out.println("DRVT_Files:fw3 exec result: "+res);
            res = fw3.logAdd("NICK", "de_su!loper@desktop.lan", "developer_su");
            System.out.println("DRVT_Files:fw3 exec result: "+res);
            res = fw3.logAdd("MODE", "de_su!loper@desktop.lan", "+b username");
            System.out.println("DRVT_Files:fw3 exec result: "+res);
            res = fw3.logAdd("PART", "de_su!loper@desktop.lan", "#chan3");
            System.out.println("DRVT_Files:fw3 exec result: "+res);

            fw1.close();
            fw2.close();
            fw3.close();
        }
    }
}
