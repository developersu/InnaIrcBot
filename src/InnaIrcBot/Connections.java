package InnaIrcBot;

import InnaIrcBot.Config.StorageFile;
import InnaIrcBot.Config.StorageReader;
import InnaIrcBot.ProvidersConsumers.DataProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Connections {
//todo PING Server if not available
    private HashMap<String, StorageFile> configs;
    private ArrayList<Thread> connectionsList;

    Connections(String[] args){
        this.configs = new HashMap<>();
        this.connectionsList = new ArrayList<>();
        StorageFile tempConf = null;

        for (int i = 1; i < args.length; i++){
            if ((tempConf = StorageReader.readConfig(args[i])) != null) {
                configs.put(tempConf.getServerName(), tempConf);
            }
            else
                System.out.println("Connections->constructor: configuration argument dropped: "+args[i]);
        }
        if (!configs.isEmpty()){
            handleThreads();
        }
        else {
            System.out.println("Connections->constructor: Nothing to execute.");
        }
    }
    private void createAndStartThread(String serverName){
        //if connectionsList already contains record with name, it should be removed from there first
        // if there are few configs with same server name then.. fuckup

        Runnable runnableConnection = new DataProvider(configs.get(serverName));
        Thread threadConnection = new Thread(runnableConnection, serverName);
        threadConnection.start();
        connectionsList.add(threadConnection);
    }
    private void handleThreads() {
        // Crate array of threads
        for (String serverName : configs.keySet())
            createAndStartThread(serverName);
        // Watch threads
        Iterator<Thread> it = connectionsList.iterator();

        while (it.hasNext()) {
            System.out.println("\n" + it.next().getName() + "\n");
        }

        while (!connectionsList.isEmpty()) {                                   // While we have something in connectionList
            while (it.hasNext()) {                                          // Proceed for every thread in the list
                Thread curThread = it.next();
                if (!curThread.isAlive())                                   // If thread is dead
                    if (ReconnectControl.get(curThread.getName())) {        // And ReconnectControl says that this thread shouldn't be dead
                        ReconnectControl.delete(curThread.getName());         // [Try to] remove rule-record from ReconnectControl structure
                        connectionsList.remove(curThread);
                        createAndStartThread(curThread.getName());
                        System.out.println("DEBUG: Thread "+curThread.getName()+" going to restart after unexpected finish.\n\t"+connectionsList.toString());
                        break;
                    } else {                                                  // And ReconnectControl says that this thread death expected
                        ReconnectControl.delete(curThread.getName());         // [Try to] remove rule-record from ReconnectControl structure
                        connectionsList.remove(curThread);
                        System.out.println("DEBUG: Thread "+curThread.getName()+" removed from observable list after expected finish.\n\t"+connectionsList.toString());
                        break;
                    }
            }
            if (connectionsList.isEmpty()) {
                System.out.println("connectionsList.isEmpty()");
                break;
            }
            else {
                it = connectionsList.iterator();
            }

        }
    }
}
