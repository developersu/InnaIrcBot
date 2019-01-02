/**
 * InnaIrcBot
 * @author Dmitry Isaenko
 * Russia, 2018.
 * */
package InnaIrcBot;

import InnaIrcBot.Config.StorageReader;

public class BotStart {
//TODO: Steam link, flood control, kikbana
//TODO: setDaemon(true)
//TODO: multiple connections to one server not allowed

    public static void main(String[] args){
        if (args.length != 0) {
            if (args.length >= 2) {
                if (args[0].equals("--configuration") || args[0].equals("-c")) {
                    new Connections(args);
                } else if (args[0].equals("--generate") || args[0].equals("-g")) {
                    StorageReader.generateDefaultConfig(args[1]);
                } else if (args[0].equals("--version") || args[0].equals("-v")) {
                    System.out.println(GlobalData.getAppVersion());
                }
            }
            else if (args[0].equals("--generate") || args[0].equals("-g")){
                StorageReader.generateDefaultConfig(null);
            }
        }
        else {
            System.out.println("Usage:\n"
                    +" \t-c, --configuration <name.config> [<name1.config> ...]\tRead Config\n"
                    +"\t-g, --generate\t[name.config]\t\t\t\tGenerate Config\n"
                    +"\t-v, --version\t\t\t\t\t\tGet application version");
        }
    }
}