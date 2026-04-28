# Q-Link Reloaded - Developer Onboarding Guide

**Version:** 0.1.0  
**Last Updated:** 2026-04-28  
**Project Status:** Active Development

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Quick Start](#quick-start)
3. [Architecture Overview](#architecture-overview)
4. [Development Environment](#development-environment)
5. [Key Concepts](#key-concepts)
6. [Building and Testing](#building-and-testing)
7. [Protocol Reference](#protocol-reference)
8. [Current Development Focus](#current-development-focus)
9. [Code Quality Standards](#code-quality-standards)
10. [Common Tasks](#common-tasks)
11. [Debugging and Troubleshooting](#debugging-and-troubleshooting)

---

## Project Overview

**Q-Link Reloaded** is a Java server implementation that recreates the Q-Link (Quantum Link) online service - the original AOL predecessor for the Commodore 64. It allows C64 users to connect for chat, gaming, file transfers, and email.

### Historical Context

- **Q-Link** launched in 1985 as a standalone online service for Commodore 64 users
- It was later acquired and became **America Online (AOL)** in 1989
- This project is a clean-room reimplementation based on protocol analysis and documentation
- The original system was written in PL/1 running on Stratus VOS hardware with multiple 68010 processors

### Key Features

- Multi-user chat system with configurable rooms
- Real-time multiplayer games (C64-based)
- File transfer capability (upload/download)
- Email and Online Message System (OMS)
- Auditorium/panel system for broadcasts
- Gateway support for external services

---

## Quick Start

### Prerequisites

- Java 17+ (JDK)
- Maven 3.6+
- MySQL 8.0+ (or use Docker)
- Git

### Initial Setup

```bash
# Clone and navigate to the repository
cd /Users/johna/work/qlink-modern

# Build the project
mvn clean package

# Bootstrap the database (WARNING: deletes existing data)
./bootstrap

# Run the server
java -jar target/qlink-0.1.0.jar
```

The server will start on port 5190 (QTCP) waiting for C64 connections.

### Docker Setup

```bash
# Start MySQL and QLink server
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Q-Link C64 Client                        │
│         (Protocol: QTCP Port 5190 / Habilink 1986)          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   QLinkServer                                │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │ QTCPListener    │  │ HabilinkListener│                   │
│  │ (Port 5190)     │  │ (Port 1986)     │                   │
│  └────────┬────────┘  └────────┬────────┘                   │
│           │                    │                              │
│           ▼                    ▼                              │
│  ┌──────────────────────────────────────────────┐           │
│  │              QConnection (Thread)             │           │
│  │  - Frame serialization                        │           │
│  │  - Sequence numbers (0x10-0x7f)               │           │
│  │  - Flow control (QSIZE=16)                    │           │
│  │  - Keep-alive ping                            │           │
│  └──────────────────┬───────────────────────────┘           │
│                     │                                        │
│                     ▼                                        │
│  ┌──────────────────────────────────────────────┐           │
│  │               QSession                       │           │
│  │  - State machine (Authentication→MainMenu)   │           │
│  │  - Account management                         │           │
│  │  - OLM message queue                          │           │
│  │  - Current state and delegates                │           │
│  └──────────────────┬───────────────────────────┘           │
│                     │                                        │
│       ┌─────────────┴─────────────┐                          │
│       ▼                           ▼                          │
│  ┌──────────┐          ┌─────────────────┐                  │
│  │  State   │          │   Chat System   │                  │
│  │Machine   │          │   RoomManager   │                  │
│  │ States   │          │   Delegates     │                  │
│  └──────────┘          └─────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
```

### Core Components

| Module | Location | Description |
|--------|----------|-------------|
| **QLinkServer** | `org.jbrain.qlink.QLinkServer` | Main server entry point, manages all sessions |
| **QSession** | `org.jbrain.qlink.QSession` | Represents a single client connection with state machine |
| **QConnection** | `org.jbrain.qlink.connection.QConnection` | Network connection handler with frame serialization |
| **State Machine** | `org.jbrain.qlink.state` | Session states (Authentication, MainMenu, Chat, etc.) |
| **Protocol Layer** | `org.jbrain.qlink.protocol` | Protocol capture/analysis infrastructure |
| **Action Factory** | `org.jbrain.qlink.cmd.action.ActionFactory` | Parses incoming protocol into Action objects |
| **Chat System** | `org.jbrain.qlink.chat` | Room management, delegates, IRC integration |
| **Data Access** | `org.jbrain.qlink.db` | Database layer (DAOs + Spring Data JPA) |

### Subsystem Directory Structure

```
src/main/java/org/jbrain/qlink/
├── QLinkServer.java              # Main server hub
├── QSession.java                 # Client session with state machine
├── QConfig.java                  # Global configuration singleton
├── protocol/                     # Protocol analysis tools
│   ├── ProtocolAnalyzer.java
│   ├── UnknownActionRepository.java
│   ├── ProtocolDecoder.java
│   └── ResponseExperimenter.java
├── connection/                   # Network layer
│   ├── QConnection.java
│   ├── QLinkListener.java
│   └── HabitatConnection.java
├── state/                        # State machine implementations
│   ├── QState.java               # State interface
│   ├── AbstractState.java        # Base state implementation
│   ├── Authentication.java       # Login sequence
│   ├── MainMenu.java             # Main menu state
│   ├── DepartmentMenu.java       # Department navigation
│   ├── chat/                     # Chat states
│   ├── game/                     # Game states
│   └── dialog/                   # Dialog states
├── cmd/                          # Commands and actions
│   ├── action/                   # 200+ Action subclasses
│   │   ├── LO.java              # Logoff
│   │   ├── MC.java              # Menu command
│   │   ├── EC.java              # Enter chat
│   │   ├── CS.java              # Chat message
│   │   ├── GameMove.java        # Game move
│   │   ├── SendEmail.java       # Email send
│   │   └── ... (200+ more)
│   ├── protocol/                 # Protocol analysis actions
│   └── admin/                    # Admin commands
├── chat/                         # Chat system
│   ├── RoomManager.java          # Singleton room manager
│   ├── RoomDelegate.java         # Standard room
│   ├── AuditoriumDelegate.java   # Broadcast room
│   ├── GameDelegate.java         # Game room
│   └── IRCRoomDelegate.java      # IRC gateway
├── db/                           # Data access
│   ├── dao/                      # Traditional DAO layer
│   ├── entity/                   # JPA Entities
│   ├── repository/               # Spring Data JPA Repositories
│   └── config/                   # Spring configuration
├── user/                         # User models
├── text/                         # Text formatting utilities
└── util/                         # Utility classes
    ├── SecurityUtils.java        # Input validation
    ├── DatabaseUtils.java        # Secure DB operations
    └── CRC16.java                # CRC calculation
```

---

## Development Environment

### Recommended IDE Configuration

**IntelliJ IDEA:**
-.import as Maven project
- Enable annotation processing
- Configure Java 17 language level
- Install Lombok plugin (if used)

**VS Code:**
- Java Extension Pack
- Maven for Java
- Lombok Support

**Code Style:**
- Line length: 120 characters (configured in checkstyle.xml)
- Import organization: Group by module, static imports last
- Braces: Even for single statements
- Naming: CamelCase for methods/variables, PascalCase for classes

### Build Tools

```bash
# Compile
mvn compile

# Run tests
mvn test

# Package (create executable JAR)
mvn package

# Run all quality checks
mvn verify

# Clean and rebuild
mvn clean package
```

### Code Quality Tools

| Tool | Purpose | Configuration | Command |
|------|---------|---------------|---------|
| **Checkstyle** | Code style enforcement | checkstyle.xml | `mvn checkstyle:check` |
| **PMD** | Static analysis | pmd.xml | `mvn pmd:check` |
| **SpotBugs** | Bytecode analysis | spotbugs-exclude.xml | `mvn spotbugs:check` |
| **JaCoCo** | Code coverage | pom.xml | `mvn jacoco:report` |

---

## Key Concepts

### State Machine Pattern

Sessions flow through a sequence of states:

```
User connects → Authentication → MainMenu → [Chat/Game/Email/etc] → Termination
```

Each state implements the `QState` interface:

```java
interface QState {
    void activate() throws IOException;    // Entered this state
    boolean execute(Action a) throws IOException;  // Handle action
    void passivate() throws IOException;   // Leaving this state
    void terminate();                      // Session ended
    String getName();
}
```

**State Classes:**
- `AbstractState` - Base implementation with common action handling
- `AbstractMenuState` - Menu/list navigation common logic
- `AbstractChatState` - Chat-specific behavior
- `AbstractDialogState` - Dialog interaction handling

### Protocol Frame Structure

```
Byte 0:    0x5A              # CMD_START
Bytes 1-2: CRC16 (high 12 bits)
Bytes 3-4: CRC16 (low 12 bits)
Byte 5:    Send sequence      # 0x10-0x7f
Byte 6:    Receive sequence   # 0x10-0x7f
Byte 7:    Command mnemonic   # 2-char ASCII (e.g., "LO", "MC")
Bytes 8+:  Payload            # Command-specific data
Byte N:    0x0D               # FRAME_END (CR)
```

**Example Frame:**
```hex
5a 81 42 31 4e 7f 7f 4c 4f 0d
```
Breakdown:
- `5a` - Start byte
- `81 42 31 4e` - CRC16 checksum
- `7f 7f` - Sequence numbers (send/receive)
- `4c 4f` - Mnemonic "LO" (Logoff)
- `0d` - End marker

### Action Pattern

Each protocol command is parsed into an `Action` subclass:

```java
public class Logoff extends Action {
    public static final String MNEMONIC = "LO";
    
    public Logoff(String mnemonic, QSession session) {
        super(mnemonic, session);
    }
    
    @Override
    public void execute(QState state) throws IOException {
        state.terminate();
    }
}
```

**Action Hierarchy:**
- `Action` (base)
  - `AbstractIDAction` (with user/game IDs)
  - `ProxiedAction` (Habitat proxy)
  - `UnknownAction` (unrecognized commands)

### Chat System Architecture

**RoomManager** (singleton) manages rooms with different delegate behaviors:

| Delegate | Purpose |
|----------|---------|
| `RoomDelegate` | Standard text chat rooms |
| `AuditoriumDelegate` | Read-only broadcast (panel/box office) |
| `IRCRoomDelegate` | IRC gateway to external servers |
| `GameDelegate` | Multiplayer game spaces |

**Room Types:**
- **Public Rooms** - Listed in room list,Anyone can join
- **Private Rooms** - Invite-only, password protected
- **Game Rooms** - Reserved for multiplayer games

---

## Building and Testing

### Build Commands

```bash
# Full build with all tests and quality checks
mvn clean verify

# Create executable JAR
mvn package

# Skip tests (not recommended for production)
mvn package -DskipTests

# Run a specific test class
mvn test -Dtest=SecurityUtilsTest

# Generate code coverage report
mvn clean test jacoco:report
```

### Running the Server

```bash
# Using the executable JAR
java -jar target/qlink-0.1.0.jar

# With custom configuration
java -jar target/qlink-0.1.0.jar -configFile myconfig.properties

# Environment variable overrides
export QLINK_DB_JDBC_URI=jdbc:mysql://localhost:3306/qlink
export QLINK_DB_USERNAME=qlinkuser
export QLINK_DB_PASSWORD=qlinkpass
java -jar target/qlink-0.1.0.jar
```

### Debugging

```bash
# Enable remote debugging (port 1899)
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:1899 \
    -jar target/qlink-0.1.0.jar
```

**IDE Connection:**
- Connect to localhost:1899 from IntelliJ/VSCode
- Set breakpoints in core classes
- Step through protocol processing

---

## Protocol Reference

### Command Mnemonics (Client→Server)

| Mnemonic | Description |
|----------|-------------|
| **LO** | Logoff |
| **MC** | Menu command (enter department/room) |
| **EC** | Enter chat room |
| **CS** | Chat message |
| **LM** | List rooms |
| **AA** | Chat text (with seat number) |
| **ZA** | Text entry response (dialog) |
| **D2** | Access code entry |
| **SM** | Send email/online message |
| **RE** | ReadWaiting email/OM |
| **CL** | Clear screen |
| **GP** | Generic menu item |
| **XX** | Unknown (no action) |
| **IL** | Ignore user |
| **SL** | Stop ignoring user |
| **MR** | Move to room |
| **MS** | Menu selection |

### Command Mnemonics (Server→Client)

| Mnemonic | Description |
|----------|-------------|
| **LO** | Logoff |
| **MC** | Menu command |
| **EC** | Enter chat |
| **CS** | Chat message |
| **CL** | Clear screen |
| **SM** | Special message |
| **SE** | Signal end |
| **XX** | No action (ACK) |
| **D2** | Display access code entry |
| **OE** | Online message end |
| **OM** | Online message |
| **ON** | Online message request |
| **OE** | End of online message |
| **OK** | Okay to send online message |
| **RD** | No mail waiting |
| **RE** | Last line of email |
| **RS** | Next line of email |
| **RF** | Email and clear pending flag |
| **RN** | Read email acknowledged |
| **DM** | Department menu |
| **PM** | Private menu |
| **LM** | List rooms |
| **LD** | Last room in list |
| **CP** | Change to private room |
| **CL** | Change to public room |
| **CM** | Change room (with name) |

### Admin Commands

| Command | Description |staff Only |
|---------|-------------|-----------|
| `/protocol capture start` | Start protocol capture | Yes |
| `/protocol capture stop` | Stop protocol capture | Yes |
| `/protocol unknowns list` | List unknown mnemonics | Yes |
| `/protocol status` | Show analyzer status | Yes |
| `/protocol decode <hex>` | Decode raw frame | Yes |

---

## Current Development Focus

### Phase 1: Security Modernization (COMPLETE ✅)

**Completed Improvements:**
- Log4j 1.x → Log4j 2.x upgrade (v2.23.1)
- Input validation framework (SecurityUtils)
- SQL injection prevention (parameterized queries)
- XSS prevention (HTML escaping)
- Directory traversal prevention
- Centralized error handling

**Files Created:**
- `org.jbrain.qlink.util.SecurityUtils.java`
- `org.jbrain.qlink.util.DatabaseUtils.java`
- `org.jbrain.qlink.util.ExceptionHandler.java`
- `src/main/resources/log4j2.xml`

### Phase 2: Protocol Analysis Infrastructure (COMPLETE ✅)

**Implemented Features:**
- Protocol frame capture (inbound/outbound)
- Unknown action repository (MySQL persistence)
- Protocol decoder utilities
- Admin command interface
- Response experimenter framework

**Files Created:**
- `org.jbrain.qlink.protocol.ProtocolAnalyzer`
- `org.jbrain.qlink.protocol.UnknownActionRepository`
- `org.jbrain.qlink.protocol.ProtocolDecoder`
- `org.jbrain.qlink.protocol.ResponseExperimenter`
- `org.jbrain.qlink.protocol.UnknownActionRecord`
- `org.jbrain.qlink.cmd.action.ProtocolCommand`

### Roadmap (Next Steps)

**Priority 1 - Critical (Recommended for New Contributors)**

1. **Create Unit Test Suite**
   - Target: Start with utilities (SecurityUtils, CRC16)
   - Then: State machine transitions
   - Finally: DAO layer with H2 embedded database

2. **Fix Security Issues**
   - Replace `java.util.Random` with `java.security.SecureRandom`
   - Add rate limiting for connections
   - Implement authentication for admin commands

3. **Refactor ActionFactory**
   - Replace O(n) if-chain with HashMap registry
   - Target: O(1) action dispatch

**Priority 2 - High**

4. Code Quality Improvements
   - Merge DAO and Spring Data patterns
   - Refactor large files (DepartmentMenu: 28KB)
   - Remove dead code (test loops, duplicate checks)

5. Modernize Dependencies
   - HikariCP 4.0.3 → 5.x
   - commons-configuration 1.x → 2.x
   - Java 17 → Java 21 (when needed)

**Priority 3 - Medium**

6. Add CI/CD Pipeline
   - GitHub Actions for automated testing
   - Code quality checks on PR

7. Protocol Documentation
   - Complete protocol specification
   - Example interaction flows

---

## Code Quality Standards

### Java Coding Standards

**Naming Conventions:**
```java
// Classes: PascalCase
public class QLinkServer { }

// Methods: camelCase
public void processFrame() { }

// Constants: UPPER_SNAKE_CASE
public static final int QSIZE = 16;

// Variables: camelCase
private Logger _logger;
public String userName;
```

**File Structure:**
1. Package declaration
2. Imports (grouped: Java, javax, org, com, local)
3. Class declaration with Javadoc
4. Constants (static final)
5. Class fields
6. Constructors
7. Public methods
8. Private/protected methods
9. Inner classes

**Code Style:**
```java
// Proper formatting
if (condition) {
    doSomething();
} else {
    doSomethingElse();
}

// Try-with-resources
try (Connection conn = dataSource.getConnection()) {
    // Use connection
} catch (SQLException e) {
    _logger.error("Database error", e);
}

// Parameterized logging (no string concatenation)
_logger.debug("Processing user {} in state {}", userId, state);
```

### Security Requirements

**Input Validation:**
```java
// Use SecurityUtils for all user inputs
if (!SecurityUtils.isValidHandle(input)) {
    throw new ValidationException("Invalid handle");
}
```

**Database Access:**
```java
// Always use parameterized queries
PreparedStatement stmt = connection.prepareStatement(
    "SELECT * FROM users WHERE access_code = ?"
);
stmt.setString(1, accessCode);
```

**Logging:**
```java
// Don't log sensitive data
_logger.info("User {} logged in", userName);  // ✅
_logger.info("User password: {}", password);   // ❌
```

### Testing Standards

**Test Organization:**
```
src/test/java/org/jbrain/qlink/
├── util/                   # Utility tests
│   ├── SecurityUtilsTest.java
│   └── CRC16Test.java
├── state/                  # State machine tests
│   ├── AuthenticationTest.java
│   └── MainMenuTest.java
├── action/                 # Action tests
│   ├── LogoffTest.java
│   └── ChatMessageTest.java
└── dao/                    # DAO tests
    ├── UserDAOTest.java
    └── AccountDAOTest.java
```

**Test Naming:**
```java
public class ClassNameTest {
    // Test method naming: methodUnderTest_condition_expectedResult
    public void testLogin_validCredentials_returnsSuccess() { }
    public void testLogin_invalidCredentials_returnsFailure() { }
}
```

---

## Common Tasks

### Adding a New Protocol Action

1. Create Action class in `src/main/java/org/jbrain/qlink/cmd/action/`:

```java
package org.jbrain.qlink.cmd.action;

import org.jbrain.qlink.QSession;
import org.jbrain.qlink.state.QState;

public class NewAction extends Action {
    public static final String MNEMONIC = "NA";
    
    public NewAction(String mnemonic, QSession session) {
        super(mnemonic, session);
    }
    
    @Override
    public void execute(QState state) throws IOException {
        // Handle the action
    }
}
```

2. Register in ActionFactory.java:

```java
if (MNEMONIC.equals(LO.MNEMONIC)) {
    return new LO(mnemonic, session);
}
// Add new action
if (MNEMONIC.equals(NewAction.MNEMONIC)) {
    return new NewAction(mnemonic, session);
}
```

3. Test with a C64 client or protocol analyzer

### Modifying a State

1. Find the state class in `src/main/java/org/jbrain/qlink/state/`

2. Override the `execute()` method:

```java
@Override
public boolean execute(Action action) throws IOException {
    if (Login.MNEMONIC.equals(action.getMnemonic())) {
        // Handle login
        return true;  // Handled
    }
    return super.execute(action);  // Delegate to parent
}
```

3. For state transitions:

```java
QQState newState = new QQState(_session);
newState.activate();
_session.setState(this);
```

### Database Changes

1. Create Flyway migration in `src/main/resources/db/migration/`:

```sql
-- V3__Add_New_Table.sql
CREATE TABLE new_table (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

2. Update entity classes with JPA annotations

3. Add repository interface:

```java
@Repository
public interface NewTableRepository extends JpaRepository<NewTableEntity, Integer> {
    List<NewTableEntity> findByCreatedAtAfter(DateTime dateTime);
}
```

4. Use in code:

```java
@Autowired
private NewTableRepository newTableRepository;

public void save(Data data) {
    NewTableEntity entity = new NewTableEntity();
    entity.setCreatedAt(OffsetDateTime.now());
    newTableRepository.save(entity);
}
```

---

## Debugging and Troubleshooting

### Common Issues

**Server Won't Start**

```bash
# Check for port conflicts
lsof -i :5190

# Check database connection
mysql -u qlinkuser -p qlink

# Enable debug logging
java -jar target/qlink-0.1.0.jar -configFile config.properties
```

**C64 Client Can't Connect**

1. Verify port 5190 is open:

```bash
nc -zv localhost 5190
```

2. Check QLinkServer logs for connection attempts

3. Verify firewall rules

4. Test with Habitat proxy first (port 1986)

**Database Errors**

```sql
-- Verify schema
SELECT * FROM information_schema.tables WHERE table_schema = 'qlink';

-- Check for missing tables
SHOW TABLES;
```

**Protocol Analysis**

Use the admin interface to capture traffic:

```
/protocol capture start
# Perform action on C64 client
/protocol unknowns list
/protocol status
```

### Debugging Tools

**Log Analysis:**
```bash
# Count connections by IP
grep "Connection from" logs/qlink.log | awk '{print $7}' | sort | uniq -c

# Find unknown actions
grep "UnknownAction" logs/qlink.log

# Monitor active sessions
grep "State change" logs/qlink.log
```

**Protocol Capture Export:**
```java
UnknownActionRepository repo = UnknownActionRepository.getInstance();
List<UnknownActionRecord> records = repo.findAll();
for (UnknownActionRecord record : records) {
    System.out.printf("%s: %s %s\n", 
        record.getDirection(),
        record.getMnemonic(),
        record.getPayloadHex());
}
```

### Performance Profiling

```bash
# JVM flags for profiling
java -agentlib:perf -jar target/qlink-0.1.0.jar

# Memory usage
jstat -gcutil <pid> 1000

# Thread dump
jstack <pid>
```

---

## Additional Resources

### Documentation Files

| File | Purpose |
|------|---------|
| `README.md` | Basic setup and quick start |
| `README-BUILD.md` | Build system configuration |
| `PLAN.md` | Protocol analysis infrastructure plan |
| `PHASE1_COMPLETE.md` | Security modernization summary |
| `PHASE2_IMPLEMENTATION.md` | Protocol analysis implementation |
| `SPRING_DATA_MODERNIZATION.md` | JPA migration guide |
| `TESTING_PLAN.md` | Testing approach |
| `CLAUDE.md` | Claude Code assistant guide |

### External References

**Protocol Documentation:**
- `reference/protocol/qlinkfuncs.txt` - C64 command listing
- `reference/protocol/general.txt` - Frame format details
- `reference/protocol/All About q-link.txt` - Historical context
- `reference/protocol/q-link_sys_calls_from_dialer.txt` - Dialer commands

**Research:**
- `reference/mike_n/` - Mike Cray's Q-Link documentation
- `reference/msgs/` - System messages
- `reference/files/` - File transfer specifications

**Historical:**
- Randell Jesup (original Q-Link architect) interviews
- PlayNet history
- Commodore 64 communications protocols

---

## Getting Help

### Project Knowledge Base

- Check existing documentation before asking
- Review test files for usage examples
- Look at protocol captures for unknown behavior

### Development Workflow

1. Fork the repository
2. Create feature branch (`git checkout -b feature/my-feature`)
3. Make changes, add tests
4. Run quality checks: `mvn verify`
5. Push branch and create PR

### Community Guidelines

- Follow existing code style
- Write tests for new features
- Document public APIs
- Use meaningful commit messages

---

## Appendix: Quick Reference

### Project Statistics

- **Java Version:** 17
- **Maven Version:** 3.6+
- **Database:** MySQL 8.0
- **Build Size:** ~9.6MB JAR
- **Test Coverage:** ~0% (needs tests!)
- **Action Classes:** 200+
- **State Classes:** 20+
- **Database Tables:** 15+

### Key Classes Quick Reference

| Class | File | Description |
|-------|------|-------------|
| QLinkServer | `QLinkServer.java` | Main server class |
| QSession | `QSession.java` | Client session manager |
| QConnection | `connection/QConnection.java` | Network connection handler |
| ActionFactory | `cmd/action/ActionFactory.java` | Protocol → Action parser |
| RoomManager | `chat/RoomManager.java` | Chat room manager |

### Default Configuration

| Setting | Default | Override |
|---------|---------|----------|
| QTCP Port | 5190 | `-qtcpPort` or `QLINK_QTCP_PORT` |
| Habilink Port | 1986 | `-habilinkPort` or `QLINK_HABILINK_PORT` |
| Debug Port | 1899 | JVM args |
| DB Host | localhost | `QLINK_DB_HOST` |
| DB Port | 3306 | `QLINK_DB_PORT` |
| DB Name | qlink | `QLINK_DB_NAME` |
| Pool Size | 10 | `QLINK_DB_POOL_SIZE` |

---

**For more information, see the [GitHub repository](https://github.com/your-org/qlink-modern) or contact the project maintainers.**