#!/usr/bin/env python3
"""Capture an actual terminal window in an isolated Xvfb display, not the user's desktop.

Run after make build. Optional QA tools: Xvfb, xwininfo, ImageMagick import,
and xterm or Konsole. No database or game server is used.
"""
import argparse
import os
from pathlib import Path
import select
import shutil
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--emulator", choices=("xterm", "konsole"), default="xterm")
parser.add_argument("--pieces", choices=("unicode", "nerd"), default="unicode")
parser.add_argument("--no-graphics", action="store_true")
parser.add_argument("--scene", choices=("selection", "waiting", "checkmate", "resigned", "updated", "flipped"),
                    default="selection")
parser.add_argument("--output", type=Path, required=True)
args = parser.parse_args()
for dependency in ("Xvfb", "xwininfo", "import", args.emulator):
    if shutil.which(dependency) is None:
        parser.error(f"Optional screenshot dependency missing: {dependency}")

root = Path(__file__).resolve().parents[1]
classpath = os.pathsep.join(str(root / path) for path in (
    "client/target/test-classes", "client/target/classes", "shared/target/classes",
    "client/target/client-jar-with-dependencies.jar"))
java = os.environ.get("CHESS_TEST_JAVA", "java")
read_fd, write_fd = os.pipe()
server = subprocess.Popen(["Xvfb", "-displayfd", str(write_fd), "-screen", "0", "1100x700x24", "-nolisten", "tcp"],
                          pass_fds=(write_fd,), stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
os.close(write_fd)
window = None
try:
    if not select.select([read_fd], [], [], 5)[0]:
        raise RuntimeError("Xvfb did not allocate a display within five seconds")
    with os.fdopen(read_fd) as display_pipe:
        number = display_pipe.readline().strip()
    if not number.isdigit():
        raise RuntimeError("Xvfb did not allocate a display")
    env = {key: value for key, value in os.environ.items() if key != "NO_COLOR"}
    env.update({"DISPLAY": ":" + number, "QT_QPA_PLATFORM": "xcb"})
    title = "Chess-CLI-isolated-QA"
    command = [java, "-cp", classpath, "ui.TerminalSmokeMain", "--preview", "--pieces=" + args.pieces]
    if args.scene != "selection":
        command.append("--" + args.scene)
    if args.no_graphics:
        command.append("--no-graphics")
    launch = (["xterm", "-title", title, "-fa", "JetBrainsMono Nerd Font Mono", "-fs", "11",
               "-geometry", "80x24", "-xrm", "XTerm*decTerminalID: vt340",
               "-xrm", "XTerm*allowWindowOps: true", "-e", *command] if args.emulator == "xterm" else
              ["konsole", "--separate", "--builtin-profile", "--hide-menubar", "--hide-tabbar",
               "-p", "LocalTabTitleFormat=" + title, "-p", "TerminalColumns=80", "-p", "TerminalRows=24",
               "-p", "Font=JetBrainsMono Nerd Font Mono,11", "-e", *command])
    window = subprocess.Popen(launch, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    time.sleep(3)
    tree = subprocess.check_output(["xwininfo", "-root", "-tree"], env=env, text=True, timeout=5)
    owned = [line.strip().split()[0] for line in tree.splitlines() if title in line]
    if len(owned) != 1:
        raise RuntimeError("Expected one isolated QA terminal; check emulator startup")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["import", "-window", owned[0], str(args.output.resolve())], env=env, check=True, timeout=5)
    print(args.output.resolve())
finally:
    if window is not None and window.poll() is None:
        window.terminate()
        window.wait(timeout=5)
    server.terminate()
    server.wait(timeout=5)
