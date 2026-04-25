# Q-Link Client Analysis – Action Code Investigation Plan

## 1. Overview

The Commodore 64 Q-Link (QuantumLink) client is a self-contained telecommunications program that communicates with a host system over a serial modem or emulated connection. The protocol uses framed packets with a two-character **action code** (`CMD_ACTION = 0x20`) to trigger various operations such as login, chat, file transfer, games, and account management.

This document summarises the discovered action codes, their dispatch mechanism, relevant memory locations, and human-readable strings extracted from a full memory dump. It serves as a briefing for a coding agent to design and execute a test plan that exercises each action code and observes the client’s behaviour.

## 2. Action Code Dispatch Mechanics

### 2.1 Dispatch table location

The client maintains a linear list of supported action code mnemonics at **memory address `$BEA1`** (in the running client’s RAM). The mnemonics are stored as PETSCII bytes separated by spaces:

```
sg dd md mo ma mb lo mp ss xx or gn qk pk zy zn za zm zo d1 d2 d5 d6
```

That yields **22 action codes**. Any incoming `CMD_ACTION` frame that does not match one of these codes will be ignored or may trigger an error handler.

### 2.2 Dispatcher routine

The matching routine starts at **`$BE9D`**:

```asm
.C:be9d   A0 00       LDY #$00
.C:be9f   B9 A2 BE    LDA $BEA2,Y    ; load next byte from table
```

It compares the incoming two-character mnemonic against the table. If a match is found, the corresponding handler address is fetched from a parallel table of 16-bit pointers immediately following the mnemonic string. The exact start of the pointer table is approximately **`$BEE4`**, but you should verify by examining the code at `$BE9F` onward.

**To the coding agent:** Load the full disassembly and identify the exact offset of the pointer table. The handler addresses can then be extracted directly from the memory dump.

### 2.3 Additional candidate commands

- **`Jf`** – not present in the table above, but captured from client->server traffic. It carries a filename (`johna.mus`) and likely initiates a file upload. It may be handled by a separate dispatch or by a file‑transfer subsystem that interprets the payload after a generic command. Test it as a separate case.

- The `$BEA1` table uses **lowercase** letters. Upper/Lower case may be significant – always test with the exact byte from the capture: `4A 66` for `Jf`.

## 3. Key Memory Regions

| Address | Description |
|---------|-------------|
| `$BEA1` | Action code table (string) |
| `$BEE4` (approx) | Handler address table |
| `$0D2E` | Main command‑type dispatch table (0x20 points to action handler) |
| `$1F93` | Another jump table (local keyboard commands) |
| `$0F18` | Menu prompts and status messages |
| `$2200–$2A00` | Large blocks of user‑interface strings |
| `$2E00–$2E1F` | Connection parameters (baud rate, etc.) |
| `$2EC8` / `$2FC8` | Possibly active handler vectors |
| `$30B6–$3FFF` | Help text, diagnostic strings, terminal configuration |
| `$2D7D` | “Please wait for network connection.” |
| `$2933` | “Call not connected – press RETURN to retry.” |
| `$2302` | “One Moment Please...” |
| `$22E0` | “Insert Disk And Press RETURN” |
| `$0FE9` | “Modem disconnected” |

## 4. Human‑Readable Strings Already Extracted

These strings give context about the client’s features. They can be used to infer what each action code might do.

### 4.1 Menus and Prompts

```
$22E0:   Insert Disk And Press RETURN
$2302:   One Moment Please...
$2318:   Q-Link disk please
$0FE9:   Modem disconnected
$2933:   Call not connected - press RETURN to retry.
$2D7D:   Please wait for network connection.
$2DD3:   Connecting -
$2D14:   41400:  (baud rate?)
$2F19:   10020    (baud rate?)
$0F18:   PLEASE ENTER COMMAND  (inferred)
$20E4:   (startup initialization)
```

### 4.2 Chat and Messaging

```
$1FA2:   * INCOMING   (status line template)
$1FB0:   OLM: (OnLine Message prompt)
$207C:    olm: (lowercase version)
$B0D1:   ... (long block of spaces, possibly a blank screen template)
$F0E9:   people in the current room:
$6570:   locate someone: t
$662A:   go to public room: t
$66BF:   send an online message: t
$6A96:   : (room list separator)
$6AC9:   :
```

### 4.3 Email

```
$68D9:   send e-mail to someone: t
$2A14:   save
$2A26:    e-m
$A00D:    e-m
$A0A5:    sid f   (save in disk? file?)
$8062:    e-m
$2323:   Thank you
```

### 4.4 File Transfer

```
$6872:   play by filename: t
$10D30:  q on line music
```

### 4.5 Games

```
$1FB9:   (encrypted table, but references to game commands)
$BEA1:   Contains "sg", "gn", "d1".."d6"
$AA22:   Long message about inserting disk, loading games, etc.
```

### 4.6 Account / Configuration

```
$A08D:   olm m
$4199:    iy   (might be part of "identify"?)
$5290:   I P   (IP address?)
$1606:   XI     (terminal type?)
$1629:   AI@
```

### 4.7 Modem / Terminal

