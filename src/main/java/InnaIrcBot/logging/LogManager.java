package InnaIrcBot.logging;

import InnaIrcBot.config.ConfigurationFile;
import InnaIrcBot.config.ConfigurationManager;
import InnaIrcBot.config.LogDriverConfiguration;

public class LogManager {
    private final String server;
    private final String channel;
    private Worker worker;

    public LogManager(String server, String channel){
        this.server = server;
        this.channel = channel;
        this.worker = getWorker(server, channel);
    }

    public void add(String event, String initiator, String message) {
        try {
            worker.logAdd(event, initiator, message);
        }
        catch (Exception e){
            System.out.println("Unable to use LogDriver for "+server+"/"+channel+" "+e.getMessage());
            worker = new WorkerZero();
        }
    }

    public void close() {
        worker.close();
    }

    private Worker getWorker(String server, String channel) {
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
}
