package InnaIrcBot.LogDriver;

import java.util.HashMap;

public class BotDriver {
    private static HashMap<String, String[][]> serverDriver = new HashMap<>();
    /**
     * Define driver for desired server
     * */
    // TODO: add proxy worker for using with multiple drivers
    public static synchronized boolean setLogDriver(String serverName, String driver, String[] driverParams){
        if (!driver.isEmpty() && driverParams != null && driverParams.length > 0 && driverParams[0] != null && !driverParams[0].isEmpty()) {
            String[][] drvAndParams = {
                    {driver},
                    driverParams
            };
            serverDriver.put(serverName, drvAndParams);
            return true;
        }
        else
            return false;
    }
    public static synchronized Worker getWorker(String serverName, String chanelName){
        if (serverDriver.containsKey(serverName)) {
            switch (serverDriver.get(serverName)[0][0].toLowerCase()) {
                case "files":
                    BotFilesWorker botFilesWorker = new BotFilesWorker(serverName, serverDriver.get(serverName)[1], chanelName);
                    return validateConstancy(botFilesWorker, serverName, chanelName);
                case "sqlite":
                    BotSQLiteWorker botSQLiteWorker = new BotSQLiteWorker(serverName, serverDriver.get(serverName)[1], chanelName);
                    return validateConstancy(botSQLiteWorker, serverName, chanelName);
                case "mongodb":
                    BotMongoWorker botMongoWorker = new BotMongoWorker(serverName, serverDriver.get(serverName)[1], chanelName);
                    return validateConstancy(botMongoWorker, serverName, chanelName);
                case "zero":
                    return new BotZeroWorker();
                default:
                    System.out.println("BotDriver->getWorker(): Configuration issue: can't find required driver \""
                            +serverDriver.get(serverName)[0][0]
                            +"\".Using \"ZeroWorker\".");
                    return new BotZeroWorker();
            }
        }
        System.out.println("BotDriver->getWorker(): Unknown issue: Channel exists for non-existing server.\n\tUsing ZeroWorker.");
        return new BotZeroWorker();
    }
    // If channel found that it's impossible to use defined worker from user settings and asking for use ZeroWorker
    public static synchronized Worker getZeroWorker(){
        return new BotZeroWorker();
    }
    private static Worker validateConstancy(Worker worker, String srv, String chan){     // synchronized?
        if (worker.isConsistent()){
            return worker;
        }
        else {
            System.out.println("BotDriver->validateConstancy(): Unable to use "
                    +worker.getClass().getSimpleName()+" for "+srv+"/"+chan
                    +". Using ZeroWorker instead.");
            return new BotZeroWorker();
        }
    }
}
