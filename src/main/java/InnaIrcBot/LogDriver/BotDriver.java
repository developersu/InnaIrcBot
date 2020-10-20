package InnaIrcBot.LogDriver;

import java.util.HashMap;

public class BotDriver {
    private final static HashMap<String, String[][]> serverDriver = new HashMap<>();
    private final static HashMap<String, BotSystemWorker> systemLogWorkerMap = new HashMap<>();
    /**
     * Define driver for desired server
     * */
    // TODO: add proxy worker for using with multiple drivers
    public static synchronized boolean setLogDriver(String serverName, String driver, String[] driverParams, String applicationLogDir){
        if (driver == null)
            return false;
        if (driver.isEmpty())
            return false;
        if (driverParams.length == 0)
            return false;
        if (driverParams[0] == null)
            return false;
        if (driverParams[0].isEmpty())
            return false;

        String[][] drvAndParams = {
                {driver},
                driverParams
        };
        serverDriver.put(serverName, drvAndParams);
        systemLogWorkerMap.put(serverName, new BotSystemWorker(serverName, applicationLogDir));
        return true;
    }

    public static synchronized Worker getWorker(String server, String channel){
        if (serverDriver.containsKey(server)) {
            switch (serverDriver.get(server)[0][0].toLowerCase()) {
                case "files":
                    BotFilesWorker botFilesWorker = new BotFilesWorker(server, serverDriver.get(server)[1], channel);
                    return validateConsistancy(botFilesWorker, server, channel);
                case "sqlite":
                    BotSQLiteWorker botSQLiteWorker = new BotSQLiteWorker(server, serverDriver.get(server)[1], channel);
                    return validateConsistancy(botSQLiteWorker, server, channel);
                case "mongodb":
                    BotMongoWorker botMongoWorker = new BotMongoWorker(server, serverDriver.get(server)[1], channel);
                    return validateConsistancy(botMongoWorker, server, channel);
                case "zero":
                    return new BotZeroWorker();
                default:
                    System.out.println("BotDriver->getWorker(): Configuration issue: can't find required driver \""
                            +serverDriver.get(server)[0][0]
                            +"\".Using \"ZeroWorker\".");
                    return new BotZeroWorker();
            }
        }
        System.out.println("Issue on BotDriver->getWorker(): Channel requested for non-existing server?\n" +
                "\tUsing ZeroWorker.");
        return new BotZeroWorker();
    }
    // If channel found that it's impossible to use defined worker from user settings and asking for use ZeroWorker
    public static synchronized Worker getZeroWorker(){ return new BotZeroWorker(); }

    public static synchronized BotSystemWorker getSystemWorker(String serverName){
        return systemLogWorkerMap.get(serverName);
    }

    private static Worker validateConsistancy(Worker worker, String server, String channel){     // synchronized?
        if (worker.isConsistent()){
            return worker;
        }
        System.out.println("BotDriver->validateConstancy(): Unable to use "
                +worker.getClass().getSimpleName()+" for "+server+"/"+channel
                +". Using ZeroWorker instead.");
        return new BotZeroWorker();
    }
}
