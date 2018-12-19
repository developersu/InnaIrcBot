package InnaIrcBot.LogDriver;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.io.File;
import java.sql.*;

public class BotSQLiteWorker implements Worker {

    private Connection connection;
    private boolean consistent = false;
    private PreparedStatement preparedStatement;
/**
 * Don't even think of changing this balalaika.
 * */
    public BotSQLiteWorker(String server, String[] driverParameters,  String channel){    // TODO: threads on SQLite level // remember: One file one DB
        driverParameters[0] = driverParameters[0].trim();
        File dir = new File(driverParameters[0]);
        dir.mkdirs();
        if (!dir.exists()) {
            System.out.println("Unable to create directory to store DB file: " + driverParameters[0]);      //TODO: notify requester
            this.consistent = false;
        }
        String connectionURL;
        if (driverParameters[0].endsWith(File.separator))
            connectionURL = "jdbc:sqlite:"+driverParameters[0]+server+".db";
        else
            connectionURL = "jdbc:sqlite:"+driverParameters[0]+File.separator+server+".db";

        String safeChanName = channel.trim().replaceAll("\"","\\\"");           // TODO: use trim in every driver/worker?
        try {
            SQLiteConfig sqlConfig = new SQLiteConfig();
            sqlConfig.setOpenMode(SQLiteOpenMode.NOMUTEX);      //SQLITE_OPEN_NOMUTEX : multithreaded mode

            this.connection = DriverManager.getConnection(connectionURL, sqlConfig.toProperties());
            if (connection != null){
                // Create table if not created
                Statement statement = connection.createStatement();
                String query = "CREATE TABLE IF NOT EXISTS \""+safeChanName+"\" ("
                        + "	id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                        + "	unixtime INTEGER,"
                        + "	event TEXT,"
                        + "	subject TEXT,"
                        + "	message TEXT,"
                        + "	object TEXT"
                        +");";
                statement.executeUpdate(query);

                // Check table representation
                ResultSet rs = statement.executeQuery("PRAGMA table_info(\""+safeChanName+"\");");  // executeQuery never null
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
                        System.out.println("BotSQLiteWorker: Found already existing table for channel with incoorect syntax: removing table and re-creating.");
                        statement.executeUpdate("DROP TABLE \"" + safeChanName + "\";");
                        statement.executeUpdate(query);
                        break;
                    }
                }
                this.consistent = true;

                this.preparedStatement = connection.prepareStatement(
                        "INSERT INTO \""+safeChanName
                        +"\" (unixtime, event, subject, message, object) "
                        +"VALUES (?, ?, ?, ?, ?);");
            }
            else {
                this.consistent = false;
            }
        }
        catch (SQLException e){
            System.out.println("Internal issue: BotSQLiteWorker->constructor() failed\n\t"+e);
            this.consistent = false;
        }

    }
    private long getDate(){
        return System.currentTimeMillis() / 1000L;                      // UNIX time
    }
    @Override
    public boolean isConsistent() {return consistent; }

    @Override
    public void logAdd(String event, String initiatorArg, String messageArg) {
        try {
            preparedStatement.setLong(1, getDate());
            preparedStatement.setString(2, event);
            preparedStatement.setString(3, initiatorArg);
            switch (event) {
                case "NICK":
                case "JOIN":
                    preparedStatement.setString(4, messageArg);
                    preparedStatement.setString(5, null);
                    break;
                case "PART":
                case "QUIT":
                case "TOPIC":
                    preparedStatement.setString(4, messageArg.replaceAll("^.+?:", ""));
                    preparedStatement.setString(5, null);
                    break;
                case "MODE":
                    preparedStatement.setString(4, messageArg.replaceAll("(^(.+?\\s){1})|(\\s.+$)",""));
                    preparedStatement.setString(5, messageArg.replaceAll("^(.+?\\s){2}", ""));
                    break;
                case "KICK":
                    preparedStatement.setString(4,messageArg.replaceAll("^.+?:", ""));
                    preparedStatement.setString(5,messageArg.replaceAll("(^.+?\\s)|(\\s.+$)", ""));
                    break;
                case "PRIVMSG":
                    preparedStatement.setString(4,messageArg.replaceAll("^:", ""));
                    preparedStatement.setString(5,null);
                    break;
                default:
                    preparedStatement.setString(4,messageArg);
                    preparedStatement.setString(5,null);
                    break;
            }
            preparedStatement.executeUpdate();
        }
        catch (SQLException e){
            System.out.println("Internal issue: BotSQLiteWorker->logAdd() failed\n\t"+e);
            this.consistent = false;
        }
    }

    @Override
    public void close() {
        try {
            //System.out.println("SQLite drier closed");
            this.connection.close();
        }
        catch (SQLException e){
                System.out.println("Internal issue: BotSQLiteWorker->close() failed\n\t" + e);
        }
    }
}