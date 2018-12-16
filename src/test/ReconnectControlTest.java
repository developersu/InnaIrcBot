import InnaIrcBot.ReconnectControl;
import com.sun.org.apache.regexp.internal.RE;

public class ReconnectControlTest {
    public static void main(String args[]){
        ReconnectControl.register("testing");
        ReconnectControl.register("testing1");
        ReconnectControl.update("testing1", false);

        System.out.println(ReconnectControl.get("testing"));
        System.out.println(ReconnectControl.get("testing1"));
        System.out.println(ReconnectControl.get("wrong"));
    }
}
