# Agent Handoff: GUI, Makefile & Performance Work

Instructions for the next AI agent (or human) to pick up this branch. Delete this file
in the final commit before merging.

- **Branch:** `feature/gui-makefile-perf` (draft PR against `main`)
- **Last updated:** 2026-10-02
- **History:** A Codex session on 2026-09-23 wrote the bulk of the changes. A Claude Code
  session on 2026-10-02 verified them, fixed the test runner, and opened the draft PR.

## Goal

The original request, verbatim:

> Update [the Java chess repo] to support a real quality UI and improve performance. Run
> verification to ensure that functionality is the same or improved. Add a top-level
> makefile that will provide commands to start the server, start the cli, or start the new
> improved GUI. Clients can be connected over cli or GUI. Add descriptive messages if
> necessary build and run dependencies are missing.

## What is already done

| Area | Files |
| --- | --- |
| Swing desktop client (login/register, lobby, seat selection, observe, legal-move highlighting, board flip, promotion, resign) | `client/src/main/java/ui/gui/ChessGui.java`, `ChessBoardPanel.java`, `client/src/main/java/GuiMain.java` |
| Separate entry points (fixes a fat-jar `Main` collision between client and server) | `ClientMain.java`, `ServerMain.java`, `client/pom.xml`, `server/pom.xml`, `test-dependencies-assembly.xml` |
| Shared HTTP connection pool; CLI no longer busy-waits | `ui/ServerFacade.java`, `ui/Terminal.java`, `ui/GamePlay.java`, `ui/WebSocketClient.java` |
| Thread-safe server error and connection state | `server/Server.java`, `websocket/WSServer.java` |
| Makefile with dependency checks: `make server`, `cli`, `gui`, `build`, `test`, `test-engine`, `verify` | `Makefile` |
| Database configuration template (the real `db.properties` is gitignored) | `server/src/main/resources/db.properties.example` |
| README rewrite | `README.md` |
| New tests | `client/src/test/java/ui/gui/ChessBoardPanelTests.java`, `client/src/test/java/client/ServerFacadeTransportTests.java` |
| **Surefire pinned to 3.2.5** (Claude Code session) | root `pom.xml` |

### Verification status on 2026-10-02 (Linux, Java 21, Maven 3.8.7, no `db.properties`)

- `make build`: **passes**
- Chess rule tests (`shared`): **107/107 pass**
- Client GUI and transport tests: **pass**
- All server tests and `client.ServerFacadeTests`: **error, as expected**, because no
  MySQL credentials are configured. These are the tests still to verify.

> **Important gotcha:** Before the Surefire pin, Maven 3.8's default Surefire 2.12 silently
> ran **0** JUnit 5 tests and still reported `BUILD SUCCESS`. Always confirm that
> `Tests run:` is non-zero. Don't remove the pin.

## Remaining work, in order

### 1. Set up the new machine

```sh
# Ubuntu/Debian. On macOS use brew: openjdk@21, maven, mysql
sudo apt install -y openjdk-21-jdk maven mysql-server
sudo systemctl enable --now mysql
git fetch && git switch feature/gui-makefile-perf
make check          # confirms Java 21+ and Maven
```

Create the database and a dedicated user. The **user** must pick the password. Never
ask them to paste it into the chat, and never commit it.

```sql
-- sudo mysql
CREATE DATABASE chess;
CREATE USER 'chess'@'localhost' IDENTIFIED BY '<user-chosen password>';
GRANT ALL PRIVILEGES ON chess.* TO 'chess'@'localhost';
FLUSH PRIVILEGES;
```

```sh
cp server/src/main/resources/db.properties.example server/src/main/resources/db.properties
# The user edits db.password (and db.user if different) locally
```

### 2. Harden database startup (small code change)

`server/src/main/java/dataaccess/DatabaseManager.java`, `createDatabase()`:

- It runs `CREATE DATABASE IF NOT EXISTS <name>`. With `GRANT ALL ON chess.*` this should
  succeed, because a schema-level CREATE privilege covers that schema. Confirm that it does.
  If it fails with an access-denied error when the database already exists, catch that case
  and continue. Don't require global privileges.
- The `Connection` in `createDatabase()` is never closed, which leaks a connection. Wrap it
  in try-with-resources.
- Keep the README's "Database setup" section accurate after the change. It currently says
  the account "needs permission to create the named database".

### 3. Run the full verification

```sh
make verify                     # build + every test. All must pass, with non-zero counts
```

Expected: 107 rule tests and all of the server, DAO, service, WebSocket, database and client
facade tests pass. Fix any real regressions caused by this branch. To check whether a
failure already existed, compare against `main` in a separate worktree.

### 4. Manual end-to-end smoke test (needs a desktop session)

Use three terminals: `make server`, `make gui` and `make cli`.

1. Register two users, one in each client.
2. Create a game. One user joins as WHITE (GUI) and the other as BLACK (CLI).
3. Play several moves, including a capture. Both boards must update live.
4. Check that illegal moves are rejected and that the GUI highlights legal moves.
5. Open a third client as an observer. Then resign, and confirm that both players and the
   observer see the result.
6. Check `make gui HOST=... PORT=...` and `make server PORT=9090` with non-default values.

Report what was actually observed. If no desktop is available, say so; don't claim this step passed.

### 5. Review the diff before merge

- The branch deletes about 230 lines from existing client and server files. Diff against
  `main` and confirm that no CLI command or behavior was lost: help, register, login,
  logout, create, list, join, observe, redraw, leave, move, resign and highlight. The
  original command shortcuts came from commit `3417037`.
- Remove unused imports and dead code, and make sure no secrets or `db.properties` are
  tracked (`git ls-files | grep db.properties` should list only the `.example` file).

### 6. Finish

1. Delete this `HANDOFF.md`.
2. Update the PR description with the final verification results.
3. Mark the PR ready for review. **Ask the user before merging** into `main`.

## Environment notes

- Codex transcripts from the original session live on the first machine at
  `~/.codex/sessions/2026/09/23/rollout-2026-09-23T10-49-55-*.jsonl`. They aren't needed
  to continue.
- `jf300exc/RChess` is a separate, unrelated Rust project. Don't confuse the two.
