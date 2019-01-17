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
            switch (serverDriver.get(serverName)[0][0]) {
                case "Files":
                    return new BotFilesWorker(serverName, serverDriver.get(serverName)[1], chanelName);
                case "SQLite":
                    return new BotSQLiteWorker(serverName, serverDriver.get(serverName)[1], chanelName);
                case "MongoDB":
                    return new BotMongoWorker(serverName, serverDriver.get(serverName)[1], chanelName);
                case "Zero":
                    return new BotZeroWorker();
                default:
                    System.out.println("Configuration issue: BotDriver->getWorker() can't find required driver \""
                            +serverDriver.get(serverName)[0][0]
                            +"\".Using \"ZeroWorker\".");
                    return new BotZeroWorker();
            }
        }
        return null;
    }
}
