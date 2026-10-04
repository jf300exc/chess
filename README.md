# ♞ Multiplayer Chess

A Java 21 client/server chess application with interchangeable browser, desktop, and terminal clients. Multiple players and observers can connect to the same server over HTTP and WebSocket, while MySQL provides persistent users, sessions, and games.

## What is included

- A mobile-friendly browser client with accounts, a game lobby, touch and keyboard board controls, legal-move highlights, promotion choices, observers, and automatic reconnection. Open the server address to play; no app installation or frontend build is needed.
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

The browser, CLI, and GUI can be used at the same time and can join the same match.

### Play from a phone or browser

Start `make server`, then open `http://localhost:8080` on the server computer. On a phone or another computer on the same network, open `http://<server-LAN-IP>:8080` (for example, `http://192.168.1.20:8080`). The server listens on all interfaces; if a firewall is enabled, allow its configured TCP port. The phone uses the server that served the page automatically, including its port.

Sign in or create an account, create a game, and choose **Play White** or **Play Black**. A second player can open the same address and take the other seat. **Watch** joins as an observer. Tap or click a piece, then a highlighted destination; promotions offer queen, rook, bishop, and knight. Keyboard users can Tab to squares and activate them with Enter or Space. Escape or **Clear selection** cancels selection. **Flip board** changes the view while preserving your playing color.

The browser stores the session and current match in per-tab session storage so reloading resumes your seat. Passwords are never stored. Losing the connection pauses moves and reconnects with a fresh server snapshot. **Back to lobby** releases a connected player's seat after confirmation; disconnecting or closing a tab keeps the seat available to resume. Resignation also asks for confirmation. The final position remains visible after checkmate, stalemate, or resignation.

The existing API playground is available at `/api/index.html`. Browser assets ship inside the server jar and require no JavaScript package installation at runtime. HTTPS deployments use `wss:` automatically; configure the reverse proxy to pass WebSocket upgrades for `/ws`.

Screenshots and implementation details are in the [browser client comparison](docs/web-client.md).

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

The board keeps recognizable chess-piece symbols. When the terminal positively advertises SIXEL graphics and reports valid pixel geometry, only the board pieces are enlarged. Font outlines are centered by their visible bounds inside each square, with light/outlined White pieces and dark Black pieces directly on the square background—no rectangular piece badges. A standard 80×24 terminal fits the board at a normal text font size. Selection and legal destinations use gold and cyan backgrounds, and turn/check state stays visible. Larger windows allow larger squares.

Normal input does not clear the screen. Text updates use fixed-position differences, and the graphical board compares square pixels: typing and notifications leave it untouched, while a normal move emits local patches around its source and destination. Patches occupy complete terminal rows and six-pixel bands, copying any needed neighboring pixels without spilling outside the board. This avoids black bottom-edge padding at font sizes whose squares are not a multiple of six pixels high. Resizing, switching to help, and an explicit `redraw` can still require a full refresh.

Gameplay uses green for `YOUR TURN`, red for check/errors, cyan for waiting/notifications, and yellow for warnings and game-over results. Checkmate identifies the winning team; stalemate identifies a draw; resignation shows the server's message without guessing a winner. The final board stays available to inspect or flip before `leave` returns to the lobby. Opponent-turn waiting uses a small status-line spinner, updated twice per second; turn-start cues stay steady, with no flashing, sounds, or piece animations.

Use `--no-animation` for a completely still waiting indicator. `--no-color`, or a nonempty `NO_COLOR` environment variable, disables gameplay text styles and symbol-board colors; enlarged board images retain their piece/square colors. Uncolored Nerd Font text falls back to hollow/filled Unicode pieces so the teams remain distinguishable. Plain text mode never adds animation or colors.

Terminals without graphics use a compact one-line-per-rank symbol board (`♔♕♖♗♘♙` / `♚♛♜♝♞♟`), without badges or empty-square dot clutter. Mouse input remains available independently of graphics. Ordinary ANSI text cannot enlarge an individual character separately from its terminal font; padding alone is not treated as enlargement. `--no-graphics` explicitly selects this fallback. Plain output retains dots to distinguish empty squares.

