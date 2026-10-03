# ♞ Multiplayer Chess

A Java 21 client/server chess application with interchangeable desktop and terminal clients. Multiple players and observers can connect to the same server over HTTP and WebSocket, while MySQL provides persistent users, sessions, and games.

## What is included

- A polished, resizable desktop client with login and registration, game lobby, seat selection, observation mode, legal-move highlighting, board flipping, promotion selection, live activity, resignation, and navigation between matches.
- A terminal client with adaptive boards, mouse selection and movement where supported, text commands, and mixed CLI/GUI matches.
- Complete chess rules, including castling, en passant, promotion, check, checkmate, and stalemate.
- A multiplayer server with REST endpoints for accounts and games plus WebSocket gameplay updates.

## Quick start

Requirements:

- JDK 21 or newer
- Apache Maven 3.8 or newer
- GNU Make
- MySQL for the server and full integration tests
- A graphical desktop for the GUI (the CLI works without one)

Run `make` to see every command. The three application entry points are:

```sh
make server
make cli
make gui
```

The server defaults to port `8080`. Both clients default to `localhost:8080`. Override either value without editing code:

```sh
make server PORT=9090
make gui HOST=192.168.1.20 PORT=9090
make cli HOST=192.168.1.20 PORT=9090
```

The CLI and GUI can be used at the same time and can join the same match.

### Terminal gameplay commands

The terminal client accepts commands without regard to capitalization or extra spacing. While playing or observing, use:

```text
status                  Show the current turn, role, and check state
moves e2                List legal moves for the piece on e2
move e2e4               Submit a move directly from the command prompt
move e7e8=Q             Submit a promotion without a follow-up prompt
highlight e2            Highlight legal destinations on the board
flip                    View the board from the opposite perspective
```

Coordinate moves can also be written with spaces or a hyphen (`e2 e4` or `e2-e4`). `help` lists the available commands in each client state.

In a terminal with mouse reporting, click one of your pieces to highlight its legal destinations, then click a highlighted square to move. Click the selected piece again, right-click, or press Escape to cancel. Observers can click pieces to inspect moves but cannot submit moves. Promotions prompt for queen, rook, knight, or bishop; entering a blank response cancels the move. Your playing color stays the same when you flip the view.

The board uses high-contrast `wK`/`bK` piece labels (`w` = White, `b` = Black; `N` = knight) and spacious cells, so small chess-font glyphs no longer determine readability. It adapts from a one-line compact board to two- or three-line squares as the window grows, with coordinates and an on-screen legend. A standard 80×24 terminal fits the board without increasing the terminal font size. Selection and legal destinations use gold and cyan backgrounds, and the current turn/check state stays visible.

JLine detects terminal capabilities. When mouse reporting is unavailable, gameplay uses keyboard commands. Non-interactive input, `TERM=dumb`, or unavailable terminal controls automatically use a plain ASCII board with no mouse or cursor escape sequences. You can also force the mode:

```sh
make cli CLI_ARGS="--no-mouse"  # Keep the interactive board, use keyboard commands
make cli CLI_ARGS="--text"      # Plain board and line input; suitable for pipes/limited terminals
```

Mouse tracking is enabled only during gameplay. Leaving, Ctrl-C/EOF, and connection errors restore the previous terminal mode and screen. `help` opens a paged help view in interactive mode. The lobby lists both game numbers and server IDs; enter `id 902` to choose an ID explicitly.

## Database setup

The server intentionally does not commit credentials. Copy the example and edit it for your MySQL installation:

```sh
cp server/src/main/resources/db.properties.example server/src/main/resources/db.properties
```

Set `db.host`, `db.port`, `db.name`, `db.user`, and `db.password`. Use a dedicated MySQL account with privileges on the configured database only. For example, run the following as a MySQL administrator, choosing your own password locally:

```sql
CREATE DATABASE IF NOT EXISTS chess;
CREATE USER 'chess'@'localhost' IDENTIFIED BY '<your password>';
GRANT ALL PRIVILEGES ON chess.* TO 'chess'@'localhost';
```

The server creates the configured database and tables if needed; schema-level privileges are sufficient and global privileges are unnecessary. `make server`, `make test`, and `make verify` stop with an actionable message when the configuration file is missing. Full integration tests clear the configured database, so use a separate database for testing rather than one containing games you want to keep.

## Build and verification

```sh
make build        # compile and package runnable jars
make test-engine  # chess rules only; no database needed
make test-cli     # terminal commands, input decoding, coordinates, and gameplay; no database/display
make test         # complete unit and integration suite; MySQL required
make verify       # package everything, then run the complete suite
```

Swing lifecycle tests require a display. On a Linux build machine, install Xvfb and run `xvfb-run -a make verify` to include them; without a display they are reported as skipped.

After `make build`, run `python3 scripts/test-cli-terminal.py` on POSIX to exercise real pseudo-terminal mouse reports, both board orientations, resizing, keyboard/text fallbacks, and terminal restoration without a database or network server.

You can still invoke Maven directly (`mvn test`, `mvn package`, or individual module goals). The Makefile is the supported operator interface because it checks Java, Maven, desktop, and database prerequisites before launching.

## Architecture

The Maven reactor contains three modules:

- `shared`: chess rules, models, requests, and WebSocket message contracts
- `client`: terminal and Swing desktop clients sharing one HTTP/WebSocket transport
- `server`: Spark HTTP/WebSocket server and MySQL data access

[![Architecture sequence diagram](10k-architecture.png)](10k-architecture.png)

Performance-sensitive client requests reuse a single Java HTTP connection pool, UI network work stays off the Swing event thread, terminal input polls without busy-spinning, and server connection/error state is safe for concurrent clients. Lobby and gameplay share one input reader. Gameplay paints synchronized snapshots on the input thread; server callbacks update state without writing over typed commands in interactive mode.
