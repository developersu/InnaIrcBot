package InnaIrcBot.logging;

import InnaIrcBot.config.LogDriverConfiguration;
import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.*;
import org.bson.Document;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** For each IRC server we create one DB that stored in the hashmap ('static', since we may have few configuration that have to work at once)
 * For each channel we store collection that have name of the ircServer + chanelName.
 * If user decides to use one MongoDB for various servers, he may use even one DB (declared in configuration file) and store collections in there.
 * **/
public class WorkerMongoDB implements Worker {               //TODO consider skipping checks if server already added.
    private final static Map<String, MongoClient> serversMap = Collections.synchronizedMap(new HashMap<>());
    private final String server;
    private final MongoCollection<Document> collection;
    private boolean consistent;

    public WorkerMongoDB(String server, LogDriverConfiguration logDriverConfiguration, String channel) throws Exception{
        this.server = server;

        String mongoHostAddress = logDriverConfiguration.getMongoURI();
        String mongoDBName = logDriverConfiguration.getMongoTable();
        String mongoUser = logDriverConfiguration.getMongoUser();
        String mongoPass = logDriverConfiguration.getMongoPassword();

        if (mongoUser.isEmpty()) {// Consider that DB does not require auth, therefore any credentials are fine, since not null ;)
            mongoUser = "anon";
            mongoPass = "anon";
        }

        if (! serversMap.containsKey(server)){
            /*      // Leave this validations for better times.
            CommandListener mongoCommandListener = new CommandListener() {
                @Override
                public void commandStarted(CommandStartedEvent commandStartedEvent) {
                    System.out.println("BotMongoWorker (@"+this.server+"): C: commandStarted");
                }

                @Override
                public void commandSucceeded(CommandSucceededEvent commandSucceededEvent) {
                    System.out.println("BotMongoWorker (@"+this.server+"): C: commandSucceeded");
                }

                @Override
                public void commandFailed(CommandFailedEvent commandFailedEvent) {
                    System.out.println("BotMongoWorker (@"+this.server+"): C: commandFailed");
                    //consistent = false;
                    //close(server);           // server recieved by constructor, not this.server
                }
            };
            */
            ServerListener mongoServerListener = getServerListener();

            MongoClientSettings MCS = MongoClientSettings.builder()
            //      .addCommandListener(mongoCommandListener)
                    .applyConnectionString(new ConnectionString("mongodb://"+mongoHostAddress))
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(5, TimeUnit.SECONDS))
                    .applyToServerSettings(builder -> builder.addServerListener(mongoServerListener))
                    .credential(MongoCredential.createCredential(mongoUser, mongoDBName, mongoPass.toCharArray()))
                    .build();

            MongoClient mongoClient = MongoClients.create(MCS);
            serversMap.put(server, mongoClient);
        }

        MongoDatabase mongoDB = serversMap.get(server).getDatabase(mongoDBName);
        collection = mongoDB.getCollection(server + channel);

        Document ping = new Document("ping", 1);
        //
        Document answer = mongoDB.runCommand(ping);         // reports to monitor thread if some fuckups happens
        if (answer.get("ok") == null || (Double)answer.get("ok") != 1.0d){
            close(server);
            return;
        }
        consistent = true;

        setClosable();
    }

    private ServerListener getServerListener(){
        return new ServerListener() {
            @Override
            public void serverOpening(ServerOpeningEvent serverOpeningEvent) {
                System.out.println("BotMongoWorker (@"+server+"): ServerListener: Server opened successfully: "+serverOpeningEvent.getServerId());
            }

            @Override
            public void serverClosed(ServerClosedEvent serverClosedEvent) {
                System.out.println("BotMongoWorker (@"+server+"): ServerListener: Server has been closed");
            }

            @Override
            public void serverDescriptionChanged(ServerDescriptionChangedEvent serverDescriptionChangedEvent) {
                if (serverDescriptionChangedEvent.getNewDescription().isOk())
                    return;
                System.out.println("BotMongoWorker (@"+server+"): ServerListener: Server description changed (exception occurs): "
                        + serverDescriptionChangedEvent.getNewDescription().getException());
                close(server);           // server recieved by constructor, not this.server
            }
        };
    }

    private void setClosable(){
        if (! consistent)
            return;

        Closeable thing = () -> {
            if (serversMap.containsKey(server)) {
                serversMap.get(server).close();
                serversMap.remove(server);
            }
        };

        WorkerSystem.getSystemWorker(server).registerInSystemWorker(thing);
    }

    @Override
    public void logAdd(String event, String initiator, String message) throws Exception{
        Document document = new Document("date", getDate())
                                .append("event", event)
                                .append("initiator", initiator);
        switch (event) {
            case "PART":
            case "QUIT":
            case "TOPIC":
                document.append("message1", message.replaceAll("^.+?:", ""));
                break;
            case "MODE":
                document.append("message1",  message.replaceAll("(^(.+?\\s){1})|(\\s.+$)",""));
                document.append("message2",  message.replaceAll("^(.+?\\s){2}", ""));
                break;
            case "KICK":
                document.append("message1", message.replaceAll("^.+?:", ""));
                document.append("message2", message.replaceAll("(^.+?\\s)|(\\s.+$)", ""));
                break;
            case "PRIVMSG":
                document.append("message1", message.replaceAll("^:", ""));
                break;
            case "353":
                break;
            case "NICK":
            case "JOIN":
            default:
                document.append("message1", message);
                break;
        }

        insert(document);

        if (consistent)
            return;

        throw new Exception();
    }
    private void insert(Document document){
        try {
            collection.insertOne(document);                                                     // TODO: call finalize?
            consistent = true;          // if no exceptions, then true
        }catch (MongoException me){
            System.out.println("BotMongoWorker (@"+this.server +")->logAdd(): MongoDB Exception: "+me.getMessage());
            this.close();
        } catch (IllegalStateException ise){
            System.out.println("BotMongoWorker (@"+this.server +")->logAdd(): Illegal state exception: MongoDB server already closed (not an issue).");
            this.close();
        }
    }

    private long getDate(){ return System.currentTimeMillis() / 1000L; }  // UNIX time

    @Override
    public boolean isConsistent() { return consistent; }

    @Override
    public void close() {
        consistent = false;
    }
    private void close(String server) {
        if (serversMap.containsKey(server)) {
            serversMap.get(server).close();
            serversMap.remove(server);
            System.out.println("BotMongoWorker (@"+this.server +")->close() [forced by listeners]");
        }
        consistent = false;
    }
}