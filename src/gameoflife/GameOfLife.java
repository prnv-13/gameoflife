package gameoflife;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/**
 * Conway's Game of Life, a zero-player game: once the starting position is set,
 * every click on the board advances one generation.
 *
 * Usage:
 *   java -cp out gameoflife.GameOfLife                      random 10x10 board
 *   java -cp out gameoflife.GameOfLife --size 20x30         random 20x30 board
 *   java -cp out gameoflife.GameOfLife patterns/glider.txt  start from a file (board = pattern size)
 *   java -cp out gameoflife.GameOfLife --pattern ".O./..O/OOO" --size 10x10   (inline glider)
 *   add --console to play in the terminal (Enter = next generation)
 */
public final class GameOfLife {
    static final int DEFAULT_ROWS = 10;
    static final int DEFAULT_COLS = 10;
    static final double DEFAULT_DENSITY = 0.3;

    public static void main(String[] args) {
        Options opt;
        try {
            opt = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(Options.USAGE);
            System.exit(2);
            return;
        }
        if (opt.help) {
            System.out.println(Options.USAGE);
            return;
        }

        Grid grid;
        try {
            grid = opt.buildGrid();
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error: could not load starting position: " + e.getMessage());
            System.exit(2);
            return;
        }

        if (opt.console) {
            runConsole(grid);
        } else {
            Random rng = opt.seed == null ? new Random() : new Random(opt.seed);
            Grid start = grid;
            SwingUtilities.invokeLater(() -> new Window(start, opt.density, rng).setVisible(true));
        }
    }

