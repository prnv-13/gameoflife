package gameoflife;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Tells when a game has stopped being interesting: every cell has died, the board no
 * longer changes (still life), or it has returned to an earlier board (oscillator).
 *
 * Every board seen since the last {@link #reset} is remembered with its generation, so a
 * repeat of any earlier board is caught, whatever its period. To keep memory bounded on
 * big boards only the first {@value #MAX_HISTORY} boards are remembered; a cycle that only
 * starts after that is not detected.
 */
public final class EndDetector {
    static final int MAX_HISTORY = 10_000;

    public enum Kind { DIED_OUT, STILL_LIFE, OSCILLATOR }

    /**
     * @param kind       how the game ended
     * @param generation generation at which the end was detected
     * @param since      first generation of the final state or cycle
     * @param period     generations per cycle (1 for still life, 0 when died out)
     */
    public record Result(Kind kind, int generation, int since, int period) {
        public String describe() {
            return switch (kind) {
                case DIED_OUT -> since == 0 ? "Board is empty" : "All cells died at generation " + since;
                case STILL_LIFE -> "Stable since generation " + since + ", nothing changes any more";
                case OSCILLATOR -> "Repeats every " + period + " generations (since generation " + since + ")";
            };
        }
    }

    private final Map<BitSet, Integer> seen = new HashMap<>();
    private Result result;

    /** Forget the history and start watching from the grid's current board. */
    public Result reset(Grid grid) {
        seen.clear();
        result = null;
        return check(grid);
    }

    /**
     * Call after every generation. Returns the end result once one is found (and keeps
     * returning it), or null while the game is still evolving.
     */
    public Result check(Grid grid) {
        if (result != null) return result;
        int gen = grid.generation();
        if (grid.liveCount() == 0) {
            return result = new Result(Kind.DIED_OUT, gen, gen, 0);
        }
        BitSet key = grid.stateKey();
        Integer earlier = seen.get(key);
        if (earlier != null) {
            int period = gen - earlier;
            Kind kind = period == 1 ? Kind.STILL_LIFE : Kind.OSCILLATOR;
            return result = new Result(kind, gen, earlier, period);
        }
        if (seen.size() < MAX_HISTORY) seen.put(key, gen);
        return null;
    }

    public Result result() {
        return result;
    }
}
