package InnaIrcBot.Commanders;

import InnaIrcBot.ProvidersConsumers.StreamProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class JoinFloodHandlerTest {

    private static final String serverName = "testServer";
    private static final String channelName = "testChannel";

    private final JoinFloodHandler joinFloodHandler = new JoinFloodHandler(3, 5, serverName, channelName);
    private static final String userNickName = "John";
    private Thread socketTestThread;

    JoinFloodHandlerTest(){
        try{
            socketTestThread = new Thread(() -> {
                try{
                    ServerSocket serverSocket = new ServerSocket(60000);
                    serverSocket.accept();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            });
            socketTestThread.start();

            Socket testSocket = new Socket();
            testSocket.connect(new InetSocketAddress(60000));

            StreamProvider.setStream(serverName, testSocket);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    @DisplayName("JoinFloodHandler: timeout 5s & limit 3 attempts")
    @Test
    void track() throws Exception{
        assertNull(joinFloodHandler.users.get(userNickName));

        joinFloodHandler.track(userNickName);
        Thread.sleep(1000);
        joinFloodHandler.track(userNickName);
        Thread.sleep(2000);
        joinFloodHandler.track(userNickName);
        Thread.sleep(1990);
        joinFloodHandler.track(userNickName);

        assertNull(joinFloodHandler.users.get(userNickName));

        Thread.sleep(900);
        joinFloodHandler.track(userNickName);

        assertTrue(joinFloodHandler.users.containsKey(userNickName));
    }
/*
    @AfterAll
    static void cleanup(){
        socketTestThread.interrupt();
    }
 */
}