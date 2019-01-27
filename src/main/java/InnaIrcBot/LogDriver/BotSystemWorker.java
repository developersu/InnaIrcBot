package InnaIrcBot.LogDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BotSystemWorker implements SystemWorker{

    private FileWriter fileWriter;
    private DateTimeFormatter dateFormat;
    private ThingToCloseOnDie thingToCloseOnDie;     // call .die() method of this classes when this (system log class) dies.

    String ircServer;

    private boolean consistent = false;

    public BotSystemWorker(String ircServer, String appLogDir){
        this.ircServer = ircServer;
        this.dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");


        if (appLogDir.isEmpty()) {
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                appLogDir = System.getProperty("user.home")+ File.separator
                        +"AppData"+File.separator
                        +"Local"+File.separator
                        +"InnaIrcBot"+File.separator;
            } else {
                appLogDir = "/var/log/innaircbot/";
            }
        }
        if (!appLogDir.endsWith(File.separator))
            appLogDir = appLogDir+File.separator;

        appLogDir = appLogDir+ircServer;
        File logFile = new File(appLogDir);
        try {
            logFile.getParentFile().mkdirs();
        } catch (SecurityException e){
            System.out.println("BotSystemWorker (@"+ircServer+")->constructor() failed. Unable to create sub-directory(-ies) to store logs file ("+appLogDir+"):\n\t"+e);
            return;                     // Consistent = false
        }
        if (!logFile.getParentFile().exists()) {
            System.out.println("BotSystemWorker (@"+ircServer+")->constructor() failed:\n\tUnable to create sub-directory(-ies) to store log file: " + appLogDir);
            return;
        }
        try {
            this.fileWriter = new FileWriter(logFile, true);
            consistent = true;
        } catch (IOException oie){
            System.out.println("BotSystemWorker (@"+ircServer+")->constructor() failed:\n\tUnable to open file to store logs: " + appLogDir);
        }
    }

    private String genDate(){
        return "["+ LocalTime.now().format(dateFormat)+"]  ";
    }

    @Override
    public void logAdd(String event, String initiatorArg, String messageArg) {
        if (consistent) {
            try {
                fileWriter.write(genDate() + event + " " + initiatorArg + " " + messageArg + "\n");
                fileWriter.flush();
            } catch (IOException e) {
                System.out.println("BotSystemWorker (@" + ircServer + ")->logAdd() failed\n\tUnable to write logs of because of internal failure in LocalTime representation.");
                //this.close();
                consistent = false;
            } catch (NullPointerException npe) {
                System.out.println("BotSystemWorker (@" + ircServer + ")->logAdd() failed\n\tUnable to write logs of because file descriptor already closed/was not opened.");
                consistent = false;
            } catch (Exception unknowne) {     // ??? No ideas. Just in case. Consider removing.
                System.out.println("BotSystemWorker (@" + ircServer + ")->logAdd() failed\n\tUnable to write logs of because of exception:\n\t" + unknowne);
                //this.close();
                consistent = false;
            }
        }
        else {
            System.out.println(genDate() + event + " " + initiatorArg + " " + messageArg + "\n");
        }
    }

    @Override
    public void registerInSystemWorker(ThingToCloseOnDie thing){
        if (this.thingToCloseOnDie == null){        // only one needed
            this.thingToCloseOnDie = thing;
        }
    }

    @Override
    public void close() {
        if (thingToCloseOnDie != null)
            thingToCloseOnDie.die();
        if (fileWriter != null) {
            try {
                fileWriter.close();
            }
            catch (java.io.IOException e){
                System.out.println("BotSystemWorker (@"+ircServer+")->close() failed\n\tUnable to properly close logs file."); // Live with it.
            }
        }
        consistent = false;
    }
}
