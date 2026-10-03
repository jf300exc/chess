#!/usr/bin/env python3
"""Exercise actual raw input, mouse reports, resizing and restoration without MySQL.

Run after `make build` (which also compiles the Java test fixture).
Requires Python 3 and a POSIX pseudo-terminal; it does not open a window or connect to a server.
"""

import fcntl
import os
from pathlib import Path
import pty
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


def run_session(name, actions, args=(), term="xterm-256color", expected="e2e4"):
    master, slave = pty.openpty()
    fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", 24, 80, 0, 0))
    original_attributes = termios.tcgetattr(slave)

    def attach_terminal():
        os.setsid()
        fcntl.ioctl(0, termios.TIOCSCTTY, 0)

    process = subprocess.Popen(
        [JAVA, "-cp", CLASSPATH, "ui.TerminalSmokeMain", *args],
        stdin=slave, stdout=slave, stderr=slave, cwd=ROOT,
        env={**os.environ, "TERM": term}, preexec_fn=attach_terminal)
    output = bytearray()

    def wait_for(marker, timeout=10):
        deadline = time.monotonic() + timeout
        while marker not in output:
            if time.monotonic() >= deadline:
                raise AssertionError(f"{name}: missing {marker!r}\n{output.decode(errors='replace')}")
            if select.select([master], [], [], .1)[0]:
                output.extend(os.read(master, 65536))

    try:
        wait_for(b"Text controls:" if "--text" in args or term == "dumb" else b"CHESS")
        for action in actions:
            if isinstance(action, tuple):
                rows, columns = action
                fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", rows, columns, 0, 0))
                os.kill(process.pid, signal.SIGWINCH)
                time.sleep(.25)
            else:
                os.write(master, action)
                time.sleep(.15)
        wait_for(b"SMOKE moves=")
        process.wait(timeout=10)
        while select.select([master], [], [], .05)[0]:
            output.extend(os.read(master, 65536))
        result = output.decode(errors="replace")
        expected_count = 0 if expected == "none" else 1
        assert f"SMOKE moves={expected_count} first={expected} restored=true" in result, result
        restored_attributes = termios.tcgetattr(slave)
        # JLine normalizes PTY baud/control flags; verify the input, output, local
        # modes and control characters that matter for restoring echo/canonical input.
        assert all(restored_attributes[i] == original_attributes[i] for i in (0, 1, 3, 6)), (
            f"Terminal attributes were not restored\nbefore={original_attributes}\nafter={restored_attributes}")
        assert process.returncode == 0, result
        if "--pieces=ascii" in args:
            assert "White: KQRBNP" in result, "ASCII glyph option was not applied"
        elif "--pieces=nerd" in args and "--text" not in args and term != "dumb":
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
    click(26, 16), click(26, 16, release=True), click(26, 12, 2),
    click(26, 16), click(26, 12), b"leave\n"])
run_session("legacy mouse reports", [
    b"\x1b[M" + bytes((32, 26 + 32, 16 + 32)),
    b"\x1b[M" + bytes((32, 26 + 32, 12 + 32)), b"leave\n"])
run_session("flipped board and compact resize", [
    b"flip\n", (18, 44), click(14, 5), click(14, 7), b"leave\n"])
run_session("large board resize", [
    (36, 90), click(35, 22), click(35, 16), b"leave\n"])
run_session("observer inspection cannot send moves", [
    click(26, 16), click(26, 12), b"move e2e4\n", b"leave\n"], ["--observer"], expected="none")
run_session("keyboard fallback with mouse disabled", [
    b"highlight e2\n", b"move e2e4\n", b"leave\n"], ["--no-mouse"])
run_session("dumb terminal fallback", [b"move e2e4\n", b"leave\n"], term="dumb")
run_session("explicit text mode", [b"move e2e4\n", b"leave\n"], ["--text"])
run_session("Nerd Font symbols preserve mouse alignment", [
    click(26, 16), click(26, 12), b"leave\n"], ["--pieces=nerd"])
run_session("ASCII single-character text fallback", [
    b"move e2e4\n", b"leave\n"], ["--text", "--pieces=ascii"])
run_session("Nerd Font text mode keeps teams distinct", [
    b"move e2e4\n", b"leave\n"], ["--text", "--pieces=nerd"])
run_session("Ctrl-C cleanup", [b"\x03"], expected="none")
run_session("Ctrl-D cleanup", [b"\x04"], expected="none")
run_session("mouse promotion chooses knight", [click(6, 6), click(6, 4), b"n\n", b"leave\n"],
            ["--promotion"], expected="a7a8=N")
run_session("help, text editing and selection preserve typed commands", [
    b"help\n", b"\n", b"sta", click(26, 16), b"tus\n", click(26, 12), b"leave\n"])
run_session("undersized screen keeps text controls", [
    (12, 28), b"move e2e4\n", b"leave\n"])
