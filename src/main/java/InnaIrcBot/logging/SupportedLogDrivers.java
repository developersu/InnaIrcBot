package InnaIrcBot.logging;

import java.util.ArrayList;
import java.util.List;

public class SupportedLogDrivers{
    public static final String files = "files";
    public static final String sqlite = "sqlite";
    public static final String mongodb = "mongodb";
    public static final String zero = "zero";

    private static final List<String> supportedLogDrivers = new ArrayList<>();

    static {
        supportedLogDrivers.add(files);
        supportedLogDrivers.add(sqlite);
        supportedLogDrivers.add(mongodb);
        supportedLogDrivers.add(zero);
    }

    public static boolean contains(String driverName){
        return supportedLogDrivers.contains(driverName);
    }
}