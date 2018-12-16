package InnaIrcBot.Config;

public class StorageFile {
    private final String serverName;
    private final int serverPort;
    private final String[] channels;
    private final String userNick;
    private final String userIdent;
    private final String userRealName;
    private final String userNickPass;
    private final String userNickAuthStyle;
    private final String userMode;
    private final boolean rejoinOnKick;
    private final String logDriver;
    private final String[] logDriverParameters;
    private final String botAdministratorPassword;
    private final String chanelConfigurationsPath;

    public String getServerName() { return serverName; }
    public int getServerPort() { return serverPort; }
    public String[] getChannels() { return channels; }
    public String getUserNick() { return userNick; }
    public String getUserIdent() { return userIdent; }
    public String getUserRealName() { return userRealName; }
    public String getUserNickPass() { return userNickPass; }
    public String getUserNickAuthStyle() { return userNickAuthStyle; }
    public String getUserMode() { return userMode; }
    public boolean getRejoinOnKick() { return rejoinOnKick; }
    public String getLogDriver() { return logDriver; }
    public String[] getLogDriverParameters() { return logDriverParameters; }
    public String getBotAdministratorPassword() { return botAdministratorPassword; }
    public String getChanelConfigurationsPath() { return chanelConfigurationsPath; }

    public StorageFile(String serverName,
                int serverPort,
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
                String chanelConfigurationsPath){
        this.serverName = serverName;
        this.serverPort = serverPort;
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
    }
}
