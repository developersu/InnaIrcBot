package InnaIrcBot.ProvidersConsumers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;

public class StreamProvider {

    private static final HashMap<String, OutputStreamWriter> srvStreamMap = new HashMap<>();

    public static synchronized void writeToStream(String server, String message){
        try {
            if (message.matches("(^.+?\\s)INNA")) {
                SystemConsumer.getSystemConsumer(server).add("INNA "+message);
                return;
            }
            srvStreamMap.get(server).write(message+"\n");
            srvStreamMap.get(server).flush();
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
    }
}
