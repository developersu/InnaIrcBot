# InnaIrcBot

[![status-badge](https://ci.redrise.ru/api/badges/8/status.svg)](https://ci.redrise.ru/repos/8)

InnaIrcBot is IRC bot.

_Pet-project created to learn Java in action_ 

## License
Source code spreads under the GNU General Public License v3 or higher. Please see LICENSE file.

#### Used libraries:
* Apache commons CLI: https://commons.apache.org/proper/commons-cli/
* ini4j: http://ini4j.sourceforge.net/
* sqliteJDBC: https://bitbucket.org/xerial/sqlite-jdbc
* mongodb-driver-sync: https://mongodb.github.io/mongo-java-driver/3.9/
* JUnit 5: https://junit.org/junit5/

## Usage
``` 
java -jar InnaIrcBot.jar [OPTION]... [FILE]...
options:
 -c,--configuration <arg>   Start with configuration
 -g,--generate <arg>        Create configuration template
 -h,--help                  Show this help
 -v,--version               Show application version
```
#### Configuration notes
"nickserv auth method" could be either "rusnet" or "freenode" where:
* rusnet - send '/nickserv IDENTIFY mySecretPass'
* freenode - send '/msg nickserv IDENTIFY mySecretPass'

Section [logging] "driver" could be "files", "SQLite", "MongoDB" or "Zero"
* Files - log everything to files using /yourPathSet/serverName/#chanelName_YYYY-MM-DD.txt format.
* SQLite - use /yourPathSet/server.db (or /yourPathSet/yourFileName.db) sqlite file.
* MongoDB - write files to MongoDB. See ConfigurationExamples folder.
* Zero - do not use any

Running application with '-g' option would create 'file-driven' configuration. 

### TODO:
- [ ] Documentation
- [ ] QA: add good unit tests
- [ ] Logs backend workers as threads (SQLite and co. are too slow)
- [ ] Logs backend worker for redis/redis node
- [ ] Scripts support at 'ChanelCommander' 
- [ ] Docker(+compose) package
- [ ] Flood tracker
- [ ] ncurses-like or/and GUI configuration files (server/chanel setting) editor
- [ ] Access roles support (i.e. delegating some rights to another users)
- [ ] Logs for application (partly implemented)