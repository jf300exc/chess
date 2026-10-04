package engine;

import chess.ChessGame;
import chess.ChessMove;
import model.StockfishOptions;
import model.StockfishPlayer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** One bounded UCI process per search; no shell or executable supplied by a client. */
public class StockfishEngine implements AutoCloseable {
    private static final String EOF = "<engine exited>";
    private final Process process;
    private final BufferedWriter input;
    private final BlockingQueue<String> output = new ArrayBlockingQueue<>(1024);
    private final Thread reader;
    private int minElo, maxElo;

    public StockfishEngine() throws IOException {
        this(executable());
    }

    public StockfishEngine(String executable) throws IOException {
        process = new ProcessBuilder(executable).redirectErrorStream(true).start();
        input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        reader = Thread.ofVirtual().start(() -> {
            try (var lines = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = lines.readLine()) != null) { output.put(line); }
            } catch (IOException | InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally { output.offer(EOF); }
        });
        try {
            send("uci");
            boolean skill = false, strength = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            String line;
            while (!(line = next(deadline)).equals("uciok")) {
                Matcher range = Pattern.compile("option name UCI_Elo type spin .* min (\\d+) max (\\d+)").matcher(line);
                if (range.matches()) { minElo = Integer.parseInt(range.group(1)); maxElo = Integer.parseInt(range.group(2)); }
                skill |= line.startsWith("option name Skill Level type spin") && line.endsWith("min 0 max 20");
                strength |= line.startsWith("option name UCI_LimitStrength type check");
            }
            if (minElo <= 0 || maxElo < minElo || !skill || !strength) {
                throw new IOException("Engine does not support Stockfish difficulty options");
            }
            send("setoption name Threads value 1");
            send("setoption name Hash value 16");
            ready();
        } catch (IOException | RuntimeException e) { close(); throw e; }
    }

    private static String executable() {
        String configured = System.getenv("STOCKFISH_PATH");
        return configured != null && !configured.isBlank() ? configured
                : Files.isExecutable(Path.of("/usr/games/stockfish")) ? "/usr/games/stockfish" : "stockfish";
    }

    public StockfishPlayer player(StockfishOptions options, int humanElo) {
        options.validate();
        int elo = Math.clamp(options.elo() == null ? humanElo : options.elo(), minElo, maxElo);
        return new StockfishPlayer(options.color(), options.mode(), elo, options.skillLevel());
    }

    public ChessMove bestMove(ChessGame game, StockfishPlayer player) throws IOException {
        send("ucinewgame");
        if (player.mode() == StockfishOptions.Mode.ELO) {
            send("setoption name UCI_LimitStrength value true");
            send("setoption name UCI_Elo value " + Math.clamp(player.elo(), minElo, maxElo));
        } else {
            send("setoption name UCI_LimitStrength value false");
            send("setoption name Skill Level value " + player.skillLevel());
        }
        ready();
        send("position fen " + ChessUci.fen(game));
        send("go movetime 250");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (true) {
            String line = next(deadline);
            if (line.startsWith("bestmove ")) {
                try { return ChessUci.move(line.split("\\s+")[1]); }
                catch (IllegalArgumentException e) { throw new IOException(e.getMessage(), e); }
            }
        }
    }

    private void ready() throws IOException {
        send("isready");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!next(deadline).equals("readyok")) { /* Ignore info lines. */ }
    }

    private String next(long deadline) throws IOException {
        try {
            String line = output.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (line == null) { throw new IOException("Stockfish timed out"); }
            if (line.equals(EOF)) { throw new IOException("Stockfish exited unexpectedly"); }
            return line;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Stockfish search interrupted", e);
        }
    }

    private void send(String command) throws IOException { input.write(command); input.newLine(); input.flush(); }

    @Override
    public void close() {
        process.destroyForcibly();
        reader.interrupt();
        try { input.close(); } catch (IOException ignored) { }
    }
}
