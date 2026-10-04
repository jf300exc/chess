# Chess features introduced by the last merged pull request

[PR #1 — Add Swing GUI client, Makefile, and performance fixes](https://github.com/jf300exc/chess/pull/1) was merged on October 3, 2026. It changed the project from a terminal-only client into a multiplayer application with interchangeable desktop and terminal clients. This comparison describes the merged change, not the unfinished CLI work in PR #2.

## Before and after

| Area | Before PR #1 | After PR #1 |
| --- | --- | --- |
| Playing interface | A command-line client was the only player interface. | A resizable Swing desktop client is available alongside the CLI. Both clients use the same server and can join the same match. |
| Accounts and lobby | Registration, login, listing games, and joining were driven by terminal prompts. | Desktop forms support registration and login. The lobby supports creating games, choosing an available seat, and observing games. Terminal commands remain available. |
| Board interaction | Players entered move coordinates and highlight commands. | Desktop players can select pieces and destinations, see legal-move highlights, flip the board, and choose a promotion piece. Observers can inspect games without submitting moves. |
| Match information | Gameplay notifications were printed in the terminal. | The GUI presents live activity and game state. Resignation is shown as game over, and a finished game disables the player board. |
| Navigation and activity | There was no desktop match navigation or scrollable activity panel. | Users can leave matches and return to the lobby. Old match callbacks cannot overwrite the next screen. The activity log follows new messages only when the reader is already at the bottom. |
| Launching applications | Users generally invoked Maven or Java directly. Client and server fat jars could collide on their `Main` entry point. | Separate entry points and `make server`, `make cli`, and `make gui` provide predictable launch commands. Host and port are configurable without editing source. |
| Setup and testing | Build and runtime prerequisites were less explicit, and JUnit 5 execution was not reliably configured. | Make targets check Java, Maven, database configuration, and desktop availability. `build`, `test`, `test-engine`, and `verify` are documented. Surefire 3.2.5 runs the JUnit 5 tests. |
| Responsiveness | Client HTTP requests did not share the new connection pool; terminal waits could busy-spin. | Requests reuse an HTTP connection pool, GUI network calls stay off the Swing event thread, and terminal waits sleep instead of spinning. |
| Multiplayer reliability | Connection bookkeeping and disconnect paths had gaps, including the last observer leaving a match. | Server connection/error handling supports concurrent clients. Player move broadcasts survive the last observer disconnecting. Closed sends, failed logout, and stale callbacks receive explicit handling. |
| Game selection | A lobby list number could be confused with a nonsequential server game ID. | The CLI resolves the selected list entry to its actual game ID. |
| Database setup | Real configuration filenames could be tracked, and privilege requirements were less clear. | Real `db.properties` files are ignored, example files are tracked, and setup documents database-scoped privileges and a separate test database. The startup connection is closed. |

## What stayed the same

The HTTP and WebSocket multiplayer protocol, MySQL persistence, and chess rules remain shared between clients. Castling, en passant, promotion, check, checkmate, and stalemate were existing engine features; PR #1 did not introduce them. The original CLI remained usable, but mouse control and the new adaptive terminal renderer were not part of that merged PR.

## Example workflow

Before, a player used terminal commands to authenticate, choose a game, and enter moves. After, one player can use `make gui` to log in, select a seat, and click moves while another uses `make cli` in the same game. An observer can follow the match in the GUI.

```sh
make server PORT=9090
make gui HOST=127.0.0.1 PORT=9090
make cli HOST=127.0.0.1 PORT=9090
```

## Verification recorded in the merged PR

PR #1 reports 217 passing tests at its full-suite checkpoint, followed by a passing build and five GUI lifecycle tests for the final activity-scroll change. Its end-to-end smoke exercised a GUI player, CLI player, and GUI observer against the same server. These are historical results from that PR, not a claim that the complete suite has been rerun for PR #2.

The comparison is grounded in the merged PR description and the repository changes between the first parent of merge commit `c0dcb0a` and that merge commit. Native desktop use was not manually tested in the recorded PR #1 run; its GUI verification used Xvfb.
