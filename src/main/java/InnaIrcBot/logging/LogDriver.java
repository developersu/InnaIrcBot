package InnaIrcBot.logging;

import InnaIrcBot.config.ConfigurationFile;
import InnaIrcBot.config.ConfigurationManager;
import InnaIrcBot.config.LogDriverConfiguration;

import java.util.HashMap;

public class LogDriver {
    private final static HashMap<String, WorkerSystem> systemLogWorkerMap = new HashMap<>();

    // TODO: add proxy multiple drivers support
    public static synchronized void setLogDriver(String server){
        String applicationLogDir;
        try {
            applicationLogDir = ConfigurationManager.getConfiguration(server).getApplicationLogDir();
        }
        catch (Exception e){
            applicationLogDir = "";
        }
        systemLogWorkerMap.put(server, new WorkerSystem(server, applicationLogDir));
    }

    public static synchronized Worker getWorker(String server, String channel){
        try {
            ConfigurationFile serverConfiguration = ConfigurationManager.getConfiguration(server);
            LogDriverConfiguration logDriverConfiguration = serverConfiguration.getLogDriverConfiguration();

            switch (logDriverConfiguration.getName()) {
                case "files":
                    return new WorkerFiles(server, logDriverConfiguration, channel);
                case "sqlite":
                    return new WorkerSQLite(server, logDriverConfiguration, channel);
                case "mongodb":
                    return new WorkerMongoDB(server, logDriverConfiguration, channel);
                case "zero":
                default:
                    return new WorkerZero();
            }
        }
        catch (Exception e){
            System.out.println("Issue on BotDriver->getWorker(): Channel requested for non-existing server? "
                    + e.getMessage()
                    + "\n\tUsing ZeroWorker.");
            return new WorkerZero();
        }
    }

    public static synchronized WorkerSystem getSystemWorker(String serverName){
        return systemLogWorkerMap.get(serverName);
    }
}
