package InnaIrcBot.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class StorageReader {
    public static StorageFile readConfig(String pathToFile){        // TODO: NULL or object
        StorageFile storageObject = null;

        File configFile = new File(pathToFile);

        try (Reader fileReader = new InputStreamReader(new FileInputStream(configFile))) {
            storageObject = new Gson().fromJson(fileReader, StorageFile.class);
            return validateConfig(storageObject);
        } catch (java.io.FileNotFoundException e){
            System.out.println("Configuration file not found.");
            return null;
        } catch (java.io.IOException e){
            System.out.println("Configuration file is empty or incorrect.");
            return null;
        }
    }
    private static StorageFile validateConfig(StorageFile sf){      //TODO: more validation

        if(sf.getServerName().isEmpty()){
            System.out.println("Server not defined in configuration file.");
            return null;
        }
        else if(sf.getServerPort() <= 0){
            System.out.println("Server port set incorrectly in configuration file.");
            return null;
        }
        else
            return sf;
    }
    public static void generateDefaultConfig(String pathToFile){
        File savingFile;
        if (pathToFile == null) {                               // no pathToFile? create new in homeDir
            pathToFile = System.getProperty("user.dir")
                    +File.separator
                    +"myBotConfig.conf";
            savingFile = new File(pathToFile);
        }
        else if(pathToFile.endsWith(File.separator)) {          // ends with .../ then create in dir
            pathToFile = pathToFile + "myBotConfig.conf";
            savingFile = new File(pathToFile);
            if (!savingFile.getParentFile().exists())
                savingFile.getParentFile().mkdirs();
        }
        else {                                                   // check if it's dir, if yes, then create inside
            savingFile = new File(pathToFile);
            if (savingFile.exists() && savingFile.isDirectory()) {
                pathToFile = pathToFile + File.separator + "myBotConfig.conf";
                savingFile = new File(pathToFile);
            }
            else if (!savingFile.getParentFile().exists())
                savingFile.getParentFile().mkdirs();
        }
        try {
            savingFile.createNewFile();
            Writer writerFile = new OutputStreamWriter(new FileOutputStream(savingFile.getAbsolutePath()), StandardCharsets.UTF_8);

            StorageFile storageFileObject = new StorageFile("srv",
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
            writingStorageObject.toJson(storageFileObject, writerFile);
            writerFile.close();
            System.out.println("Configuration file created: " + pathToFile);
        }  catch (java.io.FileNotFoundException e){
            System.out.println("Configuration file not found or can't create:\n\t"+e);
        } catch (java.io.UnsupportedEncodingException e){
            System.out.println("Unsupported encoding of the configuration file:\n\t"+e);
        } catch (java.io.IOException e){
            System.out.println("Unable to write configuration file: I/O exception:\n\t"+e);
        }
    }
}
