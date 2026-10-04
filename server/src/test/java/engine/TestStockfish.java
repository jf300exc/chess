package engine;

import java.io.IOException;
import java.nio.file.*;

/** A tiny scripted UCI peer for deterministic protocol tests, never used by the app. */
public final class TestStockfish {
    private TestStockfish() { }
    public static Path executable(Path directory, String bestMove) throws IOException {
        Path script = directory.resolve("fake-stockfish.py");
        Files.writeString(script, """
                #!/usr/bin/python3
                import sys
                from pathlib import Path
                log = Path(__file__).with_suffix('.log')
                for line in sys.stdin:
                    command = line.strip()
                    with log.open('a') as out: out.write(command + '\\n')
                    if command == 'uci':
                        print('option name UCI_Elo type spin default 1320 min 1320 max 3190')
                        print('option name UCI_LimitStrength type check default false')
                        print('option name Skill Level type spin default 20 min 0 max 20')
                        print('uciok', flush=True)
                    elif command == 'isready': print('readyok', flush=True)
                    elif command.startswith('go '): print('bestmove %s', flush=True)
                """.formatted(bestMove));
        script.toFile().setExecutable(true);
        return script;
    }
}