```
$31C9:    Type commands to the modem, then
$31F1:     press F1 when connection is made.
$0E7B:   IzP1 O
$0EB3:   B.M
$0EBB:   C.M
$0EBF:   .P
$370B:    press RETURN when carrier is present
$36D6:   no carrier
```

## 5. Proposed Test Plan

For each action code listed in the table (plus `Jf`), send a minimal valid packet from the server and monitor:

- **Screen output** (check for changes in PETSCII screen memory `$0400‑$07E7`)
- **Color RAM** (`$D800‑$DBE7`) to see if colours change
- **Zero‑page variables** around `$2E00‑$2E20` (connection/state flags)
- **Higher RAM tables** at `$BEA1` and `$2EC8` (they might be modified)
- **Serial port output** (use VICE’s RS‑232 monitor or a virtual null‑modem)
- **Disk activity** (watch for KERNAL LOAD/SAVE calls, `$FFD5` / `$FFD8`)

### 5.1 Base packet template

All test packets should follow the standard Q‑Link frame structure:

```
Offset  Content
0       $5A (CMD_START)
1‑2     CRC (two bytes, can be zeroed for testing if the client doesn't check)
3       Send Sequence (e.g. $00)
4       Recv Sequence (e.g. $00)
5       $20 (CMD_ACTION)
6‑7     Two‑byte mnemonic (e.g. $64 $64 for "dd")
8+      Payload (empty for simple tests)
```

Example: **Login** would be`5A 00 00 00 00 20 64 64 <account> <code>` (but account/code format unknown – start with an empty string).

### 5.2 Action‑by‑action test cases

| Code |Bytes (hex)| Suggested payload | Expected client behaviour | Notes |
|------|-----------|-------------------|---------------------------|-------|
|`sg` |`73 67`   | none              | Might enter game state, display “game” prompt | Look for memory changes in game board area `$40xx` |
|`dd` |`64 64`   | 10 bytes + 4 bytes? | Login prompt; display “Account:” / “Code:” | This is the standard login command |
|`md` |`6D 64`   | none              | Maybe “Message Delete” – screen prompt? | Check for disk or serial access |
|`mo` |`6D 6F`   | none              | “Message Open” – read mail? | Watch for mail directory listing |
|`ma` |`6D 61`   | none              | “Message All” – list all? | |
|`mb` |`6D 62`   | none              | Message Board | Expect board listing |
|`lo` |`6C 6F`   | none              | Logoff – may clear screen and return to terminal | Observe serial disconnect sequence |
|`mp` |`6D 70`   | none              | Message Post? | Prompt for text |
|`ss` |`73 73`   | none              | System status? | Return serial info? |
|`xx` |`78 78`   | none              | Unknown | |
|`or` |`6F 72`   | none              | Unknown | |
|`gn` |`67 6E`   | none              | Game Next? | |
|`qk` |`71 6B`   | none              | Quick? | |
|`pk` |`70 6B`   | none              | Peek? | |
|`zy` |`7A 79`   | none              | Unknown | |
|`zn` |`7A 6E`   | none              | Unknown | |
|`za` |`7A 61`   | none              | Unknown | |
|`zm` |`7A 6D`   | none              | Unknown | |
|`zo` |`7A 6F`   | none              | Unknown | |
|`d1` |`64 31`   | none              | Diagnostic 1 – dump info? | |
|`d2` |`64 32`   | none              | Diagnostic 2 | |
|`d5` |`64 35`   | none              | Diagnostic 5 | |
|`d6` |`64 36`   | none              | Diagnostic 6 | |
|`Jf` |`4A 66`   | `00 01 00 00 04 90 04` + filename + `90 FF` | File upload initiate | Expect disk access, “Sending file…” message, block counter |

### 5.3 Observability hints

- **Screen output:** Copy the 1000 bytes at `$0400` before and after sending a packet. A simple diff will show any new text.
- **Disk activity:** In VICE, enable “True drive emulation” and monitor via “Peripheral devices” → “Drive 8”. The client uses standard KERNAL LOAD/SAVE, so breakpoints on `$FFD5` and `$FFD8` will catch file operations.
- **Serial I/O:** If the client sends data back, you’ll see bytes in the RS‑232 output buffer (`$029C` pointer) or the CIA #2 registers.
- **State variables:** Watch `$2E07` (online flag?), `$2E11` (message count?), `$2E14` (block count?), `$2E1E` (sequence numbers?).

## 6. Deliverables to Request from the Coding Agent

- A script (Python or similar) that can:
  - Connect to the emulated client (e.g., via VICE binary monitor or a virtual serial link)
  - Send each action code from the table, with various payloads
  - Capture screen memory, colour RAM, and selected memory regions before/after
  - Log all outgoing serial traffic
  - Produce a diff report for each test
- A mapping of **handler addresses** from the memory dump, cross‑referenced with the action codes.
- A report on what each mnemonic does, including any payload structure and expected responses.

## 7. Next Steps for Manual Analysis

1. Locate the exact handler pointer table (use the dispatcher code to follow the indexing).
2. Set breakpoints on those handler addresses in VICE and trace execution when a packet arrives.
3. For `Jf`, search the disassembly for the bytes `4A 66` and the string `"johna.mus"` to find where the filename is processed.
4. Map the full command state machine by observing screen transitions.

This document should provide all necessary context for the coding agent to begin automated testing. 