For Nerd Font chess icons, launch with `--pieces=nerd`. The graphics renderer uses locally installed **JetBrainsMono Nerd Font** outlines if available, otherwise standard chess shapes. No font download is required at runtime. For the text fallback, select **JetBrainsMono Nerd Font Mono** (or another Nerd Font Mono) in your terminal's settings; the Mono variant keeps single-column icons consistent. The icons use the [official Nerd Fonts v3 Material Design chess glyphs](https://github.com/ryanoasis/nerd-fonts/blob/master/glyphnames.json). Default pieces remain Unicode. `--pieces=ascii` disables graphics and uses uppercase for White and lowercase for Black, one character per square.

```sh
make cli CLI_ARGS="--pieces=nerd"     # Chess icons with a Nerd Font Mono terminal font
make cli CLI_ARGS="--pieces=unicode"  # Standard white/black chess symbols (default)
make cli CLI_ARGS="--pieces=ascii"    # Single-character KQRBNP / kqrbnp fallback
make cli CLI_ARGS="--no-graphics"     # Compact symbol board, retaining supported mouse input
make cli CLI_ARGS="--no-animation"    # Steady waiting status, without a spinner
make cli CLI_ARGS="--no-color --no-graphics"  # Uncolored gameplay text and symbol board
```

JLine detects terminal capabilities. When mouse reporting is unavailable, gameplay uses keyboard commands. Non-interactive input, `TERM=dumb`, or unavailable terminal controls automatically use a plain board with no mouse or cursor escape sequences. Text mode retains standard Unicode team symbols (even with `--pieces=nerd`, whose icons otherwise rely on color to distinguish teams). Add `--pieces=ascii` for completely ASCII board output. You can also force the mode:

```sh
make cli CLI_ARGS="--no-mouse"  # Keep the interactive board, use keyboard commands
make cli CLI_ARGS="--text"      # Plain board and line input; suitable for pipes/limited terminals
make cli CLI_ARGS="--text --pieces=ascii"  # Plain input and ASCII board
```

Mouse tracking is enabled only during gameplay. Leaving, Ctrl-C/EOF, and connection errors restore the previous terminal mode and screen. `help` opens a paged help view in interactive mode. The lobby lists both game numbers and server IDs; enter `id 902` to choose an ID explicitly.

The [last merged PR comparison](docs/pr-1-before-and-after.md) explains the desktop and build changes. The [CLI PR comparison](docs/pr-2-before-and-after.md) records this PR's goals, before-and-after screenshots, validation, and terminal limitations.

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
make test-web     # browser checks against a running disposable test server; Playwright/Chrome required
make test         # complete unit and integration suite; MySQL required
make verify       # package everything, then run the complete suite
```

Swing lifecycle tests require a display. On a Linux build machine, install Xvfb and run `xvfb-run -a make verify` to include them; without a display they are reported as skipped.

After `make build`, run `python3 scripts/test-cli-terminal.py` on POSIX to exercise real pseudo-terminal mouse reports, both board orientations, resizing, keyboard/text fallbacks, and terminal restoration without a database or network server.

You can still invoke Maven directly (`mvn test`, `mvn package`, or individual module goals). The Makefile is the supported operator interface because it checks Java, Maven, desktop, and database prerequisites before launching.

### Browser regression checks

Use a separate database and run the server against it before running browser tests. The browser script creates unique test accounts and games and never clears the database. Java integration tests do clear their configured database.

The optional test harness uses Node.js, Playwright, and Chrome. Install the test dependency outside the checkout:

```sh
npm install --prefix /tmp/chess-browser-tests playwright
NODE_PATH=/tmp/chess-browser-tests/node_modules make test-web WEB_URL=http://localhost:8080
```

Set `CHROME_BIN` if Chrome is not at `/usr/bin/google-chrome-stable`; set `CHESS_WEB_SCREENSHOTS=/tmp/chess-web-shots` to save full-page screenshots. Tests cover accounts, lobby, touch moves, observers, orientation, network/reload recovery, game-over behavior, promotion, castling, en passant, and phone/desktop widths.

## Architecture

The Maven reactor contains three modules:

- `shared`: chess rules, models, requests, and WebSocket message contracts
- `client`: terminal and Swing desktop clients sharing one HTTP/WebSocket transport
- `server`: Spark HTTP/WebSocket server, MySQL data access, and the static browser client

[![Architecture sequence diagram](10k-architecture.png)](10k-architecture.png)

Performance-sensitive client requests reuse a single Java HTTP connection pool, UI network work stays off the Swing event thread, terminal input polls without busy-spinning, and server connection/error state is safe for concurrent clients. Lobby and gameplay share one input reader. Gameplay paints synchronized snapshots on the input thread; server callbacks update state without writing over typed commands in interactive mode.
