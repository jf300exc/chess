package service;

/** Bounded, shared locks coordinate REST seat changes and WebSocket/engine moves. */
public final class GameLocks {
    private static final Object[] LOCKS = new Object[256];
    static { java.util.Arrays.setAll(LOCKS, ignored -> new Object()); }
    private GameLocks() { }
    public static Object forGame(int id) { return LOCKS[Math.floorMod(id, LOCKS.length)]; }
}
