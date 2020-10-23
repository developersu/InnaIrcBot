package InnaIrcBot.config;

import InnaIrcBot.logging.SupportedLogDrivers;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Wini;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationFileTest {
    ConfigurationFile config;
    
    ConfigurationFileTest() throws Exception{
        File file = File.createTempFile("temp", "Ini");
/*
        List<String> logDriverPreferences = new ArrayList<>();
        logDriverPreferences.add(System.getProperty("user.home"));

        List<String> channels = new ArrayList<>();
        channels.add("#main");
        channels.add("#lpr");

        Config myConfig = new Config();
        myConfig.setFileEncoding(StandardCharsets.UTF_8);

        Wini ini = new Wini();
        ini.setConfig(myConfig);

        Ini.Section mainSection = ini.add("main");
        mainSection.put( "server name", "srv");
        mainSection.put( "server port", 6667);
        mainSection.put( "server password", "");
        mainSection.put(  "channels", channels); //mainSectionName,
        mainSection.put( "nickname", "InnaIrcBot");
        mainSection.put( "ident", "sweethome");
        mainSection.put( "real name", "bot");
        mainSection.put( "nickname password", "");
        mainSection.put( "nickserv auth method", "freenode");
        mainSection.put( "user modes", "i");
        mainSection.put( "auto rejoin", true);
        mainSection.put( "logging driver", "files");
        mainSection.put( "logging driver prefs", logDriverPreferences);
        mainSection.put( "bot administrator password", "i_pswd");
        mainSection.put( "channels configuration path", "/tmp");
        mainSection.put( "application logs", "/tmp");
        ini.store(file);

 */
        ConfigurationFileGenerator.generate(file.getAbsolutePath());
        config = new ConfigurationFile(file.getAbsolutePath());
        printAllConfigFields();
    }

    private String generateFile(){
        return "";
    }
    
    private void printAllConfigFields(){
        System.out.println();
        System.out.println(config.getServerName());;
        System.out.println(config.getServerPort());;
        System.out.println(config.getServerPass());;
        System.out.println(config.getUserNick());;
        System.out.println(config.getUserIdent());;
        System.out.println(config.getUserRealName());;
        System.out.println(config.getUserNickPass());;
        System.out.println(config.getUserNickAuthStyle());;
        System.out.println(config.getUserMode());;
        System.out.println(config.getRejoinOnKick());;
        System.out.println(config.getBotAdministratorPassword());;
        System.out.println(config.getApplicationLogDir());;
    }
    
    @Test
    void getServerName() {
    }
/*
    @Test
    void getServerPort() {
    }

    @Test
    void getServerPass() {
    }

    @Test
    void getChannels() {
    }

    @Test
    void getUserNick() {
    }

    @Test
    void getUserIdent() {
    }

    @Test
    void getUserRealName() {
    }

    @Test
    void getUserNickPass() {
    }

    @Test
    void getUserNickAuthStyle() {
    }

    @Test
    void getUserMode() {
    }

    @Test
    void getRejoinOnKick() {
    }

    @Test
    void getLogDriverConfiguration() {
        assertEquals(config.getLogDriverConfiguration().getName(), SupportedLogDrivers.zero);
    }

    @Test
    void getBotAdministratorPassword() {
    }

    @Test
    void getChanelConfigurationsPath() {
    }

    @Test
    void getApplicationLogDir() {
    }

    @Test
    void setUserNickAuthStyle() {
    }
     */
}