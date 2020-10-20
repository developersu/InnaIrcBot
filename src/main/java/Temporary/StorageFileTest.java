package Temporary;

import InnaIrcBot.config.ConfigurationFile;

public class StorageFileTest {
    static public void main(String[] args){
        ConfigurationFile config = new ConfigurationFile(
                "",
                0,
                "",
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                true,
                "",
                new String[]{null},
                "",
                "",
                ""
        );

        System.out.println(config.getLogDriver().isEmpty());
        System.out.println(config.getLogDriverParameters().length);


    }
}
