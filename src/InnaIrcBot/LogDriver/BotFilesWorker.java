package InnaIrcBot.LogDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class BotFilesWorker implements Worker {
    private String  filePath;
    private boolean consistent = false;
    private DateTimeFormatter dateFormat;

    private LocalDate fileWriterDay;
    private FileWriter fileWriter;

    public BotFilesWorker(String server, String[] driverParameters, String channel){
        if (System.getProperty("os.name").startsWith("Windows")){
            channel = channel.replaceAll("\",",",");
        }
        else {
            channel = channel.replaceAll("/",",");
        }

        driverParameters[0] = driverParameters[0].trim();       //Consider parameters[0] as dirLocation
        String dirLocation;
        if (driverParameters[0].endsWith(File.separator))
            dirLocation = driverParameters[0]+server;
        else
            dirLocation = driverParameters[0]+File.separator+server;
        File dir = new File(dirLocation);
        dir.mkdirs();
        if (!dir.exists()) {
            System.out.println("Unable to create directory to store files: " + dirLocation);      //TODO: notify requester
            this.consistent = false;
        }
        this.filePath = dirLocation+File.separator+channel;

        dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        if (resetFileWriter(false))
            this.consistent = true;
    }

    private boolean resetFileWriter(boolean reassign){
        try {
            if (reassign)
                this.fileWriter.close();
            this.fileWriterDay = LocalDate.now();
            this.fileWriter = new FileWriter(this.filePath+"_"+LocalDate.now().toString()+".txt", true);
            return true;
        }
        catch (java.io.IOException e){
            System.out.println("Internal issue: BotFilesWorker->constructor() can't create file to store logs: "+this.filePath);
            return false;
        }
    }
    @Override
    public boolean isConsistent() {
        return consistent;
    }

    /**
     * argument[0] should be always 'from whom'
     * argument[1] should be always 'subject'
     * */
    @Override
    public void logAdd(String event, String initiatorArg, String messageArg) {
        switch (event){
            case "PRIVMSG":
                PRIVMSG(initiatorArg, messageArg);
                break;
            case "JOIN":
                JOIN(initiatorArg, messageArg);
                break;
            case "MODE":
                MODE(initiatorArg, messageArg);
                break;
            case "KICK":
                KICK(initiatorArg, messageArg);
                break;
            case "PART":
                PART(initiatorArg, messageArg);
                break;
            case "QUIT":
                QUIT(initiatorArg, messageArg);
                break;
            case "NICK":
                NICK(initiatorArg, messageArg);
                break;
            case "TOPIC":
                TOPIC(initiatorArg, messageArg);
                break;
            default:
                this.prettyPrint("["+LocalTime.now().format(dateFormat)+"] "+event+" "+initiatorArg+" "+messageArg+"\n");   // TODO: QA @ big data
                break;
        }
    }
    @Override
    public void close() {
        try {
            fileWriter.close();
        }
        catch (java.io.IOException e){
            System.out.println("Internal issue: BotFilesWorker->close() failed\n\tUnable to properly close file: "+this.filePath);        // Live with it.
        }

    }
    private void prettyPrint(String string){
        if (LocalDate.now().isAfter(fileWriterDay))
            resetFileWriter(true);
        try {
            fileWriter.write(string);
            fileWriter.flush();
        } catch (IOException e) {
            System.out.println("Internal issue: BotFilesWorker->prettyPrint() failed\n\tUnable to write logs of "+this.filePath+" because of internal failure in LocalTime representation.");
            consistent = false;
        }
    }
    private String genDate(){
        return "["+LocalTime.now().format(dateFormat)+"]  ";
    }
    private String getUserNameOnly(String userNameFull){return userNameFull.replaceAll("!.+$", "");}
    private String getUserNameAndHost(String userNameFull){return userNameFull.replaceAll("!.+$", "")+" [!"+userNameFull.replaceAll("^.+!", "")+"] ";}

    private void PRIVMSG(String initiatorArg, String messageArg){
        String msg = messageArg.substring(messageArg.indexOf(":")+1);
        if (!Pattern.matches("^\\u0001ACTION .+\\u0001", msg)){
            this.prettyPrint(genDate()+"<"+getUserNameOnly(initiatorArg)+"> "+msg+"\n");
        }
        else {
            this.prettyPrint(genDate()+getUserNameOnly(initiatorArg)+msg.replaceAll("(^\\u0001ACTION)|(\\u0001$)","")+"\n");
        }
    }
    private void JOIN(String initiatorArg, String messageArg){
        this.prettyPrint(genDate()+">>  "+getUserNameAndHost(initiatorArg)+"joined "+messageArg+"\n");
    }
    private void MODE(String initiatorArg, String messageArg){
        String initiatorChain;
        if (initiatorArg.contains("!"))
            initiatorChain = getUserNameAndHost(initiatorArg)+"set";
        else
            initiatorChain = initiatorArg+" set";
        this.prettyPrint(genDate()+"-!- "+initiatorChain+messageArg.substring(messageArg.indexOf(" "))+"\n");
    }
    private void KICK(String initiatorArg, String messageArg){
        this.prettyPrint(genDate()+"!<< "+messageArg.replaceAll("(^.+?\\s)|(\\s.+$)", "")+
                " kicked by "+getUserNameAndHost(initiatorArg)+"with reason: "+messageArg.replaceAll("^.+?:", "")+"\n");
    }
    private void PART(String initiatorArg, String messageArg){
        this.prettyPrint(genDate()+"<<  "+getUserNameAndHost(initiatorArg)+"parted: "+messageArg.replaceAll("^.+?:","")+"\n");
    }
    private void QUIT(String initiatorArg, String messageArg){
        this.prettyPrint(genDate()+"<<  "+getUserNameAndHost(initiatorArg)+" quit: "+messageArg.replaceAll("^.+?:","")+"\n");
    }
    private void NICK(String initiatorArg, String messageArg){
        this.prettyPrint(genDate()+"-!- "+getUserNameAndHost(initiatorArg)+"changed nick to: "+messageArg+"\n");
    }
    private void TOPIC(String initiatorArg, String messageArg) {
        this.prettyPrint(genDate()+"-!- "+getUserNameAndHost(initiatorArg)+"has changed topic to: "+messageArg.replaceAll("^.+?:", "")+"\n");
    }
}
