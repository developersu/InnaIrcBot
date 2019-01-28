# InnaIrcBot

Another one IRC bot in deep-deep beta.

## Usage
`  	-c, --configuration <name.config> [<name1.config> ...]	Read Config`

` 	-g, --generate	[name.config]		Generate Config`

` 	-v, --version						Get application version`
#### Configuration settings
"userNickAuthStyle": "rusnet" or "freenode"
* rusnet - send '/nickserv IDENTIFY mySecretPass'
* freenode - send '/msg nickserv IDENTIFY mySecretPass'

"logDriver" could be "Files", "SQLite" or "Zero"
* Files - log everything to files using /yourPathSet/serverName/#chanelName_YYYY-MM-DD.txt format.
* SQLite - use /yourPathSet/server.db (or /yourPathSet/yourFileName.db) sqlite file.
* MongoDB - write files to MongoDB. See ConfigurationExamples folder.

## License
Source code spreads under the GNU General Public License v3 or higher. Please see LICENSE file.

Used libraries:
* GSON: https://github.com/google/gson
* sqliteJDBC: https://bitbucket.org/xerial/sqlite-jdbc
* mongodb-driver-sync: https://mongodb.github.io/mongo-java-driver/3.9/


## TODO:
- [ ] Documentation
- [ ] Code refactoring
- [ ] QA: good regression testing
- [x] CI/CD Jenkins
- [x] Suppress messages from server or handle them separately from selected worker
- [ ] Logs backend workers as threads (SQLite and co. are too slow)
- [x] Logs backend worker for mongodb 
- [ ] Logs backend worker for redis/redis node
- [ ] Re-implement connection routine
- [ ] Availability to run scripts @ 'ChanelCommander' 
- [ ] Docker(+compose) package
- [ ] Flood tracker
- [ ] Deep configuration files validation
- [x] Maven ~~or Gradle~~ build
- [ ] ncurses-like or/and GUI configuration files (server/chanel setting) editor
- [x] CTCP support for using @ 'ChanelCommander'
- [ ] Access roles support (i.e. delegating some rights to another users)
- [ ] Logs for application (partly implemented)