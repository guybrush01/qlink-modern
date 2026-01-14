# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Q-Link Reloaded is a Java server implementation that recreates the Q-Link (Quantum Link) online service - the original AOL predecessor for the Commodore 64. It allows C64 users to connect for chat, gaming, and file transfers.

## Build Commands

```bash
# Build the project (creates executable JAR in target/)
mvn package

# Clean and rebuild
mvn clean package

# Build with local dependencies (runs bridge script first)
./package
```

The build produces `target/qlink-0.1.0.jar` using maven-shade-plugin to create a fat JAR.

## Database Setup

```bash
# Bootstrap database (WARNING: deletes existing database)
./bootstrap

# Or manually:
mysql -uroot < ./dev_privileges.sql      # Creates 'qlink' DB and 'qlinkuser'
mysql -uroot -Dqlink < ./schema.sql      # Loads main schema
mysql -uroot -Dqlink < ./skern.sql       # Applies updates
```

## Running the Server

```bash
java -jar target/qlink-0.1.0.jar [options]

# Options:
#   -configFile <file>    Configuration file
#   -qtcpPort <port>      QTCP port (default 5190)
#   -habilinkPort <port>  Habilink port (default 1986)
```

Configuration can be overridden via environment variables: `QLINK_DB_JDBC_URI`, `QLINK_DB_USERNAME`, `QLINK_DB_PASSWORD`

## Architecture

### Core Components

- **QLinkServer** (`org.jbrain.qlink.QLinkServer`) - Main server hub managing sessions and protocol listeners
- **QSession** (`org.jbrain.qlink.QSession`) - User session with state machine
- **QConnection** (`org.jbrain.qlink.connection.QConnection`) - Network layer handling frame serialization and keep-alive

### State Machine Pattern

Sessions flow through states defined in `org.jbrain.qlink.state`:
```
User connects → Authentication → MainMenu → Chat/Games/etc → Termination
```

Each state implements `QState` interface and processes `Action` objects.

### Command/Action Processing

```
Raw bytes → CommandFactory → Command objects → ActionFactory → Action subclasses
                                                    ↓
                                            QSession.actionOccurred()
                                                    ↓
                                            CurrentState.execute(Action)
```

Action classes are in `org.jbrain.qlink.cmd.action` (~280+ types).

### Key Subsystems

| Directory | Purpose |
|-----------|---------|
| `chat/` | Room management, IRC integration, auditorium |
| `cmd/action/` | Protocol actions (Login, Chat, GameMove, etc.) |
| `cmd/fdo/` | FDO (Frozen Data Objects) UI protocol |
| `state/` | Session state machine implementations |
| `db/` | Database utilities (DBUtils) |
| `connection/` | Socket/network handling |

### Chat System

`RoomManager` (singleton) manages rooms with different delegate behaviors:
- `RoomDelegate` - Standard text chat
- `AuditoriumDelegate` - Read-only broadcast
- `IRCRoomDelegate` - IRC gateway (bridges to external IRC servers)
- `GameDelegate` - Game spaces

### Protocol Listeners

- **QTCPListener** (port 5190) - Main Q-Link protocol
- **HabilinkListener** (port 1986) - Habitat virtual world proxy

## Configuration

Default config: `src/main/resources/qlink_defaults.properties`

Key settings:
- Database connection (jdbc_uri, username, password)
- Habitat integration (host, port)
- IRC integration (host, port for chat bridging)
- Keep-alive settings

## Dependencies

- MySQL Connector for database
- Log4j 1.2.5 for logging
- Commons Configuration for config management
- Martyr (local JAR in `lib/`) for IRC protocol

## Current Development Focus

Recent commits focused on converting SQL string concatenation to PreparedStatements for security. When modifying database code, use PreparedStatements.
