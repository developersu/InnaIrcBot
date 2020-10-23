package InnaIrcBot.logging;

import InnaIrcBot.config.LogDriverConfiguration;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.io.File;
import java.sql.*;

public class WorkerSQLite implements Worker {

    private Connection connection;
    private boolean consistent = false;
    private PreparedStatement preparedStatement;

    private final String server;
/**
 * Don't even think of changing this balalaika.
 * */
    public WorkerSQLite(String server, LogDriverConfiguration logDriverConfiguration, String channel) throws Exception{        // TODO: threads on SQLite level // remember: One file one DB
        this.server = server;
        String dbFileLocation = logDriverConfiguration.getPath();
        File dir = new File(dbFileLocation);
        dir.mkdirs();   // ignore result, because if it's already exists we're good. Otherwise, it will be created. Only issue that can occur is SecurityException thrown, so let's catch it.

        if (!dir.exists()) {
            throw new Exception("BotSQLiteWorker (@"+server+")->constructor(): Failure:\n\tUnable to create directory to store DB file: " + dbFileLocation);
        }
        String connectionURL;
        if (dbFileLocation.endsWith(File.separator))
            connectionURL = "jdbc:sqlite:"+dbFileLocation+server+".db";
        else
            connectionURL = "jdbc:sqlite:"+dbFileLocation+File.separator+server+".db";

        channel = channel.trim().replaceAll("\"","\\\"");           // TODO: use trim in every driver/worker?

        SQLiteConfig sqlConfig = new SQLiteConfig();
        sqlConfig.setOpenMode(SQLiteOpenMode.NOMUTEX);      //SQLITE_OPEN_NOMUTEX : multithreaded mode

        this.connection = DriverManager.getConnection(connectionURL, sqlConfig.toProperties());
        if (connection != null){
            // Create table if not created
            Statement statement = connection.createStatement();
            String query = "CREATE TABLE IF NOT EXISTS \""+channel+"\" ("
                    + "	id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + "	unixtime INTEGER,"
                    + "	event TEXT,"
                    + "	subject TEXT,"
                    + "	message TEXT,"
                    + "	object TEXT"
                    +");";
            statement.executeUpdate(query);

            // Check table representation
            ResultSet rs = statement.executeQuery("PRAGMA table_info(\""+channel+"\");");  // executeQuery never null
            boolean[] schemaResultCheck = {false, false, false, false, false, false};

            while (rs.next()) {
                switch (rs.getInt("cid")) {
                    case 0:
                        if (rs.getString("name").equals("id")
                                && rs.getString("type").equals("INTEGER")
                                && (rs.getInt("notnull") == 1)
                                && (rs.getString("dflt_value") == null)
                                && (rs.getInt("pk") == 1))
                            schemaResultCheck[0] = true;
                        //System.out.println("Got 0");
                        break;
                    case 1:
                        if (rs.getString("name").equals("unixtime")
                                && rs.getString("type").equals("INTEGER")
                                && (rs.getInt("notnull") == 0)
                                && (rs.getString("dflt_value") == null)
                                && (rs.getInt("pk") == 0))
                            schemaResultCheck[1] = true;
                        //System.out.println("Got 1");
                        break;
                    case 2:
                        if (rs.getString("name").equals("event")
                                && rs.getString("type").equals("TEXT")
                                && (rs.getInt("notnull") == 0)
                                && (rs.getString("dflt_value") == null)
                                && (rs.getInt("pk") == 0))
                            schemaResultCheck[2] = true;
                        //System.out.println("Got 2");
                        break;
                    case 3:
                        if (rs.getString("name").equals("subject")
                                && rs.getString("type").equals("TEXT")
                                && (rs.getInt("notnull") == 0)
                                && (rs.getString("dflt_value") == null)
                                && (rs.getInt("pk") == 0))
                            schemaResultCheck[3] = true;
                        //System.out.println("Got 3");
                        break;
                    case 4:
                        if (rs.getString("name").equals("message")
                                && rs.getString("type").equals("TEXT")
                                && (rs.getInt("notnull") == 0)
                                && (rs.getString("dflt_value") == null)
                                && (rs.getInt("pk") == 0))
                            schemaResultCheck[4] = true;
                        //System.out.println("Got 4");
                        break;
                    case 5:
                        if (rs.getString("name").equals("object")
                                && rs.getString("type").equals("TEXT")
                                && (rs.getInt("notnull") == 0)
                                && (rs.getString("dflt_value") == null)
                                && (rs.getInt("pk") == 0))
                            schemaResultCheck[5] = true;
                        //System.out.println("Got 5");
                        break;
                    default:
                        for (int i = 0; i <= 5; i++) {
                            schemaResultCheck[i] = false;       // If more then 5 elements, ruin results
                        }
                }
            }
                // Validating result: it table in DB have expected schema. If not, removing and recreating table.
            for (boolean element: schemaResultCheck) {
                if (!element) {
                    System.out.println("BotSQLiteWorker (@"+server+")->Constructor(): Notice:\n\tFound already existing table for channel with incorrect syntax: removing table and re-creating.");
                    statement.executeUpdate("DROP TABLE \"" + channel + "\";");
                    statement.executeUpdate(query);
                    break;
                }
            }
            this.consistent = true;

            this.preparedStatement = connection.prepareStatement(
                    "INSERT INTO \""+channel
                    +"\" (unixtime, event, subject, message, object) "
                    +"VALUES (?, ?, ?, ?, ?);");
        }
        else {
            System.out.println("BotSQLiteWorker (@"+server+")->constructor() failed:\n\t Connection to SQLite not established.");
            this.consistent = false;
        }

    }
    private long getDate(){
        return System.currentTimeMillis() / 1000L;                      // UNIX time
    }
    @Override
    public boolean isConsistent() {return consistent; }

    @Override
    public boolean logAdd(String event, String initiator, String message) {
        try {
            preparedStatement.setLong(1, getDate());
            preparedStatement.setString(2, event);
            preparedStatement.setString(3, initiator);
            switch (event) {
                case "PART":
                case "QUIT":
                case "TOPIC":
                    preparedStatement.setString(4, message.replaceAll("^.+?:", ""));
                    preparedStatement.setString(5, null);
                    break;
                case "MODE":
                    preparedStatement.setString(4, message.replaceAll("(^(.+?\\s){1})|(\\s.+$)",""));
                    preparedStatement.setString(5, message.replaceAll("^(.+?\\s){2}", ""));
                    break;
                case "KICK":
                    preparedStatement.setString(4, message.replaceAll("^.+?:", ""));
                    preparedStatement.setString(5, message.replaceAll("(^.+?\\s)|(\\s.+$)", ""));
                    break;
                case "PRIVMSG":
                    preparedStatement.setString(4, message.replaceAll("^:", ""));
                    preparedStatement.setString(5,null);
                    break;
                case "NICK":
                case "JOIN":
                default:
                    preparedStatement.setString(4, message);
                    preparedStatement.setString(5,null);
                    break;
            }
            preparedStatement.executeUpdate();
        }
        catch (Exception e) {
            System.out.println("BotSQLiteWorker (@" + server + ")->logAdd() failed:\n\t" + e.getMessage());
            this.close();
        }
        return consistent;
    }

    @Override
    public void close() {
        try {
            //System.out.println("SQLite drier closed");
            this.connection.close();
        }
        catch (SQLException | NullPointerException e){      //todo: consider redo
                System.out.println("BotSQLiteWorker (@"+ server +")->close() failed:\n\t" + e);      // nothing to do here
        }
        this.consistent = false;
    }
}