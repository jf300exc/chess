#!/usr/bin/env python3
"""Exercise actual raw input, mouse reports, resizing and restoration without MySQL.

Run after `make build` (which also compiles the Java test fixture).
Requires Python 3 and a POSIX pseudo-terminal; it does not open a window or connect to a server.
"""

import fcntl
import os
from pathlib import Path
import pty
import re
import select
import signal
import struct
import subprocess
import termios
import time


ROOT = Path(__file__).resolve().parents[1]
CLASSPATH = os.pathsep.join(str(ROOT / path) for path in (
    "client/target/test-classes", "client/target/classes", "shared/target/classes",
    "client/target/client-jar-with-dependencies.jar"))
JAVA = os.environ.get("CHESS_TEST_JAVA", "java")


def run_session(name, actions, args=(), term="xterm-256color", expected="e2e4", graphics=False,
                stable=False, expected_images=None, status=None, idle=None, no_color_env=False):
    master, slave = pty.openpty()
    fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", 24, 80, 0, 0))
    original_attributes = termios.tcgetattr(slave)

    def attach_terminal():
        os.setsid()
        fcntl.ioctl(0, termios.TIOCSCTTY, 0)

    # Exercise color deliberately, independently of the calling agent/shell's NO_COLOR preference.
    session_env = {key: value for key, value in os.environ.items() if key != "NO_COLOR"}
    if no_color_env:
        session_env["NO_COLOR"] = "1"
    process = subprocess.Popen(
        [JAVA, "-cp", CLASSPATH, "ui.TerminalSmokeMain", *args],
        stdin=slave, stdout=slave, stderr=slave, cwd=ROOT,
        env={**session_env, "TERM": term}, preexec_fn=attach_terminal)
    output = bytearray()
    probe_count = 0

    def collect(timeout=.1):
        nonlocal probe_count
        if select.select([master], [], [], timeout)[0]:
            output.extend(os.read(master, 65536))
        queries = output.count(b"\x1b[16t")
        if queries > probe_count:
            probe_count = queries
            if graphics:
                reply = (b"\x1b[?64;4c\x1b[6;0;0t" if graphics == "invalid" else
                         (b"mo" if graphics == "typed" else
                          b"\x1b[<0;21;16M\x1b[<0;21;12Mmo" if graphics == "stale" else b"")
                         + (b"\x1b[?80;1$y" if graphics == "mode80" else b"\x1b[?80;2$y")
                         + b"\x1b[?64;1;4c\x1b[6;18;9t")
                os.write(master, reply)

    def drain(seconds):
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            collect(.025)

    def wait_for(marker, timeout=10):
        deadline = time.monotonic() + timeout
        while marker not in output:
            if time.monotonic() >= deadline:
                raise AssertionError(f"{name}: missing {marker!r}\n{output.decode(errors='replace')}")
            collect()

    try:
        wait_for(b"Text controls:" if "--text" in args or term == "dumb" else b"CHESS")
        drain(.2)
        initial_clears = output.count(b"\x1b[2J")
        for action in actions:
            if isinstance(action, float):
                before_idle = len(output)
                drain(action)
                delta = bytes(output[before_idle:])
                if idle == "quiet":
                    assert not delta, "Reduced-motion idle screen unexpectedly emitted updates"
                elif idle == "animated":
                    assert b"\x1b[2;" in delta, "Waiting indicator did not update the status row"
                    assert b"\x1bP" not in delta and b"\x1b[2J" not in delta, "Animation repainted the board"
            elif isinstance(action, tuple):
                rows, columns = action
                fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", rows, columns, 0, 0))
                os.kill(process.pid, signal.SIGWINCH)
                drain(.4)
            else:
                os.write(master, action)
                drain(.15)
        wait_for(b"SMOKE moves=")
        process.wait(timeout=10)
        while select.select([master], [], [], .05)[0]:
            output.extend(os.read(master, 65536))
        result = output.decode(errors="replace")
        if stable:
            assert output.count(b"\x1b[2J") == initial_clears, "Ordinary updates cleared the screen"
        if expected_images is not None:
            assert result.count("\x1bP0;1;0q") == expected_images, "Unexpected board repaint count"
        if status:
            assert status in result, f"Missing status: {status}"
        if "--no-color" in args or no_color_env:
            assert all(code == "\x1b[0m" for code in re.findall(r"\x1b\[[0-9;]*m", result)), (
                "No-color mode emitted text color/style sequences")
        expected_count = 0 if expected == "none" else 1
        assert f"SMOKE moves={expected_count} first={expected} restored=true" in result, result
        restored_attributes = termios.tcgetattr(slave)
        # JLine normalizes PTY baud/control flags; verify the input, output, local
        # modes and control characters that matter for restoring echo/canonical input.
        assert all(restored_attributes[i] == original_attributes[i] for i in (0, 1, 3, 6)), (
            f"Terminal attributes were not restored\nbefore={original_attributes}\nafter={restored_attributes}")
        assert process.returncode == 0, result
        enlarged = graphics and graphics != "invalid" and "--no-graphics" not in args
        assert ("\x1bP0;1;0q" in result) == bool(enlarged), "Graphics detection/fallback did not match capability replies"
        if "--no-graphics" in args:
            assert "\x1b[16t" not in result, "--no-graphics unexpectedly queried pixels"
        if graphics == "mode80":
            assert "\x1b[?80l" in result and "\x1b[?80h" in result, "Sixel display mode was not restored"
        if enlarged:
            assert "enlarged board" in result
        elif "--pieces=ascii" in args:
            assert "White: KQRBNP" in result, "ASCII glyph option was not applied"
        elif "--pieces=nerd" in args and "--text" not in args and term != "dumb" and not no_color_env \
                and "--no-color" not in args:
            assert chr(0xF0857) in result, "Nerd Font king was not rendered"
        else:
            assert "♔" in result and "♚" in result, "Distinct Unicode team symbols were not rendered"
        if "--text" in args or term == "dumb":
            assert "\x1b[?1000h" not in result, "Fallback unexpectedly enabled mouse tracking"
        elif "--no-mouse" in args:
            assert "\x1b[?1000h" not in result, "--no-mouse unexpectedly enabled tracking"
        else:
            assert "\x1b[?1000h" in result and "\x1b[?1000l" in result, "Mouse mode was not restored"
        print(f"PASS {name}")
    finally:
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=5)
        os.close(master)
        os.close(slave)


