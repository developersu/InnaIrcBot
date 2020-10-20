package InnaIrcBot.config;

public class ConfigurationFile {
    private String serverName;
    private int serverPort;
    private String serverPass;
    private String[] channels;
    private String userNick;
    private String userIdent;
    private String userRealName;
    private String userNickPass;
    private String userNickAuthStyle;
    private String userMode;
    private boolean rejoinOnKick;
    private String logDriver;
    private String[] logDriverParameters;
    private String botAdministratorPassword;
    private String chanelConfigurationsPath;
    private String applicationLogDir;

    public ConfigurationFile(String serverName,
                             int serverPort,
                             String serverPass,
                             String[] channels,
                             String userNick,
                             String userIdent,
                             String userRealName,
                             String userNickPass,
                             String userNickAuthStyle,
                             String userMode,
                             boolean rejoinOnKick,
                             String logDriver,
                             String[] logDriverParameters,
                             String botAdministratorPassword,
                             String chanelConfigurationsPath,
                             String applicationLogDir){
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.serverPass = serverPass;
        this.channels = channels;
        this.userIdent = userIdent;
        this.userNick = userNick;
        this.userRealName = userRealName;
        this.userNickPass = userNickPass;
        this.userNickAuthStyle = userNickAuthStyle;
        this.userMode = userMode;
        this.rejoinOnKick = rejoinOnKick;
        this.logDriver = logDriver;
        this.logDriverParameters = logDriverParameters;
        this.botAdministratorPassword = botAdministratorPassword;
        this.chanelConfigurationsPath = chanelConfigurationsPath;
        this.applicationLogDir = applicationLogDir;
    }

    public String getServerName() { return nonNullString(serverName); }
    public int getServerPort() { return serverPort; }
    public String getServerPass() { return nonNullString(serverPass); }
    public String[] getChannels() { return channels; }
    public String getUserNick() { return nonNullString(userNick); }
    public String getUserIdent() { return nonNullString(userIdent); }
    public String getUserRealName() { return nonNullString(userRealName); }
    public String getUserNickPass() { return nonNullString(userNickPass); }
    public String getUserNickAuthStyle() { return nonNullString(userNickAuthStyle); }
    public String getUserMode() { return nonNullString(userMode); }
    public boolean getRejoinOnKick() { return rejoinOnKick; }
    public String getLogDriver() { return nonNullString(logDriver); }
    public String[] getLogDriverParameters() { return logDriverParameters; }
    public String getBotAdministratorPassword() { return nonNullString(botAdministratorPassword); }
    public String getChanelConfigurationsPath() { return nonNullString(chanelConfigurationsPath); }
    public String getApplicationLogDir() { return nonNullString(applicationLogDir); }

    public void setUserNickAuthStyle(String userNickAuthStyle) {
        this.userNickAuthStyle = userNickAuthStyle;
    }

    private String nonNullString(String value){
        return value == null ? "" : value;
    }
}
