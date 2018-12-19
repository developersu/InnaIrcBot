package Temporary;

import InnaIrcBot.Commanders.JoinFloodHandler;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class brokenJoinFloodHandlerTest {

    public static void main(String[] args){
        new brokenJoinFloodHandlerTest();
        JoinFloodHandler jdh = new JoinFloodHandler(5, 5, "SRV_NAME", "#CHAN_NAME");
        System.out.println("Envents:\t5\n"
                +"Time Frame:\t5 sec\n"
                +"ServerName:\tSRV_NAME\n"
                +"Chanel:\t#CHAN_NAME\n");
        for (int i=0; i<40; i++) {
            System.out.println("Join for two users happened @"+ LocalDateTime.now());
            jdh.track("eblan");
            jdh.track("eban'ko");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