def click(x, y, button=0, release=False):
    return f"\x1b[<{button};{x};{y}{'m' if release else 'M'}".encode()


run_session("SGR mouse, selection, release and cancel", [
    click(17, 10), click(17, 10, release=True), click(17, 8, 2),
    click(17, 10), click(17, 8), b"leave\n"])
run_session("legacy mouse reports", [
    b"\x1b[M" + bytes((32, 17 + 32, 10 + 32)),
    b"\x1b[M" + bytes((32, 17 + 32, 8 + 32)), b"leave\n"])
run_session("flipped board and compact resize", [
    b"flip\n", (18, 44), click(14, 5), click(14, 7), b"leave\n"])
run_session("large board resize", [
    (36, 90), click(35, 22), click(35, 16), b"leave\n"], graphics=True)
run_session("observer inspection cannot send moves", [
    click(17, 10), click(17, 8), b"move e2e4\n", b"leave\n"], ["--observer"], expected="none")
run_session("keyboard fallback with mouse disabled", [
    b"highlight e2\n", b"move e2e4\n", b"leave\n"], ["--no-mouse"])
run_session("dumb terminal fallback", [b"move e2e4\n", b"leave\n"], term="dumb")
run_session("explicit text mode", [b"move e2e4\n", b"leave\n"], ["--text"])
run_session("Nerd Font symbols preserve mouse alignment", [
    click(17, 10), click(17, 8), b"leave\n"], ["--pieces=nerd"])
