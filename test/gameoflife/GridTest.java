package gameoflife;

import java.util.List;

/** Dependency-free checks: run with ./test.sh */
public final class GridTest {
    private static int failures;

    public static void main(String[] args) {
        blinkerOscillates();
        blockIsStable();
        gliderMoves();
        edgesAreDead();
        defaultRandomIs10x10();
        patternSetsGridSize();
        patternCentredInLargerSize();
        if (failures > 0) { System.out.println(failures + " FAILED"); System.exit(1); }
        System.out.println("All tests passed");
    }

    static Grid g(String... rows) { return Grid.fromCells(PatternParser.parse(List.of(rows))); }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failures++;
    }

    static void blinkerOscillates() {
        Grid b = g(".....", "..*..", "..*..", "..*..", ".....");
        String start = b.toString();
        GameOfLifeHelper.step(b);
        check("blinker turns horizontal", b.toString().equals(".....\n.....\n.***.\n.....\n.....\n"));
        GameOfLifeHelper.step(b);
        check("blinker returns after 2 generations", b.toString().equals(start));
    }

    static void blockIsStable() {
        Grid b = g("....", ".**.", ".**.", "....");
        String s = b.toString();
        GameOfLifeHelper.step(b);
        check("block is still life", b.toString().equals(s) && b.generation() == 1);
    }

    static void gliderMoves() {
        Grid gl = g(".*....", "..*...", "***...", "......", "......", "......");
        for (int i = 0; i < 4; i++) GameOfLifeHelper.step(gl);
        check("glider shifts by (1,1) after 4 generations",
                gl.toString().equals("......\n..*...\n...*..\n.***..\n......\n......\n"));
    }

    static void edgesAreDead() {
        Grid c = g("**", "**");
        check("corner cell has 3 neighbours", GameOfLifeHelper.liveNeighbours(c, 0, 0) == 3);
    }

    static void defaultRandomIs10x10() {
        Grid r = GameOfLife.Options.parse(new String[0]).buildGridUnchecked();
        check("default board is 10x10", r.rows() == 10 && r.cols() == 10);
    }

    static void patternSetsGridSize() {
        Grid p = GameOfLife.Options.parse(new String[]{"-p", ".*./..*/***"}).buildGridUnchecked();
        check("inline pattern sets 3x3 board", p.rows() == 3 && p.cols() == 3 && p.liveCount() == 5);
    }

    static void patternCentredInLargerSize() {
        Grid p = GameOfLife.Options.parse(new String[]{"-p", "*", "-s", "5x7"}).buildGridUnchecked();
        check("pattern centred on 5x7", p.rows() == 5 && p.cols() == 7 && p.isAlive(2, 3));
    }
}
