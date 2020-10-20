package InnaIrcBot.ProvidersConsumers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

public class StreamProvider {

    private static final HashMap<String, OutputStreamWriter> srvStreamMap = new HashMap<>();
    private static final HashMap<String, BlockingQueue<String>> srvSysConsumersMap = new HashMap<>();

    public static synchronized void writeToStream(String server, String message){
        try {
            srvStreamMap.get(server).write(message+"\n");
            srvStreamMap.get(server).flush();
            // If this application says something, then pass it into system consumer thread to handle
            if (message.startsWith("PRIVMSG ")) {
                srvSysConsumersMap.get(server).add("INNA "+message);
            }
        } catch (IOException e){
            System.out.println("Internal issue: StreamProvider->writeToStream() caused I/O exception:\n\t"+e.getMessage());
        }
    }
    public static synchronized void setStream(String server, Socket socket) throws IOException{
        OutputStream outStream = socket.getOutputStream();
        srvStreamMap.put(server, new OutputStreamWriter(outStream));
    }
    public static synchronized void delStream(String server){
        srvStreamMap.remove(server);
        srvSysConsumersMap.remove(server);
    }

    public static synchronized void setSysConsumer(String server, BlockingQueue<String> queue){
        srvSysConsumersMap.put(server, queue);
    }
}
