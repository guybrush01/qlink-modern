# Q-Link Reloaded — Proposed Improvements

Generated: 2026-04-23

## Architecture & Structure

### 1. Add unit test suite (CRITICAL)
The project has zero tests despite JUnit 5 and Mockito being declared in `pom.xml`. The `TESTING_PLAN.md` exists but was never executed. This is the highest-impact improvement — any refactoring or bug fix is untestable without coverage.

**Start with:**
- State machine states (`org.jbrain.qlink.state.*`) — deterministic, easy to mock
- DAOs (`org.jbrain.qlink.db.dao.*`) — use an embedded H2 database
- `SecurityUtils` — pure functions, trivial to test
- `Action` subclasses — serialization/deserialization roundtrips

### 2. Extract SecurityUtils as shared validation layer
`SecurityUtils` (handle/message/email/filename validation) is a solid start but isn't used consistently. Several code paths still validate input ad-hoc or not at all.

**How to apply:** Centralize all input validation through `SecurityUtils` and apply it at protocol boundaries (raw bytes → Command → Action pipeline).

### 3. Migrate remaining JDBC to Spring Data JPA
The project has a partial Spring Data JPA layer (`AccountRepository`, `UserRepository`) alongside hand-rolled `BaseDAO` with raw JDBC. The `BaseDAO` template is used in 14+ DAO classes.

**Goal:** Migrate the remaining DAOs to Spring Data repositories. This eliminates boilerplate, enables query compilation checks at build time, and unifies the data access layer.

---

## Bug Fixes

### 4. Fix NPE in AbstractRoomDelegate.changeUserName() (line ~340)
The code checks `if (info != null)` but then calls `info2.setIgnore(false)` — `info2` can be null, causing an NPE. Mis-typed variable name.

### 5. Fix String `==` comparison in HabitatConnection (line ~153)
`useFraming.toLowerCase() == "true"` compares object identity, never value. Always evaluates to `false`, so framing is never enabled. Use `.equals("true")`.

### 6. Fix broken resource management in FileDAO.getFileDataStream()
The method's finally block closes the ResultSet after returning `rs.getBinaryStream()`, immediately invalidating the InputStream the caller receives. The caller gets a dead stream.

### 7. Fix ResultSet leak in TocDAO.getNextReferenceId()
A `do/while` loop reassigns `rs = stmt.executeQuery()` without closing the prior ResultSet each iteration.

### 8. Fix unsafe Exception→RuntimeException cast in QLinkServer (lines 343, 376, 385)
`(RuntimeException) e` on a caught `Exception` throws `ClassCastException` if `e` is a checked exception, masking the original error. Either catch `RuntimeException` separately or handle generically without casting.

---

## Concurrency

### 9. Fix race conditions in AbstractRoomDelegate
- `addAdminUser()` — TOCTOU race: `_htAdmins.get()` then `_htAdmins.put()` without synchronization
- `leave()` — synchronizes on `_htUsers` but updates `_htAdmins` outside the lock
- `_alGames` — non-thread-safe `ArrayList` accessed from multiple threads

**Fix:** Synchronize all access to shared maps/lists or migrate to `ConcurrentHashMap` / `CopyOnWriteArrayList`.

### 10. Replace static `Random` with `ThreadLocalRandom`
`AbstractRoomDelegate._die` is a `static Random` shared across all room instances. `java.util.Random` is not thread-safe for concurrent `nextInt()` calls. Replace with `ThreadLocalRandom.current().nextInt(...)`.

### 11. Make RoomManager singleton thread-safe
The `_mgr` field is neither `volatile` nor `final`. Make it `final` — eager initialization (`= new RoomManager()`) makes this trivial.

---

## Dependencies

### 12. Update outdated dependencies

| Dependency | Current | Recommended | Notes |
|---|---|---|---|
| commons-beanutils | 1.9.4 | 1.9.9 | CVE-2021-44228 fix |
| HikariCP | 4.0.3 | 5.x | Java 17+ optimizations |
| Spring Framework | 5.3.39 | 5.3.latest or 6.x | 6.x requires Jakarta EE migration |
| Hibernate | 5.6.15 | 5.6.latest or 6.x | Match Spring Data version |
| commons-configuration | 1.10 | 2.x | API break; 1.x in maintenance mode |

### 13. Remove dead code
- `DBUtils` class is marked `@Deprecated` — delete it
- `RoomManager` has `for (int i = 1; i < 1; i++)` loops that never execute — leftover test code
- `EscapedOutputStream` appears unused — audit and remove if true

---

## Security

### 14. Use `SecureRandom` in SecurityUtils.generateSecureToken()
The method name says "secure" but uses `java.util.Random`, which is predictable. Replace with `java.security.SecureRandom`.

### 15. Fix swallowed IOException in EscapedInputStream (line ~68)
An IOException during the read loop is silently dropped, returning a partial buffer with no error indication. This masks network failures from the caller.

### 16. Fix IP validation regex in QLinkServer (lines 332, 365)
`^(\\d1,3}\\.\\d1,3}\\.\\d1,3}\\.\\d1,3}|...)` — the `{` is missing from the `{1,3}` quantifier. The IPv4 sub-pattern never matches; only the `[^\\s]+` fallback fires, making the validation a no-op.

---

## Build & DevOps

### 17. Add CI workflow
No `.github/workflows/` directory exists. Add a GitHub Actions workflow:
```
push/PR → mvn verify → test → checkstyle → PMD → SpotBugs
```

### 18. Fix Docker HEALTHCHECK
The HEALTHCHECK uses `curl -f http://localhost:5190/` but the server speaks the Q-Link binary protocol on port 5190, not HTTP. The health check always fails. Use a TCP probe (`bash -c 'echo > /dev/tcp/localhost/5190'`) or add a dedicated HTTP health endpoint.

---

## Code Quality

### 19. Consolidate Action subclasses
The 219 classes in `cmd/action/` are largely thin data carriers. Many could be consolidated into a smaller set of Java `record` types, or generated from a protocol definition file to reduce boilerplate.

### 20. Remove javadoc/ from source control
Generated Javadoc HTML is checked into the repo. Add `javadoc/` to `.gitignore`.
