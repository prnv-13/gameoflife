package gameoflife;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a starting position from plain text.
 *
 * Each line is a row. Live cells: '*', '#', 'O', 'X', '1'. Dead cells: '.', '0', '-', '_', ' '.
 * Lines starting with '!' or '//' are comments. Short rows are padded with dead cells.
 * The grid takes the size of the pattern unless a larger size is requested, in which
 * case the pattern is centred.
 */
public final class PatternParser {
    private PatternParser() {}

    public static boolean[][] parseFile(Path file) throws IOException {
        return parse(Files.readAllLines(file));
    }

    public static boolean[][] parse(List<String> lines) {
        List<String> rows = new ArrayList<>();
        for (String line : lines) {
            String t = line.strip();
            if (t.startsWith("!") || t.startsWith("//")) continue;
            rows.add(line.stripTrailing());
        }
        // trim blank rows at top and bottom
        while (!rows.isEmpty() && rows.get(0).isBlank()) rows.remove(0);
        while (!rows.isEmpty() && rows.get(rows.size() - 1).isBlank()) rows.remove(rows.size() - 1);
        if (rows.isEmpty()) throw new IllegalArgumentException("Pattern is empty");

        int width = rows.stream().mapToInt(String::length).max().orElse(0);
        boolean[][] cells = new boolean[rows.size()][width];
        for (int r = 0; r < rows.size(); r++) {
            String row = rows.get(r);
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                switch (ch) {
                    case '*', '#', 'O', 'o', 'X', 'x', '1' -> cells[r][c] = true;
                    case '.', '0', '-', '_', ' ' -> cells[r][c] = false;
                    default -> throw new IllegalArgumentException(
                            "Unexpected character '" + ch + "' at row " + (r + 1) + ", column " + (c + 1));
                }
            }
        }
        return cells;
    }

    /** Place the pattern in the centre of a rows x cols board (rows/cols at least the pattern size). */
    public static boolean[][] centreIn(boolean[][] pattern, int rows, int cols) {
        int pr = pattern.length, pc = pattern[0].length;
        rows = Math.max(rows, pr);
        cols = Math.max(cols, pc);
        boolean[][] out = new boolean[rows][cols];
        int offR = (rows - pr) / 2, offC = (cols - pc) / 2;
        for (int r = 0; r < pr; r++) {
            System.arraycopy(pattern[r], 0, out[r + offR], offC, pc);
        }
        return out;
    }
}
