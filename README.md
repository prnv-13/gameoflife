# Game of Life

Conway's Game of Life in Java (Swing). It's a zero-player game: you set the starting
position, then each click on the board (or **Next generation**) advances one generation.

Rules: a live cell with 2 or 3 live neighbours survives; a dead cell with exactly 3 becomes
alive; every other cell dies or stays dead. Cells beyond the edge count as dead.

## Run

Needs a JDK (17+). No build tool required.

```bash
./run.sh                          # random 10x10 board (default)
./run.sh --size 20x30             # random 20x30 board
./run.sh patterns/glider.txt      # start from a file; board takes the pattern's size
./run.sh -p ".O./..O/OOO"         # inline pattern, rows separated by '/'
./run.sh -p "OOO" --size 9x9      # pattern centred on a larger board
./run.sh --console                # play in the terminal: Enter = next generation
./run.sh --help
```

Other options: `--density 0.4` (share of live cells on random boards), `--seed 42` (repeatable random board).

## In the window

| Control | What it does |
| --- | --- |
| Click the board / **Next generation** | advance one generation |
| **Play** / **Pause** | run generations automatically |
| **Speed** slider | 1 to 30 generations per second (default 5) |
| **Edit cells** | while on, clicks toggle cells so you can draw your own start |
| **Reset** | back to the starting position |
| **Random** | new random board of the same size |
| **Clear** | empty board, switches to edit mode |

## End detection

The game watches every board it has produced and says when it is over, stopping
auto-play:

- **All cells died**: `Game over: All cells died at generation 12`
- **Still life**, nothing changes any more: `Stable since generation 31`
- **Oscillator**, an earlier board came back: `Repeats every 2 generations (since generation 5)`

Any earlier board counts, so cycles of any length are caught (only the first 10,000
boards are remembered, to bound memory). Reset, Random, Clear or editing a cell starts
detection over. Console mode prints the same message and exits.

## Pattern files

Plain text, one row per line. Live: `* # O X 1`. Dead: `. 0 - _` or space.
Lines starting with `!` or `//` are comments. Examples in [`patterns/`](patterns).

## Layout

```
src/gameoflife/Grid.java              board state + generation count
src/gameoflife/GameOfLifeHelper.java  the rules (next generation, neighbour count)
src/gameoflife/EndDetector.java       detects died out / still life / repeating boards
src/gameoflife/PatternParser.java     reads starting positions
src/gameoflife/GameOfLife.java        command line, Swing window, console mode
test/gameoflife/GridTest.java         tests (./test.sh)
```
