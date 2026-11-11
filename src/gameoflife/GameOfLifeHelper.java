package gameoflife;

/**
 * The Game of Life rules (B3/S23). Grid only stores cells; this class decides how they evolve.
 * Cells outside the edges count as dead.
 */
public final class GameOfLifeHelper {
    private GameOfLifeHelper() {}

    /** Advance the grid by one generation. */
    public static void step(Grid grid) {
        int rows = grid.rows(), cols = grid.cols();
        boolean[][] next = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int n = liveNeighbours(grid, r, c);
                next[r][c] = grid.isAlive(r, c) ? (n == 2 || n == 3) : n == 3;
            }
        }
        grid.advanceTo(next);
    }

    static int liveNeighbours(Grid grid, int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < grid.rows() && nc >= 0 && nc < grid.cols() && grid.isAlive(nr, nc)) count++;
            }
        }
        return count;
    }
}