    private static void runConsole(Grid grid) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        EndDetector detector = new EndDetector();
        EndDetector.Result end = detector.reset(grid);
        try {
            while (true) {
                System.out.printf("Generation %d  (live cells: %d)%n", grid.generation(), grid.liveCount());
                System.out.print(grid);
                if (end != null) {
                    System.out.println("Game over: " + end.describe());
                    break;
                }
                System.out.print("[Enter] next generation, [q] quit: ");
                String line = in.readLine();
                if (line == null || line.trim().equalsIgnoreCase("q")) break;
                GameOfLifeHelper.step(grid);
                end = detector.check(grid);
                System.out.println();
            }
        } catch (IOException e) {
            System.err.println("Input error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- options

    static final class Options {
        static final String USAGE = """
                Usage: ./run.sh [options] [pattern-file]

                With no starting position a random %dx%d board is created.

                Options:
                  -s, --size RxC        board size, e.g. 20x30 (or 20 for a square board)
                  -f, --file PATH       read the starting position from a text file
                  -p, --pattern TEXT    inline starting position, rows separated by '/'
                                        e.g. ".*./..*/***" is a glider
                  -d, --density N       share of live cells for random boards (0..1, default %.1f)
                      --seed N          seed for the random board (repeatable runs)
                  -c, --console         play in the terminal instead of a window
                  -h, --help            show this help

                Pattern characters: live = * # O X 1, dead = . 0 - _ space.
                With a pattern and no --size, the board is the pattern's size.
                With both, the pattern is centred on the larger board.
                """.formatted(DEFAULT_ROWS, DEFAULT_COLS, DEFAULT_DENSITY);

        Integer rows, cols;
        Path file;
        String pattern;
        double density = DEFAULT_DENSITY;
        Long seed;
        boolean console;
        boolean help;

        static Options parse(String[] args) {
            Options o = new Options();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "-s", "--size" -> o.setSize(value(args, ++i, a));
                    case "-f", "--file" -> o.file = Path.of(value(args, ++i, a));
                    case "-p", "--pattern" -> o.pattern = value(args, ++i, a);
                    case "-d", "--density" -> {
                        o.density = number(value(args, ++i, a), a);
                        if (o.density < 0 || o.density > 1) throw new IllegalArgumentException("--density must be between 0 and 1");
                    }
                    case "--seed" -> o.seed = (long) number(value(args, ++i, a), a);
                    case "-c", "--console" -> o.console = true;
                    case "-h", "--help" -> o.help = true;
                    default -> {
                        if (a.startsWith("-")) throw new IllegalArgumentException("Unknown option " + a);
                        if (o.file != null) throw new IllegalArgumentException("Only one pattern file can be given");
                        o.file = Path.of(a);
                    }
                }
            }
            if (o.file != null && o.pattern != null) {
                throw new IllegalArgumentException("Use either a pattern file or --pattern, not both");
            }
            return o;
        }

        private static String value(String[] args, int i, String flag) {
            if (i >= args.length) throw new IllegalArgumentException(flag + " needs a value");
            return args[i];
        }

        private static double number(String s, String flag) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(flag + " expects a number, got '" + s + "'");
            }
        }

        private void setSize(String s) {
            String[] parts = s.toLowerCase().split("[x*,]");
            try {
                if (parts.length == 1) {
                    rows = cols = Integer.parseInt(parts[0].trim());
                } else if (parts.length == 2) {
                    rows = Integer.parseInt(parts[0].trim());
                    cols = Integer.parseInt(parts[1].trim());
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("--size expects RxC like 20x30, got '" + s + "'");
            }
            if (rows < 1 || cols < 1 || rows > 500 || cols > 500) {
                throw new IllegalArgumentException("--size must be between 1 and 500 in each direction");
            }
        }

        Grid buildGridUnchecked() {
            try {
                return buildGrid();
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }

        Grid buildGrid() throws IOException {
            boolean[][] start = null;
            if (file != null) start = PatternParser.parseFile(file);
            if (pattern != null) start = PatternParser.parse(Arrays.asList(pattern.split("[/|\\n]", -1)));

            if (start == null) {
                Random rng = seed == null ? new Random() : new Random(seed);
                return Grid.random(rows != null ? rows : DEFAULT_ROWS, cols != null ? cols : DEFAULT_COLS, density, rng);
            }
            if (rows != null) {
                if (rows < start.length || cols < start[0].length) {
                    throw new IllegalArgumentException("pattern is " + start.length + "x" + start[0].length
                            + ", larger than --size " + rows + "x" + cols);
                }
                start = PatternParser.centreIn(start, rows, cols);
            }
            return Grid.fromCells(start);
        }
    }

    // ---------------------------------------------------------------- window

    static final class Window extends JFrame {
        static final int DEFAULT_SPEED = 5;   // generations per second
        static final int MAX_SPEED = 30;

        private final Grid grid;
        private boolean[][] initial;
        private final double density;
        private final Random rng;
        private final Board board;
        private final EndDetector detector = new EndDetector();
        private final JLabel status = new JLabel();
        private final JLabel endMessage = new JLabel(" ");
        private final JToggleButton edit = new JToggleButton("Edit cells");
        private final JToggleButton play = new JToggleButton("Play");
        private final JSlider speed = new JSlider(1, MAX_SPEED, DEFAULT_SPEED);
        private final JLabel speedValue = new JLabel();
        private final Timer timer;

        Window(Grid grid, double density, Random rng) {
            super("Game of Life");
            this.grid = grid;
            this.density = density;
            this.rng = rng;
            this.initial = snapshot(grid);
            this.board = new Board();
            this.timer = new Timer(delayMs(), e -> advance());

            JButton next = new JButton("Next generation");
            next.addActionListener(e -> advance());
            play.setToolTipText("Run generations automatically");
            play.addActionListener(e -> {
                if (play.isSelected()) start(); else pause();
            });
            JButton reset = new JButton("Reset");
            reset.setToolTipText("Back to the starting position");
            reset.addActionListener(e -> { restore(); newStart(); });
            JButton random = new JButton("Random");
            random.addActionListener(e -> { grid.randomize(density, rng); initial = snapshot(grid); newStart(); });
            JButton clear = new JButton("Clear");
            clear.addActionListener(e -> {
                pause();
                grid.clear();
                initial = snapshot(grid);
                edit.setSelected(true);
                newStart();
            });
            edit.setToolTipText("When on, clicking a cell toggles it instead of advancing the game");
            edit.addActionListener(e -> {
                if (edit.isSelected()) pause();
                refresh();
            });

            speed.addChangeListener(e -> {
                timer.setDelay(delayMs());
                timer.setInitialDelay(delayMs());
                updateSpeedLabel();
            });
            updateSpeedLabel();

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttons.add(next);
            buttons.add(play);
            buttons.add(reset);
            buttons.add(random);
            buttons.add(clear);
            buttons.add(edit);

            JPanel speedRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            speedRow.add(new JLabel("Speed"));
            speedRow.add(speed);
            speedRow.add(speedValue);

            JPanel controls = new JPanel(new GridLayout(2, 1));
            controls.add(buttons);
            controls.add(speedRow);

            endMessage.setForeground(new Color(0xA33A1E));
            endMessage.setFont(endMessage.getFont().deriveFont(Font.BOLD));
            JPanel south = new JPanel(new GridLayout(2, 1, 0, 2));
            south.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
            south.add(status);
            south.add(endMessage);

            add(controls, BorderLayout.NORTH);
            add(board, BorderLayout.CENTER);
            add(south, BorderLayout.SOUTH);

            setDefaultCloseOperation(EXIT_ON_CLOSE);
            pack();
            setLocationRelativeTo(null);
            newStart();
        }

        private int delayMs() {
            return 1000 / speed.getValue();
        }

        private void updateSpeedLabel() {
            speedValue.setText(speed.getValue() + " gen/s");
        }

        private void start() {
            if (edit.isSelected()) edit.setSelected(false);
            if (detector.result() != null) {
                // Already over: nothing new will happen, so don't start the timer.
                play.setSelected(false);
                refresh();
                return;
            }
            play.setSelected(true);
            play.setText("Pause");
            timer.start();
            refresh();
        }

        private void pause() {
            timer.stop();
            play.setSelected(false);
            play.setText("Play");
        }

        /** The board was replaced (reset, random, clear, edit): start end detection afresh. */
        private void newStart() {
            detector.reset(grid);
            refresh();
        }

        private void advance() {
            GameOfLifeHelper.step(grid);
            if (detector.check(grid) != null) pause();
            refresh();
        }

        private void refresh() {
            String hint = edit.isSelected()
                    ? "Editing: click cells to toggle"
                    : timer.isRunning() ? "Playing" : "Click board = next generation";
            status.setText(String.format("Gen %d  |  Live %d  |  %dx%d  |  %s",
                    grid.generation(), grid.liveCount(), grid.rows(), grid.cols(), hint));
            EndDetector.Result end = detector.result();
            endMessage.setText(end == null ? " " : "Game over: " + end.describe());
            board.repaint();
        }

        private static boolean[][] snapshot(Grid g) {
            boolean[][] s = new boolean[g.rows()][g.cols()];
            for (int r = 0; r < g.rows(); r++)
                for (int c = 0; c < g.cols(); c++) s[r][c] = g.isAlive(r, c);
            return s;
        }

        private void restore() {
            grid.clear();
            for (int r = 0; r < grid.rows(); r++)
                for (int c = 0; c < grid.cols(); c++) if (initial[r][c]) grid.toggle(r, c);
        }

        final class Board extends JPanel {
            private static final Color DEAD = new Color(0xF4F4F0);
            private static final Color ALIVE = new Color(0x1F6F50);
            private static final Color LINES = new Color(0xD0D0C8);

            Board() {
                int cell = Math.max(6, Math.min(48, 600 / Math.max(grid.rows(), grid.cols())));
                setPreferredSize(new Dimension(grid.cols() * cell + 1, grid.rows() * cell + 1));
                setBackground(Color.WHITE);
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (edit.isSelected()) {
                            int[] rc = cellAt(e.getX(), e.getY());
                            if (rc != null) {
                                grid.toggle(rc[0], rc[1]);
                                initial = snapshot(grid);
                                newStart();
                            }
                        } else {
                            advance();
                        }
                    }
                });
            }

            private int cellSize() {
                return Math.max(1, Math.min((getWidth() - 1) / grid.cols(), (getHeight() - 1) / grid.rows()));
            }

            private int[] cellAt(int x, int y) {
                int size = cellSize();
                int ox = (getWidth() - grid.cols() * size) / 2, oy = (getHeight() - grid.rows() * size) / 2;
                int c = (x - ox) / size, r = (y - oy) / size;
                if (x < ox || y < oy || r >= grid.rows() || c >= grid.cols()) return null;
                return new int[]{r, c};
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int size = cellSize();
                int ox = (getWidth() - grid.cols() * size) / 2, oy = (getHeight() - grid.rows() * size) / 2;
                for (int r = 0; r < grid.rows(); r++) {
                    for (int c = 0; c < grid.cols(); c++) {
                        g.setColor(grid.isAlive(r, c) ? ALIVE : DEAD);
                        g.fillRect(ox + c * size, oy + r * size, size, size);
                    }
                }
                if (size >= 4) {
                    g.setColor(LINES);
                    for (int r = 0; r <= grid.rows(); r++) g.drawLine(ox, oy + r * size, ox + grid.cols() * size, oy + r * size);
                    for (int c = 0; c <= grid.cols(); c++) g.drawLine(ox + c * size, oy, ox + c * size, oy + grid.rows() * size);
                }
            }
        }
    }
}
