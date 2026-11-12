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
        detectsDeath();
        detectsStillLife();
        detectsOscillator();
        detectsEmptyStart();
        gliderEndsAsBlockOnSmallBoard();
        resetForgetsHistory();
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

    /** Step until the detector reports an end (or give up). */
    static EndDetector.Result runToEnd(Grid g, EndDetector d, int maxSteps) {
        EndDetector.Result r = d.reset(g);
        for (int i = 0; r == null && i < maxSteps; i++) {
            GameOfLifeHelper.step(g);
            r = d.check(g);
        }
        return r;
    }

    static void detectsDeath() {
        EndDetector.Result r = runToEnd(g("*.", ".."), new EndDetector(), 10);
        check("lonely cell dies at generation 1",
                r != null && r.kind() == EndDetector.Kind.DIED_OUT && r.since() == 1);
    }

    static void detectsStillLife() {
        EndDetector.Result r = runToEnd(g("....", ".**.", ".**.", "...."), new EndDetector(), 10);
        check("block reported as still life from generation 0",
                r != null && r.kind() == EndDetector.Kind.STILL_LIFE && r.since() == 0 && r.period() == 1
                        && r.generation() == 1);
    }

    static void detectsOscillator() {
        EndDetector.Result r = runToEnd(g(".....", "..*..", "..*..", "..*..", "....."), new EndDetector(), 10);
        check("blinker reported as period-2 oscillator",
                r != null && r.kind() == EndDetector.Kind.OSCILLATOR && r.period() == 2 && r.generation() == 2);
        Grid pulsar = Grid.fromCells(PatternParser.parse(List.of(
                "...............", "...***...***...", "...............", ".*....*.*....*.", ".*....*.*....*.",
                ".*....*.*....*.", "...***...***...", "...............", "...***...***...", ".*....*.*....*.",
                ".*....*.*....*.", ".*....*.*....*.", "...............", "...***...***...", "...............")));
        EndDetector.Result p = runToEnd(pulsar, new EndDetector(), 20);
        check("pulsar reported as period-3 oscillator",
                p != null && p.kind() == EndDetector.Kind.OSCILLATOR && p.period() == 3);
    }

    static void detectsEmptyStart() {
        EndDetector.Result r = new EndDetector().reset(new Grid(3, 3));
        check("empty board is over immediately",
                r != null && r.kind() == EndDetector.Kind.DIED_OUT && r.describe().equals("Board is empty"));
    }

    static void gliderEndsAsBlockOnSmallBoard() {
        EndDetector.Result r = runToEnd(g(".*....", "..*...", "***...", "......", "......", "......"),
                new EndDetector(), 100);
        check("glider hits the edge and becomes a still life",
                r != null && r.kind() == EndDetector.Kind.STILL_LIFE);
    }

    static void resetForgetsHistory() {
        Grid b = g("....", ".**.", ".**.", "....");
        EndDetector d = new EndDetector();
        runToEnd(b, d, 10);
        b.toggle(0, 0);
        check("reset clears a previous end", d.reset(b) == null && d.result() == null);
    }
}
