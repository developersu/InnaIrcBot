package InnaIrcBot;

import InnaIrcBot.ProvidersConsumers.DataProvider;
import InnaIrcBot.config.ConfigurationFile;
import InnaIrcBot.config.ConfigurationManager;

public class ConnectionsBuilder {
    private static Connections connections;

    public static Connections getConnections(){
        return connections;
    }

    static void buildConnections(String[] pathsToConfigurationFiles){
        connections = new Connections(pathsToConfigurationFiles);
    }

    static class Connections {
        private Connections(String[] pathsToConfigurationFiles){
            for (String configuration: pathsToConfigurationFiles) {
                ConfigurationFile configurationFile = registerConfiguration(configuration);
                if (configuration != null) {
                    startNewConnection(configurationFile);
                }
            }
        }

        private ConfigurationFile registerConfiguration(String configuration){
            try {
                return ConfigurationManager.readAndSetConfiguration(configuration);
            }
            catch (Exception e){
                System.out.println("Connections->constructor: configuration argument dropped because of "+e.getMessage());
            }
            return null;
        }

        private void startNewConnection(ConfigurationFile configurationFile){
            // if connectionsList already contains record with name, it should be removed from there first
            // if there are few configs with same server name then.. fuckup
            DataProvider runnableConnection = new DataProvider(configurationFile);
            Thread threadConnection = new Thread(runnableConnection);
            threadConnection.start();
        }

        public void startNewConnection(String serverName){
            try {
                ConfigurationFile configurationFile = ConfigurationManager.getConfiguration(serverName);
                startNewConnection(configurationFile);
            }
            catch (Exception e){
                System.out.println("Unable to start new connectionL "+e.getMessage());
            }
        }
    }

}
