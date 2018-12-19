package InnaIrcBot.ProvidersConsumers;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;

public class StreamProvider {

    private static HashMap<String, OutputStreamWriter> srvStreamMap = new HashMap<>();
    //private static OutputStreamWriter streamWriter;

    public static synchronized void writeToStream(String server, String message){
        try {
            srvStreamMap.get(server).write(message+"\n");
            srvStreamMap.get(server).flush();
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
    }
}
