package InnaIrcBot.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfigurationChannel {

    private HashMap<String, String[]> joinMap;
    private HashMap<String, String[]> msgMap;
    private HashMap<String, String[]> nickMap;

    private boolean joinFloodControl;
    private int joinFloodControlEvents;
    private int joinFloodControlTimeframe;

    private boolean joinCloneControl;
    private int joinCloneControlTimeframe;
    private String joinCloneControlPattern;

    private boolean parseLinksTitles;

    public ConfigurationChannel(
            boolean joinFloodControl,
            int joinFloodControlEvents,
            int joinFloodControlSeconds,
            boolean joinCloneControl,
            int joinCloneControlTimeframe,
            String joinCloneControlPattern,
            boolean parseLinksTitles,
            List<String> rules)
    {

        parseRules(rules);

        if (joinFloodControl)
            validateJoinFloodControl(joinFloodControlEvents, joinFloodControlSeconds);

        if (joinCloneControl)
            validateJoinCloneControl(joinCloneControlTimeframe, joinCloneControlPattern);

        this.parseLinksTitles = parseLinksTitles;
    }

    private void parseRules(List<String> rules){
        this.joinMap = new HashMap<>();
        this.msgMap = new HashMap<>();
        this.nickMap = new HashMap<>();

        for (String rule : rules){
            parseRule(rule);
        }
    }

    private void parseRule(String rule){
        String[] directive = rule.split("\t");

        if (isNotValidDirective(directive))
            return;

        switch (directive[0].toLowerCase()){
            case "join":
                joinMap.put(directive[1], Arrays.copyOfRange(directive, 2, directive.length));
                break;
            case "msg":
                msgMap.put(directive[1], Arrays.copyOfRange(directive, 2, directive.length));
                break;
            case "nick":
                nickMap.put(directive[1], Arrays.copyOfRange(directive, 2, directive.length));
                break;
        }
    }
    private boolean isNotValidDirective(String[] directive){
        return directive.length < 3 || directive[0] == null || directive[1] == null || directive[2] == null;
    }

    private void validateJoinFloodControl(int events, int timeFrame){
        if (events <= 0 && timeFrame <= 0) {
            System.out.println("Join Flood Control configuration issue: 'Join number' and 'time frame' should be greater than 0.");
            return;
        }
        joinFloodControl = true;
        joinFloodControlEvents = events;
        joinFloodControlTimeframe = timeFrame;
    }

    private void validateJoinCloneControl(int timeFrame, String pattern){
        if (timeFrame < 0) {
            System.out.println("Join Clone Control configuration issue: 'time frame' should be greater than 0");
            return;
        }
        joinCloneControl = true;
        joinCloneControlTimeframe = timeFrame;
        joinCloneControlPattern = pattern;
    }

    public HashMap<String, String[]> getJoinMap() { return joinMap; }
    public HashMap<String, String[]> getMsgMap() { return msgMap; }
    public HashMap<String, String[]> getNickMap() { return nickMap; }

    public boolean isJoinFloodControl() { return joinFloodControl; }
    public int getJoinFloodControlEvents() { return joinFloodControlEvents; }
    public int getJoinFloodControlTimeframe() { return joinFloodControlTimeframe; }

    public boolean isJoinCloneControl() { return joinCloneControl; }
    public int getJoinCloneControlTimeframe() { return joinCloneControlTimeframe; }
    public String getJoinCloneControlPattern() { return joinCloneControlPattern; }

    public boolean isParseLinksTitles() { return parseLinksTitles; }
}
