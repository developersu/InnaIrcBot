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
            srvStreamMap.get(server).write(message+"\n");
            srvStreamMap.get(server).flush();
            if (message.startsWith("PRIVMSG")) {
                SystemConsumer.getSystemConsumer(server).add("INNA "+message);
            }
        } catch (IOException e){
            System.out.println("Internal issue: StreamProvider->writeToStream() caused I/O exception:\n\t"+e.getMessage());
        } catch (NullPointerException npe){
            System.out.println("Internal issue: StreamProvider->writeToStream() caused NullPointerException exception:\n"
                    +"Server: "+server
                    +"\nMessage: "+message);
            npe.printStackTrace();
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
