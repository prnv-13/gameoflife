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
| **Edit cells** | while on, clicks toggle cells so you can draw your own start |
| **Reset** | back to the starting position |
| **Random** | new random board of the same size |
| **Clear** | empty board, switches to edit mode |

## Pattern files

Plain text, one row per line. Live: `* # O X 1`. Dead: `. 0 - _` or space.
Lines starting with `!` or `//` are comments. Examples in [`patterns/`](patterns).

## Layout

```
src/gameoflife/Grid.java           board + rules
src/gameoflife/PatternParser.java  reads starting positions
src/gameoflife/GameOfLife.java     command line, Swing window, console mode
test/gameoflife/GridTest.java      tests (./test.sh)
```
