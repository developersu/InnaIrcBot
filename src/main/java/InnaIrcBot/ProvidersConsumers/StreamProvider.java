package InnaIrcBot.ProvidersConsumers;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

public class StreamProvider {

    private static HashMap<String, OutputStreamWriter> srvStreamMap = new HashMap<>();
    private static HashMap<String, PrintWriter> srvSysConsumersMap = new HashMap<>();

    public static synchronized void writeToStream(String server, String message){
        try {
            srvStreamMap.get(server).write(message+"\n");
            srvStreamMap.get(server).flush();

            //System.out.println(message);
            // If this application says something, then pass it into system consumer thread to handle
            if (message.startsWith("PRIVMSG")) {
                srvSysConsumersMap.get(server).println("INNA "+message);
                srvSysConsumersMap.get(server).flush();
            }
        } catch (java.io.IOException e){
            System.out.println("Internal issue: StreamProvider->writeToStream() caused I/O exception.");
        }
    }
    public static synchronized boolean setStream(String server, Socket socket){
        try {
            OutputStream outStream = socket.getOutputStream();
            srvStreamMap.put(server, new OutputStreamWriter(outStream));
            return true;
        } catch (java.io.IOException e){
            System.out.println("Internal issue: StreamProvider->setStream() caused I/O exception.");
            return false;
        }
    }
    public static synchronized void delStream(String server){
        srvStreamMap.remove(server);
        srvSysConsumersMap.remove(server);
    }

    public static synchronized void setSysConsumer(String server, PrintWriter pw){
        srvSysConsumersMap.put(server, pw);
    }
}
