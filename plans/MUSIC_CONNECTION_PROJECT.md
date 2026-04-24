# Music Connection Protocol Reverse Engineering

## Project Context

Q-Link Reloaded is a modern Java 17 reimplementation of the Q-Link online service
for Commodore 64 computers (original service ran 1985-1994). The server speaks the
QTCP protocol on port 5190.

The **Music Connection** was a Q-Link feature built on top of the **People Connection**
chat infrastructure. The MC client disk image is available and runs in VICE emulator.
The MC client handles all standard People Connection protocol messages, but also
responds to a set of server-initiated messages whose behavior is unknown.

The goal of this project is to identify those unknown server-initiated messages by
injecting them into live sessions and observing client behavior in VICE.

---

## Protocol Fundamentals

See `PROTOCOL_MESSAGES.md` for the full protocol reference. Key points:

- All frames start with `CMD_START` (0x5A)
- Action messages use `CMD_ACTION` (0x20) with a 2-character ISO-8859-1 mnemonic
- CRC16 is calculated over bytes 5 through length-1
- The command class hierarchy is rooted in `Action` → `AbstractAction`
- Unknown messages currently hit `UnknownAction` and are logged

---

## Unknown Mnemonics

These are server-initiated messages whose client-side behavior is unknown.
They live in `src/main/java/org/jbrain/qlink/cmd/action/fdo/` as stubs.

| Mnemonic | Current Class | Hypothesis |
|----------|--------------|------------|
| `C1` | stub | Possibly Channel 1 command |
| `C2` | stub | Possibly Channel 2 command |
| `C3` | stub | Possibly Channel 3 command |
| `CA` | stub | Possibly Channel A or Channel Action |
| `CB` | stub | Possibly Channel B |
| `CL` | stub | Possibly Channel Leave/Close |
| `D2` | stub | Unknown |
| `E2` | stub | Unknown |
| `EK` | stub | Unknown |
| `FO` | stub | Possibly File Open or Find/Open |
| `MC` | stub | Likely Music Connection init/entry |
| `MF` | stub | Possibly Music Feed or Music File |
| `OE` | stub | Unknown |
| `XX` | stub | Unknown - possibly session terminator |
| `JAM` | `JAM` | Unknown - 3-char mnemonic, unusual |
| `Fp` | `Fp` | Unknown - mixed case, unusual |
| `ZA` | `ZA` | Unknown |

---

## Reverse Engineering Approach

The strategy is **server-push probing**: inject unknown messages into a live
authenticated session and observe what the C64 client does in VICE.

### The Tool We Need

Extend the existing `/protocol` admin command
(`src/main/java/org/jbrain/qlink/cmd/action/ProtocolCommand.java`) to support
a **send** subcommand:

```
/protocol send <username> <mnemonic> [hex-payload]
```

This should:
1. Look up the named user's active session
2. Construct a valid QTCP action frame with the given mnemonic and optional payload
3. Send it down to the client
4. Log both the sent frame and any response from the client

### Frame Construction

To construct a valid injectable frame:
- Start with `CMD_START` (0x5A)
- Use `CMD_ACTION` (0x20) as command type
- Encode the mnemonic as 2 bytes ISO-8859-1
- Append the hex payload if provided
- Calculate CRC16 over the appropriate byte range
- Use the session's current send/recv sequence numbers

Look at how existing action classes serialize their responses to understand
the correct frame construction pattern.

---

## Experimental Protocol

For each unknown mnemonic:

1. Start VICE with the Music Connection disk image
2. Connect to the local server (localhost:5190)
3. Authenticate as a test user
4. From a second terminal, issue:
   ```
   /protocol send <testuser> <mnemonic>
   ```
5. Record what happens on the VICE screen:
   - Nothing (mnemonic ignored or unrecognized by client)
   - UI change (a screen, dialog, or menu appears)
   - Client crash or disconnect
   - Client sends a response message back (check server logs)
6. If a UI appears, try variations with different payloads
7. Document findings in this file under **Findings** below

Start with `MC` since it is the most likely entry point for Music Connection
specific functionality. Then try `C1/C2/C3` as a group since they likely
relate to the same feature (channels).

---

## Payload Experimentation

When a mnemonic produces a visible response, vary the payload:
- Empty payload first
- Single byte: 0x00, 0x01, 0xFF
- String payloads (null-terminated PETSCII)
- Length-prefixed strings (common Q-Link pattern)

Look at `AbstractStringAction` and similar base classes to understand
common payload patterns already established in the protocol.

---

## Implementation Notes

- The `fdo` package appears to relate to Music Connection / SuperQ improvements
  to People Connection. All stub classes there are candidates.
- `JAM`, `Fp`, and `ZA` have unusual formats (3-char, mixed case) — these may
  be malformed captures rather than real mnemonics. Treat as low priority.
- The existing `/protocol capture` mode should be enabled during all experiments
  to log raw frames in both directions.
- When a mnemonic is understood, implement a proper Action class in the `fdo`
  package following the existing class hierarchy.

---

## Findings

*Document results here as experiments are run.*

### MC
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### C1
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### C2
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### C3
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### CA
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### CB
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### CL
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### D2
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### E2
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### EK
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### FO
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### MF
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### OE
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

### XX
- Payload tried: (none yet)
- Client response: (not yet tested)
- Notes:

---

## References

- `PROTOCOL_MESSAGES.md` — Full protocol message reference
- `src/main/java/org/jbrain/qlink/cmd/action/` — All action implementations
- `src/main/java/org/jbrain/qlink/cmd/action/fdo/` — Music Connection stubs
- `src/main/java/org/jbrain/qlink/cmd/action/ProtocolCommand.java` — Admin command to extend
- `plans/` — Other planning documents
