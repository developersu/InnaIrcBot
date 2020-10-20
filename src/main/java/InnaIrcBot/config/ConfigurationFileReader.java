package InnaIrcBot.config;

import com.google.gson.Gson;

import java.io.*;

public class ConfigurationFileReader {
    private ConfigurationFileReader(){}

    static ConfigurationFile read(String pathToFile) throws Exception{        // TODO: NULL or object
        ConfigurationFile storageObject;

        File configFile = new File(pathToFile);

        try (Reader fileReader = new InputStreamReader(new FileInputStream(configFile))) {
            storageObject = new Gson().fromJson(fileReader, ConfigurationFile.class);
        }

        validate(storageObject);

        return storageObject;
    }

    private static void validate(ConfigurationFile configurationFile) throws Exception{      //TODO: more validation
        if (configurationFile.getServerName().isEmpty())
            throw new Exception("Server not defined in configuration file.");

        if (configurationFile.getServerPort() <= 0)
            throw new Exception("Server port set incorrectly in configuration file.");

        String nick = configurationFile.getUserNick();
        if (nick.isEmpty())
            throw new Exception("Configuration issue: no nickname specified. ");

        if (! configurationFile.getUserNickPass().isEmpty()) {
            if (configurationFile.getUserNickAuthStyle().isEmpty())
                throw new Exception("Configuration issue: password specified while auth method is not.");

            configurationFile.setUserNickAuthStyle( configurationFile.getUserNickAuthStyle().toLowerCase() );

            if ( ! configurationFile.getUserNickAuthStyle().equals("rusnet") && ! configurationFile.getUserNickAuthStyle().equals("freenode"))
                throw new Exception("Configuration issue: userNickAuthStyle could be freenode or rusnet.");
        }

        if (configurationFile.getServerPort() <= 0 || configurationFile.getServerPort() > 65535)
            throw new Exception("Server port number cannot be less/equal zero or greater then 65535");
    }


}
