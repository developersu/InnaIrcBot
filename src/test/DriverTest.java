import InnaIrcBot.LogDriver.BotDriver;
import InnaIrcBot.LogDriver.Worker;

public class DriverTest {

    public static void main(String[] args){
        if (BotDriver.setFileDriver("irc.tomsk.net", "SQLiteDriver", new String[]{"/tmp/"}))
                System.out.println("Successful driver initiation");
        else {
            System.out.println("Failed driver initiation");
            return;
        }

        Worker fw1 = BotDriver.getWorker("irc.tomsk.net","#lpr");
        Worker fw2 = BotDriver.getWorker("irc.tomsk.net","#main");
        Worker fw3 = BotDriver.getWorker("irc.tomsk.net","##loper");

        if ((fw1 !=null) && (fw2 !=null) && (fw3 !=null)){
            System.out.println("LogFile1: "+fw1.isConsistent());
            System.out.println("LogFile2: "+fw2.isConsistent());
            System.out.println("LogFile3: "+fw3.isConsistent());

            fw1.logAdd("JOIN", "de_su!loper@desktop.lan", "message1");
            fw1.logAdd("PART", "de_su!loper@desktop.lan", "#chan1");

            fw2.logAdd("JOIN", "de_su!loper@desktop.lan", "message2");
            fw2.logAdd("PART", "de_su!loper@desktop.lan", "#chan2");

            fw3.logAdd("JOIN", "de_su!loper@desktop.lan", "message3");
            fw3.logAdd("PART", "de_su!loper@desktop.lan", "#chan3");

            fw1.close();
            fw2.close();
            fw3.close();
        }
    }
}
