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

    private String ircServer;       // hold for debug only

    public BotFilesWorker(String server, String[] driverParameters, String channel){
        ircServer = server;
        dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

        channel = channel.replaceAll(File.separator, ",");

        String dirLocation = driverParameters[0].trim();
        if (dirLocation.endsWith(File.separator))
            dirLocation = dirLocation+server;
        else
            dirLocation = dirLocation+File.separator+server;

        this.filePath = dirLocation+File.separator+channel;

        File dir = new File(dirLocation);
        try {
            dir.mkdirs();       // ignore result, because if it's already exists we're good. Otherwise, it will be created. Only issue that can occur is SecurityException thrown, so let's catch it.
        } catch (Exception e){
            System.out.println("BotFilesWorker (@"+server+")->constructor(): Failure:\n\tUnable to create directory to store DB file: \n\t" +e);
            return;     // consistent = false;
        }
        if (!dir.exists()) {
            System.out.println("BotFilesWorker (@"+server+")->constructor() failed:\n\tUnable to create directory to store files: " + dirLocation);      //TODO: notify requester
            return;
        }

        this.consistent = resetFileWriter(false);
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
            System.out.println("BotFilesWorker (@"+ircServer+")->resetFileWriter() failed:\n\tCan't create file to store logs: "+this.filePath);
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
    public boolean logAdd(String event, String initiatorArg, String messageArg) {
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
        return consistent;
    }
    private void prettyPrint(String string){
        //if (consistent) {                        // could be not-opened
            try {
                if (LocalDate.now().isAfter(fileWriterDay)) {
                    if (!resetFileWriter(true)) {
                        this.close();       // Error message already printed
                        return;
                    }
                }
                fileWriter.write(string);
                fileWriter.flush();
            } catch (IOException e) {
                System.out.println("BotFilesWorker (@" + ircServer + ")->prettyPrint() failed\n\tUnable to write logs of " + this.filePath + " because of internal failure in LocalTime representation.");
                this.close();
                //consistent = false;
            } catch (NullPointerException npe){
                System.out.println("BotFilesWorker (@" + ircServer + ")->prettyPrint() failed\n\tUnable to write logs of " + this.filePath + " because file descriptor already closed/was not opened.");
                consistent = false;
            } catch (Exception unknowne){     // ??? No ideas. Just in case. Consider removing.
                System.out.println("BotFilesWorker (@" + ircServer + ")->prettyPrint() failed\n\tUnable to write logs of " + this.filePath + " because of exception:\n\t"+unknowne);
                this.close();
            }
        //}
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

    @Override
    public void close() {
        try {
            if (fileWriter !=null)
                fileWriter.close();
        }
        catch (java.io.IOException e){
            System.out.println("BotFilesWorker (@"+ircServer+")->close() failed\n\tUnable to properly close file: "+this.filePath);        // Live with it.
        }
        this.consistent = false;
    }
}
