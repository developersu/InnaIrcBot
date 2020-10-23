package InnaIrcBot.config;

import InnaIrcBot.logging.SupportedLogDrivers;

public class LogDriverConfiguration {
    private String name;

    private final String path;

    private final String mongoURI;
    private final String mongoTable;
    private String mongoUser;
    private String mongoPassword;

    public LogDriverConfiguration(
            String name,
            String path,
            String mongoURI,
            String mongoTable,
            String mongoUser,
            String mongoPassword)
    {
        this.name = name.toLowerCase();
        this.path = path;
        this.mongoURI = mongoURI;
        this.mongoTable = mongoTable;
        this.mongoUser = mongoUser;
        this.mongoPassword = mongoPassword;
        validateName();
        validateConfiguration();
    }
    private void validateName(){
        if (! SupportedLogDrivers.contains(name)) {
            name = SupportedLogDrivers.zero;
        }
    }

    private void validateConfiguration(){
        switch (this.name){
            case SupportedLogDrivers.files:
            case SupportedLogDrivers.sqlite:
                validatePath();
                break;
            case SupportedLogDrivers.mongodb:
                validateMongo();
                break;
        }
    }

    private void validatePath(){
        try {
            checkFieldNotEmpty(path);
        }
        catch (Exception e){
            name = SupportedLogDrivers.zero;
        }
    }

    private void validateMongo(){
        try {
            checkFieldNotEmpty(mongoURI);
            checkFieldNotEmpty(mongoTable);
            if (mongoUser == null)
                mongoUser = "";
            if (mongoPassword == null)
                mongoPassword = "";
        }
        catch (Exception e){
            name = SupportedLogDrivers.zero;
        }
    }
    private void checkFieldNotEmpty(String field) throws Exception{
        if (field == null || field.isEmpty()) {
            throw new Exception();
        }
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public String getMongoURI() { return mongoURI; }
    public String getMongoTable() { return mongoTable; }
    public String getMongoUser() { return mongoUser; }
    public String getMongoPassword() { return mongoPassword; }
}
