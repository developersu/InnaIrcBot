package InnaIrcBot.logging;

import InnaIrcBot.ProvidersConsumers.StreamProvider;
import InnaIrcBot.config.LogDriverConfiguration;

import java.util.HashMap;

public class LogDriver {
    private final static HashMap<String, String[][]> serverDriver = new HashMap<>();
    private final static HashMap<String, WorkerSystem> systemLogWorkerMap = new HashMap<>();
    /**
     * Define driver for desired server
     * */
    // TODO: add proxy worker for using with multiple drivers
    public static synchronized void setLogDriver(String server,
                                                 LogDriverConfiguration logDriverConfiguration,
                                                 String applicationLogDir){
        String[][] drvAndParams = {
                {logDriverConfiguration.getName()},
                logDriverConfiguration.getParams()
        };
        serverDriver.put(server, drvAndParams);
        systemLogWorkerMap.put(server, new WorkerSystem(server, applicationLogDir));
    }

    public static synchronized Worker getWorker(String server, String channel){
        if (serverDriver.containsKey(server)) {
            switch (serverDriver.get(server)[0][0]) {
                case "files":
                    WorkerFiles workerFiles = new WorkerFiles(server, serverDriver.get(server)[1], channel);
                    return validateConsistancy(workerFiles, server, channel);
                case "sqlite":
                    WorkerSQLite workerSQLite = new WorkerSQLite(server, serverDriver.get(server)[1], channel);
                    return validateConsistancy(workerSQLite, server, channel);
                case "mongodb":
                    WorkerMongoDB workerMongoDB = new WorkerMongoDB(server, serverDriver.get(server)[1], channel);
                    return validateConsistancy(workerMongoDB, server, channel);
                case "zero":
                    return new WorkerZero();
            }
        }
        System.out.println("Issue on BotDriver->getWorker(): Channel requested for non-existing server?\n" +
                "\tUsing ZeroWorker.");
        return new WorkerZero();
    }
    // If channel found that it's impossible to use defined worker from user settings and asking for use ZeroWorker
    public static synchronized Worker getZeroWorker(){ return new WorkerZero(); }

    public static synchronized WorkerSystem getSystemWorker(String serverName){
        return systemLogWorkerMap.get(serverName);
    }

    private static Worker validateConsistancy(Worker worker, String server, String channel){     // synchronized?
        if (worker.isConsistent()){
            return worker;
        }
        System.out.println("BotDriver->validateConstancy(): Unable to use "
                +worker.getClass().getSimpleName()+" for "+server+"/"+channel
                +". Using ZeroWorker instead.");
        return new WorkerZero();
    }
}
