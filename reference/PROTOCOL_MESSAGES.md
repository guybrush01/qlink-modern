# Q-Link Reloaded - Protocol Messages Reference

> This document summarizes all implemented protocol messages in the Q-Link Reloaded Java server implementation.

---

## Table of Contents

- [Protocol Overview](#protocol-overview)
- [Frame Structure](#frame-structure)
- [Command Types](#command-types)
- [Implemented Action Messages](#implemented-action-messages)
- [Reference Tables](#reference-tables)

---

## Protocol Overview

**Project**: Q-Link Reloaded
**Version**: 0.1.0
**Java Version**: 17+
**Build**: Maven (fat JAR)
**Main Class**: `org.jbrain.qlink.QLinkServer`

**Network Ports:**
- QTCP: 5190 (main Q-Link protocol)
- Habilink: 1986 (Habitat virtual world proxy)

---

## Frame Structure

### Common Frame Header

All Q-Link protocol frames share a common structure:

```
Offset  Size  Description
------  ----  -----------
0       1     CMD_START (0x5A) - Frame start marker
1-2     2     CRC upper byte (bits 12-15, 4-7) + flags
3-4     2     CRC lower byte (bits 8-11, 0-3) + flags
5       1     Send Sequence
6       1     Recv Sequence
7       1     Command Type
8+      varies  Payload (command-specific)
```

### CRC Calculation

The CRC16 is calculated over bytes 5 through length-1 using the standard Q-Link CRC16 algorithm.

---

## Command Types

| Command | Hex | Class | Description |
|---------|-----|-------|-------------|
| `CMD_START` | 0x5A | Constant | Frame start marker |
| `CMD_ACTION` | 0x20 | `Action` | Action messages (most protocol commands) |
| `CMD_PING` | 0x26 | `Ping` | Keep-alive ping |
| `CMD_RESET` | 0x23 | `Reset` | Connection reset |
| `CMD_RESETACK` | 0x24 | `ResetAck` | Reset acknowledgment |
| `CMD_WINDOWFULL` | 0x21 | `WindowFull` | Flow control (window full) |
| `CMD_SEQUENCEERROR` | - | `SequenceError` | Sequence error indicator |

---

## Implemented Action Messages

The action command space (0x20) contains the vast majority of protocol messages. Each action is identified by a 2-character mnemonic encoded in ISO-8859-1.

### Authentication & Session

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `DD` | `Login` | User login with account (10 chars) and code (4 chars) |
| `LO` | `Logoff` | User logout |
| `ID` | `IdentifyUser` | Query user status |
| `DL` | `DuplicateLogin` | Handle duplicate sessions |
| `UC` | `UpdatedCode` | Access code update notification |
| `IA` | `InvalidAccount` | Invalid account during login |
| `UI` | `UserInvalid` | Invalid user status |
| `US` | `SuspendServiceAck` | Service suspension acknowledgment |

### Chat System

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `AA` | `ChatSay` | Send chat message (string payload) |
| `AB` | `ChatSend` | Send to chat room |
| `AC` | `AnonChatSend` | Anonymous chat message |
| `EC` | `EnterChat` | Join chat room |
| `LC` | `LeaveChat` | Exit chat room |
| `EP` | `EnterPublicRoom` | Join public room |
| `ER` | `EnterPrivateRoom` | Join private room |
| `ES` | `EnterSuperChat` | Join premium SuperChat |
| `EA` | `EnterAuditorium` | Enter auditorium |
| `ET` | `EnterTheAuditorium` | Enter main auditorium |
| `AT` | `AuditoriumText` | Auditorium broadcast text |
| `LR` | `ListRooms` | List available rooms |
| `LM` | `ListMoreRooms` | Get additional room listings |
| `RL` | `RoomLine` | Individual room listing data |
| `RT` | `RoomText` | Room text messages |

### Email/OLM (OnLine Messages)

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `EN` | `SendEmail` | Send email message |
| `RE` | `ReadEmail` | Read email message |
| `ET` | `EmailText` | Email content text |
| `EL` | `EmailNextLine` | Request next email line |
| `EX` | `EmailLastLine` | End of email transmission |
| `EC` | `EmailCanceled` | Cancel email composition |
| `SO` | `SendOLM` | Send OnLine Message |
| `RO` | `ReadOLM` | Read OLM |
| `OT` | `OLMText` | OLM content text |
| `SS` | `SendSYSOLM` | Send System OLM |
| `OM` | `OM` | OLM management |
| `OC` | `OLMCancelled` | Cancel OLM |
| `NE` | `NewMail` | New mail notification |
| `NO` | `NoEmail` | No email available |

### File Transfer

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `DF` | `DownloadFile` | Download file request |
| `SD` | `StartDownload` | Begin file download |
| `FT` | `FileText` | File data payload |
| `FB` | `FileNextBlock` | Request next file block |
| `FX` | `FileLastBlock` | Final file block |
| `FC` | `FileCanceled` | Cancel file transfer |
| `FA` | `FileTextAck` | Acknowledge file data |
| `FP` | `FileTextPing` | File transfer keep-alive |
| `RU` | `ReadyFileUpload` | Upload ready |
| `ID` | `InitDownload` | Initialize download |
| `FD` | `FileDescriptionString` | File description |

### Messaging/Boards

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `EM` | `EnterMessageBoard` | Access message board |
| `IP` | `InitPosting` | Start message posting |
| `RP` | `RequestItemPost` | Post item to board |
| `PI` | `PostingItem` | Posting data |
| `NL` | `NextPostingLine` | Posting line data |
| `XL` | `LastPostingLine` | End of posting |
| `AP` | `AbortPosting` | Cancel posting |
| `PS` | `PostingSuccess` | Posting successful confirmation |

### Games

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `RG` | `RequestGame` | Request to join game |
| `SG` | `StartGame` | Begin game |
| `LG` | `LoadGame` | Load saved game state |
| `GM` | `GameMove` | Make a game move |
| `GS` | `GameSend` | Send data to game |
| `GN` | `GameNextPlayer` | Get next player info |
| `GX` | `GameLastPlayer` | End player info |
| `RS` | `RequestGameStart` | Start game request |
| `IV` | `InviteToGame` | Game invite |
| `AI` | `AcceptInvite` | Accept invitation |
| `DI` | `DeclineInvite` | Decline invitation |
| `RR` | `RequestGameRestart` | Restart game request |
| `AR` | `AcceptRestart` | Accept game restart |
| `DR` | `DeclineRestart` | Decline restart |
| `RA` | `RestartGameAck` | Restart acknowledged |
| `LE` | `LeaveGame` | Exit game |
| `GL` | `GameLine` | Game data line |
| `GP` | `GamePlayer` | Player data |
| `LO` | `LoadObservedGame` | Load game for observation |
| `OG` | `ObserveGame` | Observe game action |
| `PB` | `PlayBackMoves` | Replay game moves |
| `PA` | `PlayBackMovesAck` | Playback acknowledged |
| `TO` | `RequestToObserve` | Request observation rights |
| `IG` | `InvalidGameID` | Invalid game ID |
| `GC` | `GameCannotBeInitiated` | Game initialization failed |
| `NG` | `NoGames` | No games available |
| `GE` | `GameError` | Game error |
| `PE` | `PlayerInGameError` | Player game error |
| `PR` | `PlayerNotInRoomError` | Player not in room |
| `PN` | `PlayerNoResponse` | Player timeout |
| `PL` | `PlayerLeftGame` | Player left game |
| `PD` | `PlayerDeclinedInvite` | Invite declined |

### Partner Finder

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `FP` | `FindPartners` | Find compatible partners |
| `FM` | `FindMorePartners` | Get more partner listings |
| `PA` | `FindPartnersAck` | Partner list acknowledgment |
| `SP` | `SelectPartner` | Choose a partner |
| `IM` | `IncludeMe` | Add self to partner list |
| `XE` | `ExcludeMe` | Remove self from list |
| `CP` | `CancelPartnerSearch` | Cancel partner search |
| `PS` | `PartnerSearchStatusRequest` | Check search status |
| `PM` | `PartnerSearchMessage` | Partner search message |

### Reservations

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `MR` | `MakeReservation` | Make a reservation |
| `CR` | `CancelReservation` | Cancel reservation |

### Account Management

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `AP` | `AddPrimaryAccount` | Add primary account |
| `AS` | `AddSubAccount` | Add sub-account |
| `AA` | `AddAccountInSlot` | Add account to slot |
| `CE` | `ClearExtraAccounts` | Remove extra accounts |
| `DA` | `DeleteAccountInSlot` | Delete account from slot |
| `DA` | `DeleteAccountInSlotAck` | Delete acknowledgment |
| `CA` | `ChangeAccessCode` | Update access code |
| `AQ` | `AcceptingQuestions` | Enable question acceptance |
| `RQ` | `RejectingQuestions` | Disable question acceptance |
| `SF` | `SelectFileDocumentation` | File documentation selection |

### Dialog System

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `DA` | `DialogAllocated` | Dialog buffer allocated |
| `DT` | `DialogText` | Dialog text content |
| `DY` | `DialogYes` | Yes response |
| `DN` | `DialogNo` | No response |
| `DC` | `DialogCancel` | Cancel dialog |
| `CD` | `CreateDialog` | Create dialog buffer |
| `CA` | `ChatDialogAllocated` | Chat dialog allocated |
| `CT` | `ChatDialogText` | Chat dialog text |
| `CY` | `ChatDialogYes` | Chat dialog yes |
| `CN` | `ChatDialogNo` | Chat dialog no |
| `CC` | `ChatDialogClose` | Close chat dialog |
| `LY` | `YesNoMaybeRequest` | Yes/No/Maybe request |
| `LM` | `MenuDialogTextRequest` | Menu dialog prompt |

### System/Menu

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `SM` | `SelectMenuItem` | Menu item selection |
| `DM` | `DisplayMainMenu` | Show main menu |
| `EB` | `EnterBoxOffice` | Box office access |
| `EX` | `EnterExtRoom` | Enter external room |
| `EI` | `EventInfo` | Event information |
| `GM` | `GetMenuInfo` | Get menu information |
| `CS` | `ClearScreen` | Clear display screen |
| `EE` | `EnableEcho` | Enable local echo |
| `DE` | `DisableEcho` | Disable local echo |
| `DT` | `DrawsTopline` | Topline information |
| `LD` | `LoginDialogTextRequest` | Login dialog prompt |
| `MD` | `MenuDialogTextRequest` | Menu dialog prompt |

### Gateway/IRC

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `GE` | `GatewayEnter` | Enter gateway |
| `GS` | `GatewaySend` | Send to gateway |
| `GR` | `GatewayRecv` | Receive from gateway |
| `GA` | `GatewayMoreAck` | More data acknowledgment |
| `GD` | `GatewayDisconnect` | Gateway disconnect |

### List/Search

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `SL` | `SelectList` | Select from list |
| `SM` | `SelectMoreList` | Get more list items |
| `SD` | `SelectDateReply` | Date selection reply |
| `LS` | `ListSearch` | Search list |
| `AB` | `AbortSelectList` | Abort list selection |

### Protocol Commands (Admin)

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `/protocol` | `ProtocolCommand` | Admin protocol commands (capture, decode, status) |

### Habitat Integration

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `HA` | `HabitatAction` | Habitat virtual world actions |

### Error/Status

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `UN` | `UserNotOnline` | User not online |
| `UU` | `UserUnavailable` | User unavailable |
| `UP` | `UserInPrivateRoom` | User in private room |
| `IG` | `InvalidatedAccount` | Account invalidated |

### Unknown/Test

| Mnemonic | Class | Description |
|----------|-------|-------------|
| `C1`, `C2`, `C3` | Test classes | Unknown/test actions |
| `CA`, `CB`, `CL` | Test classes | Unknown/test actions |
| `D2`, `E2`, `EK` | Test classes | Unknown/test actions |
| `FO`, `MC`, `MF` | Test classes | Unknown/test actions |
| `OE`, `XX` | Test classes | Unknown/test actions |
| `JAM` | `JAM` | Test action |
| `Fp` | `Fp` | Test action |
| `ZA` | `ZA` | Test action |
| `UnknownAction` | `UnknownAction` | Unknown protocol capture |

---

## Reference Tables

### Command Class Hierarchy

```
Command (interface)
    └── AbstractCommand (abstract)
        ├── AbstractCheckedCommand (abstract, validates CRC)
        │   ├── Ping
        │   ├── WindowFull
        │   └── SequenceError
        └── Reset (no CRC validation)
```

```
Action (interface extends Command)
    └── AbstractAction (abstract, handles 2-char mnemonic)
        ├── AbstractStringAction (abstract, handles string payload)
        │   ├── ChatSay
        │   ├── SendEmail
        │   └── ...
        ├── AbstractMenuItem (abstract, menu items)
        ├── AbstractAddAccount (abstract, account management)
        ├── AbstractChatAction (abstract, chat operations)
        └── ... (many action-specific abstract classes)
```

### Protocol Flow Example: Chat Message

```
Client → Server:
  Frame: [0x5A][CRC][Seq][0x20][AA][Payload]
         ^       ^    ^    ^   ^   ^
         |       |    |    |   |   +-- ChatSay mnemonic "AA"
         |       |    |    |   +------ Command: 0x20 (ACTION)
         |       |    |    +---------- Recv Sequence
         |       |    +---------------- Send Sequence
         |       +--------------------- CRC16
         +----------------------------- Start marker

Server → Client:
  [Response based on state and room membership]
```

### Protocol Flow Example: Login

```
Client → Server (Reset):
  [0x5A][CRC][Seq][0x23][Version][Release][Padding...]

Server → Client (ResetAck):
  [0x5A][CRC][Seq][0x24][Version][Release][Padding...]

Client → Server (Login):
  [0x5A][CRC][Seq][0x20][DD][Account(10)][Code(4)]

Server → Client (Response):
  [State transition to Authentication state]
```

---

## Summary Statistics

| Metric | Count |
|--------|-------|
| **Total Action Classes** | 220+ |
| **Implemented Actions** | ~175+ (excluding abstract) |
| **Command Categories** | 4 main types |
| **Mnemonics** | 2-character ISO-8859-1 strings |
| **Protocol Version** | 9.5 |
| **Network Ports** | 2 (QTCP 5190, Habilink 1986) |

---

## See Also

- `QLinkLabels.label` - Original Q-Link label file
- `protocol/` - Q-Link protocol documentation files
- `source/` - Original source code files

---

*Last updated: 2026-03-20*
