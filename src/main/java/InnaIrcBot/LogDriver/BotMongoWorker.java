package InnaIrcBot.LogDriver;

import com.mongodb.*;
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

    private boolean isItSystemThread = false;

    public BotMongoWorker(String ircServer, String[] driverParameters, String channel){
        this.ircServer = ircServer;

        String mongoHostAddr;
        String mongoDBName;
        String mongoUser;
        String mongoPass;
        if (driverParameters.length >= 2 && !driverParameters[0].isEmpty() && !driverParameters[1].isEmpty()) {
            mongoHostAddr = driverParameters[0];
            mongoDBName = driverParameters[1];
            if (driverParameters.length == 4 && !driverParameters[2].isEmpty() && !driverParameters[3].isEmpty()) {
                mongoUser = driverParameters[2];
                mongoPass = driverParameters[3];
            }
            else {  // Consider that DB does not require auth, therefore any credentials are fine, since not null ;)
                mongoUser = "anon";
                mongoPass = "anon";
            }
        }
        else
            return; // consistent = false

        if (channel.equals("system"))       // Set ircServer variable only if it's 'system' log thread.
            this.isItSystemThread = true;

        if (!serversMap.containsKey(ircServer)){
            /*      // Leave this validations for better times.
            CommandListener mongoCommandListener = new CommandListener() {
                @Override
                public void commandStarted(CommandStartedEvent commandStartedEvent) {
                    System.out.println("BotMongoWorker (@"+this.ircServer+"): C: commandStarted");
                }

                @Override
                public void commandSucceeded(CommandSucceededEvent commandSucceededEvent) {
                    System.out.println("BotMongoWorker (@"+this.ircServer+"): C: commandSucceeded");
                }

                @Override
                public void commandFailed(CommandFailedEvent commandFailedEvent) {
                    System.out.println("BotMongoWorker (@"+this.ircServer+"): C: commandFailed");
                    //consistent = false;
                    //close(ircServer);           // ircServer recieved by constructor, not this.ircServer
                }
            };
            */
            ServerListener mongoServerListener = new ServerListener() {
                @Override
                public void serverOpening(ServerOpeningEvent serverOpeningEvent) {
                    System.out.println("BotMongoWorker (@"+ircServer+"): ServerListener: Server opened successfully: "+serverOpeningEvent.getServerId());
                }

                @Override
                public void serverClosed(ServerClosedEvent serverClosedEvent) {
                    System.out.println("BotMongoWorker (@"+ircServer+"): ServerListener: Server has been closed");
                }

                @Override
                public void serverDescriptionChanged(ServerDescriptionChangedEvent serverDescriptionChangedEvent) {
                    if (!serverDescriptionChangedEvent.getNewDescription().isOk()) {
                        consistent = false;
                        close(ircServer);           // ircServer recieved by constructor, not this.ircServer
                        System.out.println("BotMongoWorker (@"+ircServer+"): ServerListener: Server description changed (exception occurs): "
                                + serverDescriptionChangedEvent.getNewDescription().getException());
                    }
                }
            };

            MongoClientSettings MCS = MongoClientSettings.builder()
            //      .addCommandListener(mongoCommandListener)
                    .applyConnectionString(new ConnectionString("mongodb://"+mongoHostAddr))
                    .applyToServerSettings(builder -> builder.addServerListener(mongoServerListener))
                    .credential(MongoCredential.createCredential(mongoUser, mongoDBName, mongoPass.toCharArray()))
                    .build();

            MongoClient mongoClient = MongoClients.create(MCS);
            serversMap.put(ircServer, mongoClient);
        }

        MongoDatabase mongoDB = serversMap.get(ircServer).getDatabase(mongoDBName);
        collection = mongoDB.getCollection(ircServer + channel);

        Document ping = new Document("ping", "1");
        try {
            collection.insertOne(ping);
            consistent = true;          // if no exceptions, then true
        } catch (MongoCommandException mce){
            System.out.println("BotMongoWorker (@"+this.ircServer+"): Command exception. Check if username/password set correctly.");
            consistent = false;
            close(ircServer);           // ircServer received by constructor, not this.ircServer
        } catch (MongoTimeoutException mte) {
            System.out.println("BotMongoWorker (@"+this.ircServer+"): Timeout exception");
            consistent = false;
            close(ircServer);           // ircServer received by constructor, not this.ircServer
        }catch (MongoException me){
            System.out.println("BotMongoWorker (@"+this.ircServer+"): MongoDB Exception");
            consistent = false;
            close(ircServer);           // ircServer received by constructor, not this.ircServer
        } catch (IllegalStateException ise){
            System.out.println("BotMongoWorker (@"+this.ircServer+"): Illegal state exception: MongoDB server already closed (not an issue).");
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
        collection.insertOne(document);                                                                 // TODO: try/catch and watch
    }

    private long getDate(){ return System.currentTimeMillis() / 1000L; }  // UNIX time

    @Override
    public boolean isConsistent() { return consistent; }

    @Override
    public void close() {
        // If ircServer != null then it's system thread and when it's interrupted we have to close connection to DB for used server
        // And remove it from HashMap
        if (this.isItSystemThread && serversMap.containsKey(ircServer)) {
            serversMap.get(ircServer).close();
            serversMap.remove(ircServer);
            System.out.println("BotMongoWorker (@"+this.ircServer+")->close(): " + ircServer);
        }
    }
    public void close(String server) {
        if (serversMap.containsKey(server)) {
            serversMap.get(server).close();
            serversMap.remove(server);
            System.out.println("BotMongoWorker (@"+this.ircServer+")->close(): " + server + " (forced by listeners)");
        }
    }
}
