package InnaIrcBot.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationManager {
    private final static Map<String, ConfigurationFile> configurations = Collections.synchronizedMap(new HashMap<>());

    public static ConfigurationFile readAndSetConfiguration(String pathToConfigurationFile) throws Exception{
        ConfigurationFile configurationFile = ConfigurationFileReader.read(pathToConfigurationFile);
        configurations.put(configurationFile.getServerName(), configurationFile);
        return configurationFile;
    }
    
    public static ConfigurationFile getConfiguration(String serverName) throws Exception{
        ConfigurationFile configurationFile = configurations.get(serverName);
        if (configurationFile == null)
            throw new Exception("No configuration found for server "+serverName);
        return configurationFile;
    }
}