run_session("ASCII single-character text fallback", [
    b"move e2e4\n", b"leave\n"], ["--text", "--pieces=ascii"])
run_session("Nerd Font text mode keeps teams distinct", [
    b"move e2e4\n", b"leave\n"], ["--text", "--pieces=nerd"])
run_session("Ctrl-C cleanup", [b"\x03"], expected="none")
run_session("Ctrl-D cleanup", [b"\x04"], expected="none")
run_session("mouse promotion chooses knight", [click(5, 5), click(5, 4), b"n\n", b"leave\n"],
            ["--promotion"], expected="a7a8=N")
run_session("help, text editing and selection preserve typed commands", [
    b"help\n", b"\n", b"sta", click(17, 10), b"tus\n", click(17, 8), b"leave\n"])
run_session("undersized screen keeps text controls", [
    (12, 28), b"move e2e4\n", b"leave\n"])
run_session("graphics board uses the same mouse hit map", [
    click(26, 16), click(26, 12), b"leave\n"], graphics=True)
run_session("graphics help, flip and compact resize", [
    b"help\n", b"\n", b"flip\n", (18, 44), click(14, 5), click(14, 7), b"leave\n"], graphics=True)
run_session("graphics detection preserves typed move prefix", [
    b"ve e2e4\n", b"leave\n"], graphics="typed")
run_session("malformed pixel replies use symbols", [
    b"move e2e4\n", b"leave\n"], graphics="invalid")
run_session("explicit symbol board keeps mouse input", [
    click(17, 10), click(17, 8), b"leave\n"], ["--no-graphics"])
run_session("graphics works independently of mouse support", [
    b"move e2e4\n", b"leave\n"], ["--no-mouse"], graphics=True)
run_session("graphics restores prior sixel display mode", [
    b"move e2e4\n", b"leave\n"], graphics="mode80")
run_session("pre-paint mouse input cannot submit stale moves", [
    b"ve e2e4\n", b"leave\n"], graphics="stale")
run_session("typing and notifications never repaint the graphical board", [
    b"sta", b"tus\n", b"nonsense\n", b"leave\n"], graphics=True, expected="none",
    stable=True, expected_images=1)
run_session("selection and cancel only repaint three affected squares each", [
    click(26, 16), b"\x1b", b"leave\n"], graphics=True, expected="none",
    stable=True, expected_images=7)
run_session("confirmed move only repaints source and destination", [
    b"move e2e4\n", b"leave\n"], ["--confirm-update"], graphics=True,
    stable=True, expected_images=3)
run_session("waiting animation keeps the board static", [1.2, b"status\n", b"leave\n"],
    ["--waiting"], graphics=True, expected="none", stable=True, expected_images=1,
    status="Waiting for BLACK", idle="animated")
run_session("reduced motion and no color retain turn information", [1.2, b"leave\n"],
    ["--waiting", "--no-animation", "--no-color"], graphics=True, expected="none",
    stable=True, expected_images=1, status="Waiting for BLACK", idle="quiet")
run_session("checkmate outcome names winner and keeps final board", [b"status\n", b"leave\n"],
    ["--checkmate"], graphics=True, expected="none", stable=True, expected_images=1,
    status="BLACK wins by checkmate")
run_session("resignation outcome does not guess the winning team", [b"leave\n"],
    ["--resigned"], graphics=True, expected="none", stable=True, expected_images=1,
    status="Alice has resigned")
run_session("NO_COLOR keeps text teams identifiable without Nerd icon color", [b"leave\n"],
    ["--pieces=nerd", "--no-graphics"], expected="none", stable=True, no_color_env=True)
run_session("duplicate server snapshots do not repaint the board", [1.2, b"leave\n"],
    ["--repeat-snapshot"], graphics=True, expected="none", stable=True, expected_images=1)
run_session("typing leaves the symbol board static too", [b"sta", b"tus\n", b"leave\n"],
    ["--no-graphics"], expected="none", stable=True)
