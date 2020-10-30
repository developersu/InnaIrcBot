package InnaIrcBot.logging;

import InnaIrcBot.config.LogDriverConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class WorkerFiles implements Worker {
    private final String channel;
    private String filePath;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private LocalDate dateOnFile;
    private FileWriter fileWriter;

    private boolean consistent;

    public WorkerFiles(String server, LogDriverConfiguration logDriverConfiguration, String channel){
        this.channel = channel.replaceAll(File.separator, ",");

        formatFilePath(server, logDriverConfiguration.getPath());

        try {
            createServerFolder();
            createFileWriter();
            consistent = true;
        } catch (Exception e){
            System.out.println("WorkerFiles (@"+server+")->constructor(): Failure:\n" + e.getMessage());
        }
    }

    private void formatFilePath(String server, String dirLocation){
        if (dirLocation.isEmpty())
            dirLocation = ".";

        if (! dirLocation.endsWith(File.separator))
            dirLocation += File.separator;

        this.filePath = dirLocation+server+File.separator;
    }
    
    private void createServerFolder() throws Exception{
        final File file = new File(filePath);

        if (file.exists()){
            if (file.isDirectory())
                return;
            else
                throw new Exception("WorkerFiles->createServerFolder() "+filePath+" is file while directory expected.");
        }

        if (file.mkdirs())
            return;

        throw new Exception("WorkerFiles->createServerFolder() Can't create directory: "+filePath);
    }

    private void resetFileWriter() throws IOException{
        fileWriter.close();
        createFileWriter();
    }
    private void createFileWriter() throws IOException{
        dateOnFile = LocalDate.now();
        File newFile = new File(filePath+channel+"_"+ dateOnFile +".txt");
        fileWriter = new FileWriter(newFile);
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
    public void logAdd(String event, String initiator, String message) throws Exception{
        switch (event){
            case "PRIVMSG":
                PRIVMSG(initiator, message);
                break;
            case "JOIN":
                JOIN(initiator, message);
                break;
            case "MODE":
                MODE(initiator, message);
                break;
            case "KICK":
                KICK(initiator, message);
                break;
            case "PART":
                PART(initiator, message);
                break;
            case "QUIT":
                QUIT(initiator, message);
                break;
            case "NICK":
                NICK(initiator, message);
                break;
            case "TOPIC":
                TOPIC(initiator, message);
                break;
            default:
                prettyPrint("["+LocalTime.now().format(dateFormat)+"] "+event+" "+initiator+" "+message+"\n");   // TODO: QA @ big data
                break;
        }
        if (consistent)
            return;

        throw new Exception();
    }
    private void prettyPrint(String string){
        try {
            if (LocalDate.now().isAfter(dateOnFile)) {
                resetFileWriter();
            }
            fileWriter.write(string);
            fileWriter.flush();
        } catch (Exception e){
            System.out.println("WorkerFiles->prettyPrint() failed\n" +
                    "\tUnable to write logs to " + this.filePath + " "+e.getMessage());
            close();
            consistent = false;
        }
    }
    private String getCurrentTimestamp(){
        return "["+LocalTime.now().format(dateFormat)+"]  ";
    }
    private String getUserNameOnly(String userNameFull){
        return userNameFull.replaceAll("!.+$", "");
    }
    private String getUserNameAndHost(String userNameFull){
        return userNameFull.replaceAll("!.+$", "")+" [!"+userNameFull.replaceAll("^.+!", "")+"] ";
    }

    private void PRIVMSG(String initiatorArg, String messageArg){
        String msg = messageArg.substring(messageArg.indexOf(":")+1);

        if (Pattern.matches("^\\u0001ACTION .+\\u0001", msg)){
            prettyPrint(getCurrentTimestamp()+getUserNameOnly(initiatorArg)+msg.replaceAll("(^\\u0001ACTION)|(\\u0001$)","")+"\n");
            return;
        }
        prettyPrint(getCurrentTimestamp()+"<"+getUserNameOnly(initiatorArg)+"> "+msg+"\n");
    }
    private void JOIN(String initiatorArg, String messageArg){
        prettyPrint(getCurrentTimestamp()+">>  "+getUserNameAndHost(initiatorArg)+"joined "+messageArg+"\n");
    }
    private void MODE(String initiatorArg, String messageArg){
        String initiatorChain;
        if (initiatorArg.contains("!"))
            initiatorChain = getUserNameAndHost(initiatorArg)+"set";
        else
            initiatorChain = initiatorArg+" set";

        prettyPrint(getCurrentTimestamp()+"-!- "+initiatorChain + messageArg.substring(messageArg.indexOf(" "))+"\n");
    }
    private void KICK(String initiatorArg, String messageArg){
        prettyPrint(getCurrentTimestamp()+"!<< "+messageArg.replaceAll("(^.+?\\s)|(\\s.+$)", "")+
                " kicked by "+getUserNameAndHost(initiatorArg)+"with reason: "+messageArg.replaceAll("^.+?:", "")+"\n");
    }
    private void PART(String initiatorArg, String messageArg){
        prettyPrint(getCurrentTimestamp()+"<<  "+getUserNameAndHost(initiatorArg)+"parted: "
                + messageArg.replaceAll("^.+?:","")+"\n");
    }
    private void QUIT(String initiatorArg, String messageArg){
        prettyPrint(getCurrentTimestamp()+"<<  "+getUserNameAndHost(initiatorArg)+"quit: "
                + messageArg.replaceAll("^.+?:","")+"\n");
    }
    private void NICK(String initiatorArg, String messageArg){
        prettyPrint(getCurrentTimestamp()+"-!- "+getUserNameAndHost(initiatorArg)+"changed nick to: "+messageArg+"\n");
    }
    private void TOPIC(String initiatorArg, String messageArg) {
        prettyPrint(getCurrentTimestamp()+"-!- "+getUserNameAndHost(initiatorArg)+"has changed topic to: "
                + messageArg.replaceAll("^.+?:", "")+"\n");
    }

    @Override
    public void close() {
        try {
            fileWriter.close();
        }
        catch (NullPointerException ignore) {}
        catch (IOException e){
            System.out.println("WorkerFiles->close() failed\n" +
                    "\tUnable to properly close file: "+this.filePath);        // Live with it.
        }
        this.consistent = false;
    }
}
