package InnaIrcBot.config;

import org.ini4j.*;
import org.ini4j.spi.EscapeTool;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
                    + "innaircbot.conf";
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
            fileLocation = fileLocation + "innaircbot.conf";
        else
            fileLocation = fileLocation + File.separator + "innaircbot.conf";
    }
    private void createFoldersIfNeeded() throws IOException{
        Path folderPath = Paths.get(fileLocation);
        if (! Files.exists(folderPath))
            Files.createDirectories(folderPath);
    }
    private void createConfigurationFile() throws IOException{
        final String mainSectionName = "main";
        final String channelSectionName = "channels";

        List<String> channels = new ArrayList<>();
        channels.add("#main");
        channels.add("#lpr");
        List<String> logDriverPreferences = new ArrayList<>();
        logDriverPreferences.add(System.getProperty("user.home"));

        File configurationFile = new File(this.fileLocation);

        Config myConfig = new Config();
        myConfig.setFileEncoding(StandardCharsets.UTF_8);
        myConfig.setMultiOption(true);
        myConfig.setEscape(true);
        //myConfig.setEmptyOption(true);
        //myConfig.setComment(true);

        Wini ini = new Wini();
        ini.setConfig(myConfig);
        
        Ini.Section mainSection = ini.add(mainSectionName);
        mainSection.put( "server name", "srv");
        mainSection.put( "server port", 6667);
        mainSection.put( "server password", "");
        mainSection.put( "nickname", "InnaIrcBot");
        mainSection.put( "ident", "sweethome");
        mainSection.put( "real name", "bot");
        mainSection.put( "nickname password", "");
        mainSection.put( "nickserv auth method", "freenode");
        mainSection.put( "user modes", "i");
        mainSection.put( "auto rejoin", true);
        mainSection.put( "bot administrator password", "i_pswd");
        mainSection.put( "application logs", "/tmp");
        mainSection.put("cooldown (sec)", 3);

        Ini.Section loggingSection = ini.add("logging");
        loggingSection.put( "driver", "files");
        loggingSection.put("file(s) location", "");
        loggingSection.put("MongoDB host:port", "");
        loggingSection.put("MongoDB DB table", "");
        loggingSection.put("MongoDB DB user", "");
        loggingSection.put("MongoDB DB password", "");

        Ini.Section channelsSection = ini.add(channelSectionName);
        channelsSection.putAll("channel", channels);

        Ini.Section channelMainSection = ini.add( channels.get(0) );

        Ini.Section channelMainRulesSection = channelMainSection.addChild("rules");
        channelMainRulesSection.add("rule","join\t^cv.*\t\\cversion\t(^.+(\\s|\\t)+)");
        channelMainRulesSection.add("rule","msg\t^cv.*\t\\cversion\t(^.+(\\s|\\t)+)");
        channelMainRulesSection.add("rule", "join\t^BadGuy1(.+)?!.*\t\\kickban\trequested\\privmsg\tNever come back");
        channelMainRulesSection.add("rule", "join\t^BadGuy2(.+)?!.*\t\\kick\tkick!");
        channelMainRulesSection.add("rule", "join\t^BadGuy3(.+)?!.*\t\\ban\tban!");
        channelMainRulesSection.add("rule", "nick\t^BadNickname.*\t\\chanmsg\tstop it!\n");
        channelMainRulesSection.add("rule", "nick\t^Billilish.*\t\\voice");

        channelMainRulesSection.add("rule", "msg\t^cci.*\t\\cclientinfo\t\t(^.+(\\s|\\t)+)\tThere are no ");
        channelMainRulesSection.add("rule", "msg\t^cf.*\t\\cfinger\t(^.+(\\s|\\t)+)\tI \t D\t K");
        channelMainRulesSection.add("rule", "msg\t^cp.*\t\\cping\t\t(^.+(\\s|\\t)+)\tlol");
        channelMainRulesSection.add("rule", "msg\t^cs.*\t\\csource\t(^.+(\\s|\\t)+)\tpiu-\tpiu\t: ");
        channelMainRulesSection.add("rule", "msg\t^ct.*\t\\ctime\t\t(^.+(\\s|\\t)+)\tOoops:");
        channelMainRulesSection.add("rule", "msg\t^cui.*\t\\cuserinfo\t(^.+(\\s|\\t)+)\tNope: ");
        channelMainRulesSection.add("rule", "msg\t^cv.*\t\\cversion\t(^.+(\\s|\\t)+)");
        channelMainRulesSection.add("rule", "msg\t^pci.*\t\\pclientinfo\t(^.+(\\s|\\t)+)\tkek: ");
        channelMainRulesSection.add("rule", "msg\t^pf.*\t\\pfinger\t(^.+(\\s|\\t)+)\t\tNobody like: ");
        channelMainRulesSection.add("rule", "msg\t^pp.*\t\\pping\t\t(^.+(\\s|\\t)+)\tnope: ");
        channelMainRulesSection.add("rule", "msg\t^ps.*\t\\psource\t(^.+(\\s|\\t)+)\t\tNooo ");
        channelMainRulesSection.add("rule", "msg\t^pt.*\t\\ptime\t\t(^.+(\\s|\\t)+)\tHo-ho-ho: ");
        channelMainRulesSection.add("rule", "msg\t^pu.*\t\\puserinfo\t(^.+(\\s|\\t)+)\tSome random text: ");
        channelMainRulesSection.add("rule", "msg\t^pv.*\t\\pversion\t(^.+(\\s|\\t)+)\t\\chanmsg\tsent!");


        Ini.Section channelMainJoinFloodControlSection = channelMainSection.addChild("JoinFloodControl");
        channelMainJoinFloodControlSection.put("enable", true);
        channelMainJoinFloodControlSection.put("join number", 3);
        channelMainJoinFloodControlSection.put("time frame", 60);

        Ini.Section channelMainJoinCloneControlSection = channelMainSection.addChild("JoinCloneControl");
        channelMainJoinCloneControlSection.put("enable", false);
        channelMainJoinCloneControlSection.put("pattern", "^.+[0-9]+?!.*$");
        channelMainJoinCloneControlSection.put("time frame", 0);

        Ini.Section linksHeaderParser = channelMainSection.addChild("ParseLinksTitles");
        linksHeaderParser.put("enable", true);

        ini.store(configurationFile);
    }
}
