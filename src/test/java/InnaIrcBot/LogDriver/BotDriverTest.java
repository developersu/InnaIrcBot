package InnaIrcBot.LogDriver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class BotDriverTest {
    @TempDir
    Path mainLogsDir,
        mainSQLiteLogsDir;

    private static final String serverNameFiles = "irc.example.com";
    private static final String serverNameSQLite = "irc2.example.com";
    private Worker fw1;
    private Worker fw2;
    private Worker fw3;

    @DisplayName("BotDriver: test files driver")
    @Test
    void driverFilesTest() {
        assertTrue(this::initializeFilesLogDriver);
        createWorkersForFiles();
        checkConsistency();
        checkFilesWorkers();
        validateDriver();
        checkFilesWorkers();
        close();
    }
    private void createWorkersForFiles(){
        fw1 = BotDriver.getWorker(serverNameFiles,"system");
        fw2 = BotDriver.getWorker(serverNameFiles,"#main");
        fw3 = BotDriver.getWorker(serverNameFiles,"#lpr");
    }

    @DisplayName("BotDriver: test SQLite driver")
    @Test
    void driverSQLiteTest() {
        assertTrue(this::initializeSQLiteLogDriver);
        createWorkersForSQLite();
        checkConsistency();
        checkSQLiteWorkers();
        validateDriver();
        checkSQLiteWorkers();
        close();
    }
    private void createWorkersForSQLite(){
        fw1 = BotDriver.getWorker(serverNameSQLite,"system");
        fw2 = BotDriver.getWorker(serverNameSQLite,"#main");
        fw3 = BotDriver.getWorker(serverNameSQLite,"#lpr");
    }

    void checkConsistency(){
        assertTrue(fw1.isConsistent());
        assertTrue(fw2.isConsistent());
        assertTrue(fw3.isConsistent());
    }

    void checkFilesWorkers(){
        assertTrue(fw1 instanceof BotFilesWorker);
        assertTrue(fw2 instanceof BotFilesWorker);
        assertTrue(fw3 instanceof BotFilesWorker);
    }

    void checkSQLiteWorkers(){
        assertTrue(fw1 instanceof BotSQLiteWorker);
        assertTrue(fw2 instanceof BotSQLiteWorker);
        assertTrue(fw3 instanceof BotSQLiteWorker);
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

    private boolean initializeFilesLogDriver(){
        return BotDriver.setLogDriver(serverNameFiles, "files", new String[]{mainLogsDir.toString()}, "");
    }

    private boolean initializeSQLiteLogDriver(){
        return BotDriver.setLogDriver(serverNameSQLite, "sqlite", new String[]{mainSQLiteLogsDir.toString()}, "");
    }

    private void close(){
        fw1.close();
        fw2.close();
        fw3.close();
    }
}