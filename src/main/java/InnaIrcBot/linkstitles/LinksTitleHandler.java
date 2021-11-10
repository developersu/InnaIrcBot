package InnaIrcBot.linkstitles;

import InnaIrcBot.GlobalData;
import InnaIrcBot.ProvidersConsumers.StreamProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
// TODO: add timeout to constructor cooldown time etc.

class LinksTitleHandler implements Runnable{
    private static final int READ_TIMEOUT = 1000;

    private final BlockingQueue<LinksTitleRequest> queue;
    private LocalDateTime lastReplyTime;

    LinksTitleHandler(BlockingQueue<LinksTitleRequest> queue){
        this.queue = queue;
        this.lastReplyTime = LocalDateTime.now();
    }

    @Override
    public void run() {
        System.out.println("["+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] LinksTitleHandler thread started");// TODO:REMOVE DEBUG
        try {
            while (true) {
                LinksTitleRequest request = queue.take();
                String server = request.getServer();
                String channel = request.getChannel();
                String message = request.getMessage();

                if (isTooManyRequests())
                    continue;

                lastReplyTime = LocalDateTime.now();

                track(server, channel, message);
            }
        }
        catch (InterruptedException ignore){ }
        System.out.println("["+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"] LinksTitleHandler thread ended");// TODO:REMOVE DEBUG
    }

    private void track(String server, String channel, String message) {
        try {
            if (! message.contains("http"))
                return;

            int httpPosition = message.indexOf("http"); // TODO: fix http:// g asdasd https://sadasd.com/
            String link = message.substring(httpPosition).replaceAll("\\s.+$", "");

            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(READ_TIMEOUT);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                //System.out.println("reply "+connection.getResponseCode());
                return;
            }
            if (! connection.getContentType().contains("text/html")){
                return;
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );

            StringBuilder stringBuffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line);
                if (line.contains("</title>")) {
                    break;
                }
            }
            reader.close();
            connection.disconnect();
            line = stringBuffer.toString();
            int from = line.indexOf("<title")+7;
            int till = line.indexOf("</title>");
            String title = line.substring(from, till);
            title = title.substring(title.indexOf(">")+1);

            if (title.length() > 510)
                title = title.substring(0, 510);

            StreamProvider.writeToStream(server, "PRIVMSG "+ channel +" :\""+title+"\"");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean isTooManyRequests(){
        return lastReplyTime.isAfter(LocalDateTime.now().minusSeconds(GlobalData.LINKS_COOLDOWN_FRAME));
    }
}