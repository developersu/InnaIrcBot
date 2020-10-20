package InnaIrcBot.LogDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BotSystemWorker implements SystemWorker{

    private FileWriter fileWriter;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private ThingToCloseOnDie thingToCloseOnDie;     // call .die() method of this classes when this (system log class) dies.

    private final String server;

    private boolean consistent = false;

    public BotSystemWorker(String server, String appLogDir){
        this.server = server;

        if (appLogDir.isEmpty()) {
            appLogDir = System.getProperty("java.io.tmpdir")+File.separator+"innaircbot"+File.separator;
        }
        else if (! appLogDir.endsWith(File.separator)) {
            appLogDir += File.separator;
        }

        appLogDir += server;

        File logFile = new File(appLogDir);

        try {
            if (! logFile.getParentFile().exists()) {
                if (! logFile.getParentFile().mkdirs()){
                    System.out.println("BotSystemWorker (@"+server+")->constructor() failed:\n" +
                            "\tUnable to create sub-directory(-ies) to store log file: " + appLogDir);
                    return;
                }
            }
            fileWriter = new FileWriter(logFile, true);
            consistent = true;
        } catch (SecurityException e){
            System.out.println("BotSystemWorker (@"+server+")->constructor() failed.\n" +
                    "\tUnable to create sub-directory(-ies) to store logs file ("+appLogDir+"):\n\t"+e.getMessage());
        } catch (IOException oie){
            System.out.println("BotSystemWorker (@"+server+")->constructor() failed:\n" +
                    "\tUnable to open file to store logs: " + appLogDir + " "+ oie.getMessage());
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
            } catch (Exception e) {     // ??? No ideas. Just in case. Consider removing.
                System.out.println("BotSystemWorker (@" + server + ")->logAdd() failed\n\tUnable to write logs of because of exception:\n\t" + e.getMessage());
                //this.close();
                consistent = false;
            }
            return;
        }
        System.out.println(genDate() + event + " " + initiatorArg + " " + messageArg + "\n");
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

        try {
            fileWriter.close();
        }
        catch (IOException | NullPointerException ignore){}
        consistent = false;
    }
}
