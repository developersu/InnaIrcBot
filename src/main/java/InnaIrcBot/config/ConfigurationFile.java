package InnaIrcBot.config;

import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.ini4j.Wini;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigurationFile {
    private String serverName;
    private int serverPort;
    private String serverPass;
    private String userNick;
    private String userIdent;
    private String userRealName;
    private String userNickPass;
    private String userNickAuthStyle;
    private String userMode;
    private boolean rejoinOnKick;
    private String botAdministratorPassword;
    private String applicationLogDir;
    private int cooldownTime;
    private LogDriverConfiguration logDriverConfiguration;
    private List<String> channels;
    private HashMap<String, ConfigurationChannel> channelConfigs;

    public String getServerName() { return serverName; }
    public int getServerPort() { return serverPort; }
    public String getServerPass() { return serverPass; }
    public String getUserNick() { return userNick; }
    public String getUserIdent() { return userIdent; }
    public String getUserRealName() { return userRealName; }
    public String getUserNickPass() { return userNickPass; }
    public String getUserNickAuthStyle() { return userNickAuthStyle; }
    public String getUserMode() { return userMode; }
    public boolean getRejoinOnKick() { return rejoinOnKick; }
    public String getBotAdministratorPassword() { return botAdministratorPassword; }
    public String getApplicationLogDir() { return applicationLogDir; }
    public int getCooldownTime() { return cooldownTime; }

    public LogDriverConfiguration getLogDriverConfiguration(){ return logDriverConfiguration; }
    public List<String> getChannels() { return channels; }
    public ConfigurationChannel getChannelConfig(String channel) { return channelConfigs.get(channel); }

    public ConfigurationFile(String pathToConfigurationFile) throws Exception{
        Wini ini = new Wini();
        ini.setConfig(getConfig());
        ini.load(new File(pathToConfigurationFile));
        parseMain(ini);
        parseLogging(ini);
        parseChannels(ini);
        validate();
    }

    private Config getConfig(){
        Config config = new Config();
        config.setFileEncoding(StandardCharsets.UTF_8);
        config.setMultiOption(true);
        config.setEscape(true);
        return config;
    }

    private void parseMain(Wini ini){
        Ini.Section mainSection = ini.get("main");

        this.serverName = mainSection.getOrDefault("server name", "");
        this.serverPort = mainSection.get("server port", int.class);
        this.serverPass = mainSection.getOrDefault("server password", "");
        this.userNick = mainSection.getOrDefault("nickname", "");
        this.userIdent = mainSection.getOrDefault("ident", "");
        this.userRealName = mainSection.getOrDefault("real name", "");
        this.userNickPass = mainSection.getOrDefault("nickname password", "");
        this.userNickAuthStyle = mainSection.getOrDefault("nickserv auth method", "").toLowerCase();
        this.userMode = mainSection.getOrDefault("user modes", "");
        this.rejoinOnKick = mainSection.get("auto rejoin", boolean.class);
        this.botAdministratorPassword = mainSection.getOrDefault("bot administrator password", "");
        this.applicationLogDir = mainSection.getOrDefault("application logs", "");
        this.cooldownTime = mainSection.get("cooldown (sec)", int.class);
    }

    private void parseChannels(Wini ini){
        Ini.Section channelsSection = ini.get("channels");
        this.channels = channelsSection.getAll("channel");
        this.channelConfigs = new HashMap<>();
        for (String channel: channels){
            addNewChannelConfiguration(ini, channel);
        }
    }
    private void addNewChannelConfiguration(Wini ini, String channelName){
        Ini.Section channelSection = ini.get(channelName);

        if (channelSection == null)
            return;

        Ini.Section rulesChannelSection = channelSection.getChild("rules");

        List<String> channelRules = rulesChannelSection.getAll("rule"); //TODO: check not-null

        if (channelRules == null)
            channelRules = new ArrayList<>();

        Ini.Section joinFloodControlSection = channelSection.getChild("JoinFloodControl");

        boolean joinFloodControl = joinFloodControlSection.get("enable", boolean.class);
        int joinFloodControlEventsNumber = -1;
        int joinFloodControlTimeFrame = -1;
        if (joinFloodControl){
            joinFloodControlEventsNumber = joinFloodControlSection.get("join number", int.class);
            joinFloodControlTimeFrame = joinFloodControlSection.get("time frame", int.class);
        }

        Ini.Section joinCloneControlSection = channelSection.getChild("JoinCloneControl");

        boolean joinCloneControl = joinCloneControlSection.get("enable", boolean.class);

        int joinCloneControlTimeFrame = -1;
        String joinCloneControlPattern = "";
        if (joinCloneControl){
            joinCloneControlTimeFrame = joinCloneControlSection.get("time frame", int.class);
            joinCloneControlPattern = joinCloneControlSection.getOrDefault("pattern", "");
        }

        Profile.Section parseLinksTitlesSection = channelSection.getChild("ParseLinksTitles");

        boolean parseLinksTitles = parseLinksTitlesSection.get("enable", boolean.class);

        channelConfigs.put(channelName, new ConfigurationChannel(
                joinFloodControl,
                joinFloodControlEventsNumber,
                joinFloodControlTimeFrame,
                joinCloneControl,
                joinCloneControlTimeFrame,
                joinCloneControlPattern,
                parseLinksTitles,
                channelRules));
    }

    private void parseLogging(Wini ini){
        Ini.Section channelsSection = ini.get("logging");

        this.logDriverConfiguration = new LogDriverConfiguration(
                channelsSection.getOrDefault("driver", ""),
                channelsSection.getOrDefault("file(s) location", ""),
                channelsSection.getOrDefault("MongoDB host:port", ""),
                channelsSection.getOrDefault("MongoDB DB table", ""),
                channelsSection.getOrDefault("MongoDB DB user", ""),
                channelsSection.getOrDefault("MongoDB DB password", "")
        );
    }
    //TODO: more validation
    private void validate() throws Exception{
        if (serverName.isEmpty())
            throw new Exception("Server not defined in configuration file.");

        if (serverPort <= 0 || serverPort > 65535)
            throw new Exception("Server port number cannot be less/equal zero or greater then 65535");

        if (userNick.isEmpty())
            throw new Exception("Configuration issue: no nickname specified. ");

        if (! userNickPass.isEmpty()) {
            if (userNickAuthStyle.isEmpty())
                throw new Exception("Configuration issue: password specified while auth method is not.");

            if ( ! userNickAuthStyle.equals("rusnet") && ! userNickAuthStyle.equals("freenode"))
                throw new Exception("Configuration issue: userNickAuthStyle could be freenode or rusnet.");
        }
    }
}
