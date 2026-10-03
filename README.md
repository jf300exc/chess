# ♞ Multiplayer Chess

A Java 21 client/server chess application with interchangeable desktop and terminal clients. Multiple players and observers can connect to the same server over HTTP and WebSocket, while MySQL provides persistent users, sessions, and games.

## What is included

- A polished, resizable desktop client with login and registration, game lobby, seat selection, observation mode, legal-move highlighting, board flipping, promotion selection, live activity, resignation, and navigation between matches.
- The original terminal client, kept protocol-compatible for mixed CLI/GUI matches.
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
make test         # complete unit and integration suite; MySQL required
make verify       # package everything, then run the complete suite
```

Swing lifecycle tests require a display. On a Linux build machine, install Xvfb and run `xvfb-run -a make verify` to include them; without a display they are reported as skipped.

You can still invoke Maven directly (`mvn test`, `mvn package`, or individual module goals). The Makefile is the supported operator interface because it checks Java, Maven, desktop, and database prerequisites before launching.

## Architecture

The Maven reactor contains three modules:

- `shared`: chess rules, models, requests, and WebSocket message contracts
- `client`: terminal and Swing desktop clients sharing one HTTP/WebSocket transport
- `server`: Spark HTTP/WebSocket server and MySQL data access

[![Architecture sequence diagram](10k-architecture.png)](10k-architecture.png)

Performance-sensitive client requests reuse a single Java HTTP connection pool, UI network work stays off the Swing event thread, terminal waits sleep instead of busy-spinning, and server connection/error state is safe for concurrent clients.
