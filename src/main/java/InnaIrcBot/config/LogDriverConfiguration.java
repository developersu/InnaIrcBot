package InnaIrcBot.config;

import InnaIrcBot.logging.SupportedLogDrivers;

public class LogDriverConfiguration {
    private String name;
    private String[] params;

    public LogDriverConfiguration(String name, String[] params){
        this.name = name.toLowerCase();
        this.params = params;
        validateName();
        validateParams();
    }
    private void validateName(){
        if (! SupportedLogDrivers.contains(name)) {
            name = SupportedLogDrivers.zero;
        }
    }
    private void validateParams(){
        if (params == null) {
            name = SupportedLogDrivers.zero;
            return;
        }
        if (params.length == 0){
            name = SupportedLogDrivers.zero;
            return;
        }
        if (params[0] == null){
            name = SupportedLogDrivers.zero;
            return;
        }
        if (params[0].isEmpty()){
            name = SupportedLogDrivers.zero;
        }
    }

    public String getName() { return name; }
    public String[] getParams() { return params; }
}
