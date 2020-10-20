package InnaIrcBot.logging;

import InnaIrcBot.config.LogDriverConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class LogDriverTest {
    @TempDir
    Path mainLogsDir,
        mainSQLiteLogsDir;

    private static final String serverNameFiles = "files.example.com";
    private static final String serverNameSQLite = "sqlite.example.com";
    private static final String serverNameMongoDB = "mongo.example.com";
    private Worker fw1;
    private Worker fw2;
    private Worker fw3;

    @DisplayName("BotDriver: test files driver")
    @Test
    void driverFilesTest() {
        initializeFilesLogDriver();
        createWorkers(serverNameFiles);
        checkConsistency();
        checkFilesWorkers();
        validateDriver();
        checkFilesWorkers();
        close();
    }
    void checkFilesWorkers(){
        assertTrue(fw1 instanceof WorkerFiles);
        assertTrue(fw2 instanceof WorkerFiles);
        assertTrue(fw3 instanceof WorkerFiles);
    }

    @DisplayName("BotDriver: test SQLite driver")
    @Test
    void driverSQLiteTest() {
        initializeSQLiteLogDriver();
        createWorkers(serverNameSQLite);
        checkConsistency();
        checkSQLiteWorkers();
        validateDriver();
        checkSQLiteWorkers();
        close();
    }
    void checkSQLiteWorkers(){
        assertTrue(fw1 instanceof WorkerSQLite);
        assertTrue(fw2 instanceof WorkerSQLite);
        assertTrue(fw3 instanceof WorkerSQLite);
    }

    @Disabled("MongoDB connection/configuration example. Requires real MongdDB instance created & configured")
    @DisplayName("BotDriver: test MongoDB driver")
    @Test
    void driverMongoTest() {
        initializeMongoDBLogDriver();
        createWorkers(serverNameMongoDB);
        checkConsistency();
        checkMongoDBWorkers();
        validateDriver();
        checkMongoDBWorkers();
        close();
    }
    void checkMongoDBWorkers(){
        assertTrue(fw1 instanceof WorkerMongoDB);
        assertTrue(fw2 instanceof WorkerMongoDB);
        assertTrue(fw3 instanceof WorkerMongoDB);
    }

    private void createWorkers(String server){
        fw1 = LogDriver.getWorker(server,"system");
        fw2 = LogDriver.getWorker(server,"#main");
        fw3 = LogDriver.getWorker(server,"#lpr");
    }
    void checkConsistency(){
        assertTrue(fw1.isConsistent());
        assertTrue(fw2.isConsistent());
        assertTrue(fw3.isConsistent());
    }

    void validateDriver(){
        assertTrue(fw1.logAdd("JOIN", "de_su!loper@desktop.lan", "message1"));
        assertTrue(fw1.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": some text here"));
        assertTrue(fw1.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": more random tests"));
        assertTrue(fw1.logAdd("NICK", "de_su!loper@desktop.lan", "developer_su"));
        assertTrue(fw1.logAdd("MODE", "de_su!loper@desktop.lan", "+b username"));
        assertTrue(fw1.logAdd("PART", "de_su!loper@desktop.lan", "#chan1"));

        assertTrue(fw2.logAdd("JOIN", "de_su!loper@desktop.lan", "message2"));
        assertTrue(fw2.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": some text here"));
        assertTrue(fw2.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": more random tests"));
        assertTrue(fw2.logAdd("NICK", "de_su!loper@desktop.lan", "developer_su"));
        assertTrue(fw2.logAdd("MODE", "de_su!loper@desktop.lan", "+b username"));
        assertTrue(fw2.logAdd("PART", "de_su!loper@desktop.lan", "#chan2"));

        assertTrue(fw3.logAdd("JOIN", "de_su!loper@desktop.lan", "message3"));
        assertTrue(fw3.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": some text here"));
        assertTrue(fw3.logAdd("PRIVMSG", "de_su!loper@desktop.lan", ": more random tests"));
        assertTrue(fw3.logAdd("NICK", "de_su!loper@desktop.lan", "developer_su"));
        assertTrue(fw3.logAdd("MODE", "de_su!loper@desktop.lan", "+b username"));
        assertTrue(fw3.logAdd("PART", "de_su!loper@desktop.lan", "#chan3"));
    }

    private void initializeFilesLogDriver(){
        LogDriver.setLogDriver(serverNameFiles, new LogDriverConfiguration("FileS", new String[]{mainLogsDir.toString()}), "");
    }

    private void initializeSQLiteLogDriver(){
        LogDriver.setLogDriver(serverNameSQLite, new LogDriverConfiguration("SQliTe", new String[]{mainSQLiteLogsDir.toString()}), "");
    }

    private void initializeMongoDBLogDriver(){
        String[] params = new String[]{"192.168.1.186:27017",
                                        "irc",
                                        "loper",
                                        "password"};

        LogDriver.setLogDriver("irc.tomsk.net",new LogDriverConfiguration("MongoDB", params),"");
    }

    private void close(){
        fw1.close();
        fw2.close();
        fw3.close();
    }
}