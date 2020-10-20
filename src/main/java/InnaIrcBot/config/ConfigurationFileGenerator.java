package InnaIrcBot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationFileGenerator {
    private String fileLocation;

    public static void generate(String fileLocation){
        new ConfigurationFileGenerator(fileLocation);
    }

    private ConfigurationFileGenerator(String fileLocation){
        this.fileLocation = fileLocation;

        try {
            if (locationNotDefined()) {  // create new in homeDir
                setLocationDefault();
            }
            else if(locationIsFolder()) {          // ends with .../ then create in dir
                setLocationInsideFolder();
            }

            createConfigurationFile();

            System.out.println("Configuration file created: " + this.fileLocation); // TODO: Move to l4j
        } catch (IOException e){
            System.out.println("Unable to write configuration file: \n\t"+e.getMessage());
        }
    }

    private void setLocationDefault(){
        fileLocation = System.getProperty("user.dir")
                    + File.separator
                    + "myBotConfig.conf";
    }

    private boolean locationNotDefined(){
        return fileLocation == null;
    }

    private boolean locationIsFolder(){
        return fileLocation.endsWith(File.separator) || Files.isDirectory(Paths.get(fileLocation));
    }

    private void setLocationInsideFolder() throws IOException{
        createFoldersIfNeeded();
        if (fileLocation.endsWith(File.separator))
            fileLocation = fileLocation + "myBotConfig.conf";
        else
            fileLocation = fileLocation + File.separator + "myBotConfig.conf";
    }
    private void createFoldersIfNeeded() throws IOException{
        Path folderPath = Paths.get(fileLocation);
        if (! Files.exists(folderPath))
            Files.createDirectories(folderPath);
    }
    private void createConfigurationFile() throws IOException{
        File configurationFile = new File(this.fileLocation);

        Writer writerFile = new OutputStreamWriter(new FileOutputStream(configurationFile.getAbsolutePath()), StandardCharsets.UTF_8);

        ConfigurationFile configurationFileObject = new ConfigurationFile("srv",
                6667,
                "",
                new String[] {"#lpr",
                        "#main"},
                "user_nick",
                "ident",
                "bot",
                "",
                "freenode",
                "ix",
                true,
                "Files",
                new String[] {System.getProperty("user.home")},
                "pswd",
                System.getProperty("user.home"),
                "/var/logs/"
        );

        Gson writingStorageObject = new GsonBuilder().setPrettyPrinting().create();
        writingStorageObject.toJson(configurationFileObject, writerFile);
        writerFile.close();
    }
}
