package InnaIrcBot.config;

import InnaIrcBot.logging.SupportedLogDrivers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationFileTest {
    ConfigurationFile config;
    
    ConfigurationFileTest(){
         config = new ConfigurationFile(
                null,
                -100,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                new String[]{null},
                null,
                null,
                null
        );
    }
    
    @Test
    void getServerName() {
    }

    @Test
    void getServerPort() {
    }

    @Test
    void getServerPass() {
    }

    @Test
    void getChannels() {
    }

    @Test
    void getUserNick() {
    }

    @Test
    void getUserIdent() {
    }

    @Test
    void getUserRealName() {
    }

    @Test
    void getUserNickPass() {
    }

    @Test
    void getUserNickAuthStyle() {
    }

    @Test
    void getUserMode() {
    }

    @Test
    void getRejoinOnKick() {
    }

    @Test
    void getLogDriverConfiguration() {
        assertEquals(config.getLogDriverConfiguration().getName(), SupportedLogDrivers.zero);
    }

    @Test
    void getBotAdministratorPassword() {
    }

    @Test
    void getChanelConfigurationsPath() {
    }

    @Test
    void getApplicationLogDir() {
    }

    @Test
    void setUserNickAuthStyle() {
    }
}