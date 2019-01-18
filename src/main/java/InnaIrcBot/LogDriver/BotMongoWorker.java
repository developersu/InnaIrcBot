package InnaIrcBot.LogDriver;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.*;
import org.bson.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
/** For each IRC server we create one DB that stored in the hashmap ('static', since we may have few configuration that have to work at once)
 * For each channel we store collection that have name of the ircServer + chanelName.
 * If user decides to use one MongoDB for various servers, he may use even one DB (declared in configuration file) and store collections in there.
 * **/
public class BotMongoWorker implements Worker {

    private static Map<String, MongoClient> serversMap = Collections.synchronizedMap(new HashMap<String, MongoClient>());

    private String ircServer;
    private MongoCollection<Document> collection;
    private boolean consistent = false;  // TODO: clarify possible issues???

    public BotMongoWorker(String ircServer, String[] driverParameters, String channel){
        if (channel.equals("system"))       // Set ircServer variable only if it's 'system' log thread.
            this.ircServer = ircServer;
        else
            this.ircServer = null;

        if (!serversMap.containsKey(ircServer)){

            CommandListener mongoCommandListener = new CommandListener() {
                @Override
                public void commandStarted(CommandStartedEvent commandStartedEvent) {
                    System.out.println("C: commandStarted");
                }

                @Override
                public void commandSucceeded(CommandSucceededEvent commandSucceededEvent) {
                    System.out.println("C: commandSucceeded");
                }

                @Override
                public void commandFailed(CommandFailedEvent commandFailedEvent) {
                    System.out.println("C: commandFailed");
                    consistent = false;
                    close(ircServer);           // ircServer recieved by constructor, not this.ircServer
                }
            };

            ServerListener mongoServerListener = new ServerListener() {
                @Override
                public void serverOpening(ServerOpeningEvent serverOpeningEvent) {
                    System.out.println("BotMongoWorker: ServerListener: Server opened successfully: "+serverOpeningEvent.getServerId());
                }

                @Override
                public void serverClosed(ServerClosedEvent serverClosedEvent) {
                    System.out.println("BotMongoWorker: ServerListener: Server has been closed");
                }

                @Override
                public void serverDescriptionChanged(ServerDescriptionChangedEvent serverDescriptionChangedEvent) {
                    if (!serverDescriptionChangedEvent.getNewDescription().isOk()) {
                        consistent = false;
                        close(ircServer);           // ircServer recieved by constructor, not this.ircServer
                        System.out.println("BotMongoWorker: ServerListener: Server description changed (exception occurs): "
                                + serverDescriptionChangedEvent.getNewDescription().getException());
                    }
                }
            };

            MongoClientSettings MCS = MongoClientSettings.builder()
                    .addCommandListener(mongoCommandListener)
                    .applyConnectionString(new ConnectionString("mongodb://192.168.1.186:27017"))   // TODO: replace with driverParameters[0] - address
                    .applyToServerSettings(builder -> builder.addServerListener(mongoServerListener))
                    .build();

            MongoClient mongoClient = MongoClients.create(MCS);
            serversMap.put(ircServer, mongoClient);
        }

        MongoDatabase mongoDB = serversMap.get(ircServer).getDatabase("irc");    // TODO: replace with driverParameters[1] - DB NAME
        collection = mongoDB.getCollection(ircServer + channel);

        Document ping = new Document("ping", "1");
        try {
            collection.insertOne(ping);
            consistent = true;          // if no exceptions, then true
        } catch (MongoTimeoutException e) {
            System.out.println("BotMongoWorker: Timeout exception");
            consistent = false;
            close(ircServer);           // ircServer recieved by constructor, not this.ircServer
        } catch (IllegalStateException ise){
            System.out.println("BotMongoWorker: Illegal state exception: MongoDB server already closed (not an issue).");
            consistent = false;
        }
    }

    @Override
    public void logAdd(String event, String initiatorArg, String messageArg) {
        Document document = new Document("date", getDate())
                                .append("event", event)
                                .append("initiator", initiatorArg);
        switch (event) {
            case "NICK":
            case "JOIN":
                document.append("message1", messageArg);
                //preparedStatement.setString(5, null);
                break;
            case "PART":
            case "QUIT":
            case "TOPIC":
                document.append("message1", messageArg.replaceAll("^.+?:", ""));
                //preparedStatement.setString(5, null);
                break;
            case "MODE":
                document.append("message1",  messageArg.replaceAll("(^(.+?\\s){1})|(\\s.+$)",""));
                document.append("message2",  messageArg.replaceAll("^(.+?\\s){2}", ""));
                break;
            case "KICK":
                document.append("message1", messageArg.replaceAll("^.+?:", ""));
                document.append("message2", messageArg.replaceAll("(^.+?\\s)|(\\s.+$)", ""));
                break;
            case "PRIVMSG":
                document.append("message1", messageArg.replaceAll("^:", ""));
                //preparedStatement.setString(5,null);
                break;
            default:
                document.append("message1", messageArg);
                //preparedStatement.setString(5,null);
                break;
        }
        collection.insertOne(document);
    }

    private long getDate(){ return System.currentTimeMillis() / 1000L; }  // UNIX time

    @Override
    public boolean isConsistent() { return consistent; }

    @Override
    public void close() {
        // If ircServer != null then it's system thread and when it's interrupted we have to close connection to DB for used server
        // And remove it from HashMap
        if (this.ircServer != null && serversMap.containsKey(ircServer)) {
            serversMap.get(ircServer).close();
            serversMap.remove(ircServer);
            System.out.println("BotMongoWorker->close(): " + ircServer);
        }
    }
    public void close(String server) {
        if (serversMap.containsKey(server)) {
            serversMap.get(server).close();
            serversMap.remove(server);
            System.out.println("BotMongoWorker->close(): " + server + " (forced by listeners)");
        }
    }
}
