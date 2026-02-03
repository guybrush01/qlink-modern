# Protocol Analysis Infrastructure Plan

## Goal
Create a comprehensive system to capture, analyze, and reverse engineer unknown Q-Link protocol messages from unsupported application modules.

## Architecture Overview

Based on codebase exploration, the best interception points are:
1. **QConnection.processFrame()** (lines 175-197) - Raw frame access before parsing
2. **QSession._linklistener** (lines 60-77) - Parsed Action objects with session context
3. **ActionFactory.newInstance()** (line 319) - Where UnknownAction is created

## Implementation Phases

### Phase 1: Protocol Capture Layer

**New Class: `org.jbrain.qlink.protocol.ProtocolAnalyzer`**

A singleton service that captures all traffic with rich context:

```java
public class ProtocolAnalyzer {
    // Capture raw frames (before parsing)
    void captureInboundFrame(QSession session, byte[] data, int start, int len);
    void captureOutboundFrame(QSession session, byte[] data, int start, int len);

    // Capture parsed actions
    void captureAction(QSession session, Action action, QState state);

    // Control methods
    void startCapture(String sessionId);  // Capture specific session
    void startCaptureAll();               // Capture everything
    void stopCapture();
    void setFilter(ProtocolFilter filter); // Filter by mnemonic, state, etc.
}
```

**Modifications:**
- `QConnection.java`: Add capture calls in processFrame() and send methods
- `QSession.java`: Add capture call in _linklistener.actionOccurred()

### Phase 2: Unknown Action Repository

**New Class: `org.jbrain.qlink.protocol.UnknownActionRepository`**

Persists unknown actions for later analysis:

```java
public class UnknownActionRepository {
    void record(UnknownAction action, QSession session, QState state);
    List<UnknownActionRecord> findByMnemonic(String mnemonic);
    List<UnknownActionRecord> findByState(String stateName);
    Map<String, Integer> getMnemonicFrequency();
}
```

**Database Schema:**
```sql
CREATE TABLE protocol_captures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    session_id VARCHAR(50),
    user_handle VARCHAR(20),
    state_name VARCHAR(50),
    direction ENUM('INBOUND', 'OUTBOUND'),
    mnemonic CHAR(2),
    raw_hex TEXT,
    payload_hex TEXT,
    is_unknown BOOLEAN DEFAULT FALSE
);
```

**Modification:**
- `ActionFactory.java`: Record unknown actions when creating UnknownAction

### Phase 3: Analysis Tools

**New Class: `org.jbrain.qlink.protocol.ProtocolDecoder`**

Utilities for analyzing captured data:

```java
public class ProtocolDecoder {
    // Decode frame components
    FrameInfo decodeFrame(byte[] data);

    // Pattern analysis
    List<PatternMatch> findPatterns(List<UnknownActionRecord> records);

    // Payload analysis
    String hexDump(byte[] data);
    String asciiDump(byte[] data);  // ISO-8859-1 interpretation
    String petsciiDump(byte[] data); // PETSCII interpretation (C64 charset)
}
```

**New Admin Command: `/protocol`**

Admin commands for live analysis:
- `/protocol capture start` - Start capturing all traffic
- `/protocol capture stop` - Stop capturing
- `/protocol unknowns` - List unknown mnemonics seen
- `/protocol decode <hex>` - Decode a raw frame
- `/protocol replay <id>` - Replay a captured message

### Phase 4: Experimental Response Framework

**New Class: `org.jbrain.qlink.protocol.ResponseExperimenter`**

Allow crafting and sending experimental responses:

```java
public class ResponseExperimenter {
    void sendRawFrame(QSession session, byte[] data);
    void sendAction(QSession session, Action action);

    // Template-based responses
    void registerTemplate(String name, ResponseTemplate template);
    void sendTemplate(QSession session, String templateName, Map<String,Object> params);
}
```

## File Structure

```
src/main/java/org/jbrain/qlink/
├── protocol/
│   ├── ProtocolAnalyzer.java      # Main capture service
│   ├── UnknownActionRepository.java # Persistence layer
│   ├── ProtocolDecoder.java       # Analysis utilities
│   ├── ResponseExperimenter.java  # Response testing
│   ├── FrameInfo.java             # Frame structure data
│   ├── UnknownActionRecord.java   # Capture record entity
│   └── ProtocolFilter.java        # Filter criteria
```

## Implementation Order

1. **ProtocolAnalyzer** + QConnection/QSession modifications (core capture)
2. **UnknownActionRepository** + database schema (persistence)
3. **ProtocolDecoder** (analysis utilities)
4. **Admin commands** (live interaction)
5. **ResponseExperimenter** (testing framework)

## Configuration

Add to `qlink_defaults.properties`:
```properties
# Protocol Analysis
qlink.protocol.capture.enabled=false
qlink.protocol.capture.unknowns_only=true
qlink.protocol.capture.log_file=logs/protocol_capture.log
```

## Testing Strategy

1. Connect C64 client to server
2. Enable capture
3. Navigate through unsupported application modules
4. Analyze captured unknown mnemonics
5. Cross-reference with any available Q-Link documentation
6. Craft experimental responses to understand expected behavior

## Notes

- UnknownAction already flows through the pipeline (not dropped)
- Frame structure: START(0x5A) + CRC(4) + seq(2) + command type + payload + FRAME_END(0x0D)
- 2-byte ASCII mnemonics identify action types
- Session context (user, state) available at all interception points
- Existing trace() method in QConnection provides hex dump utility
