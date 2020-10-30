package InnaIrcBot.logging;

import InnaIrcBot.config.ConfigurationFile;
import InnaIrcBot.config.ConfigurationManager;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class WorkerSystem{
    private final static HashMap<String, WorkerSystem> systemLogWorkerMap = new HashMap<>();

    // TODO: add proxy multiple drivers support
    public static synchronized void setLogDriver(String server){
        String applicationLogDir;
        try {
            ConfigurationFile configuration = ConfigurationManager.getConfiguration(server);
            applicationLogDir = configuration.getApplicationLogDir();
        }
        catch (Exception e){
            applicationLogDir = "";
        }
        systemLogWorkerMap.put(server, new WorkerSystem(server, applicationLogDir));
    }

    public static synchronized WorkerSystem getSystemWorker(String serverName){
        return systemLogWorkerMap.get(serverName);
    }


    private FileWriter fileWriter;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private Closeable thingToCloseOnDie;     // call .close() method of this classes when this (system log class) dies.

    private boolean consistent;

    private String filePath;

    public WorkerSystem(String server, String appLogDir){
        try {
            formatFilePath(server, appLogDir);

            fileWriter = new FileWriter(createServerLogsFile(), true);
            consistent = true;
        } catch (Exception e){
            System.out.println("BotSystemWorker for "+server+" failed: " + e.getMessage());
        }
    }

    private void formatFilePath(String server, String dirLocation){
        if (dirLocation.isEmpty())
            dirLocation = "./";

        if (dirLocation.endsWith(File.separator))
            this.filePath = dirLocation+server+".log";
        else
            this.filePath = dirLocation;
    }

    private File createServerLogsFile() throws Exception{
        final File file = new File(filePath);

        if (file.exists()){
            if (file.isFile())
                return file;
            else
                throw new Exception("WorkerSystem: \""+filePath+"\" is directory while file expected.");
        }

        if (file.createNewFile())
            return file;

        throw new Exception("WorkerSystem: Can't create file: "+filePath);
    }

    private String genDate(){
        return "["+ LocalTime.now().format(dateFormat)+"]";
    }

    public void log(String initiatorArg, String messageArg) {
        String message = String.format("%s %s %s\n", genDate(), initiatorArg, messageArg);

        if (consistent)
            logToFile(message);
        else
            System.out.println(message);
    }
    private void logToFile(String message){
        try {
            fileWriter.write(message);
            fileWriter.flush();
        } catch (Exception e) {
            System.out.println("BotSystemWorker: unable to write application logs: " + e.getMessage());
            consistent = false;
            //this.close();
        }
    }

    public void registerInSystemWorker(Closeable thing){
        if (this.thingToCloseOnDie == null){        // only one needed
            this.thingToCloseOnDie = thing;
        }
    }

    public void close() {
        try {
            if (thingToCloseOnDie != null)
                thingToCloseOnDie.close();
            fileWriter.close();
        }
        catch (IOException | NullPointerException ignore){}
        consistent = false;
    }
}
