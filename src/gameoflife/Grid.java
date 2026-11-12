package gameoflife;

import java.util.BitSet;
import java.util.Random;

/**
 * The Game of Life board: cell state and generation count. Rules live in {@link GameOfLifeHelper}.
 */
public final class Grid {
    private final int rows;
    private final int cols;
    private boolean[][] cells;
    private int generation;

    public Grid(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Grid must be at least 1x1, got " + rows + "x" + cols);
        }
        this.rows = rows;
        this.cols = cols;
        this.cells = new boolean[rows][cols];
    }

    public static Grid random(int rows, int cols, double density, Random rng) {
        Grid g = new Grid(rows, cols);
        g.randomize(density, rng);
        return g;
    }

    public static Grid fromCells(boolean[][] initial) {
        Grid g = new Grid(initial.length, initial[0].length);
        for (int r = 0; r < g.rows; r++) {
            System.arraycopy(initial[r], 0, g.cells[r], 0, g.cols);
        }
        return g;
    }

    public void randomize(double density, Random rng) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = rng.nextDouble() < density;
            }
        }
        generation = 0;
    }

    public void clear() {
        cells = new boolean[rows][cols];
        generation = 0;
    }

    /** Replace the cells with the next generation (computed by {@link GameOfLifeHelper}). */
    void advanceTo(boolean[][] next) {
        cells = next;
        generation++;
    }

    public void toggle(int r, int c) {
        cells[r][c] = !cells[r][c];
    }

    public boolean isAlive(int r, int c) {
        return cells[r][c];
    }

    /** The board's live cells as a compact, hashable value (row-major bit per cell). */
    public BitSet stateKey() {
        BitSet bits = new BitSet(rows * cols);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (cells[r][c]) bits.set(r * cols + c);
        return bits;
    }

    public int liveCount() {
        int n = 0;
        for (boolean[] row : cells) for (boolean b : row) if (b) n++;
        return n;
    }

    public int rows() { return rows; }
    public int cols() { return cols; }
    public int generation() { return generation; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (boolean[] row : cells) {
            for (boolean b : row) sb.append(b ? '*' : '.');
            sb.append('\n');
        }
        return sb.toString();
    }
}
