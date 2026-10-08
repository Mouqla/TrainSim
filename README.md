# TrainSim — concurrent train controller

Trainspotting is a Java controller for two trains sharing a railway network. It uses sensor events, switches and binary semaphores to route both trains without collisions or deadlocks. Each train runs in its own controller thread and keeps its state locally.

## Highlights

- One controller thread and one `TrainState` object per train
- Eight binary semaphores protecting station tracks, middle tracks and junctions
- Dynamic selection between parallel tracks, allowing a faster train to overtake
- Blocking synchronization with `Semaphore.acquire()` instead of busy waiting
- Safe operation for train speeds from 0 through 17
- Bundled Java simulator and a headless integration test

## Design

Only the eight semaphores are shared between the two controller threads. Speed, direction, selected tracks and ownership flags remain local to each train.

| Semaphore | Protected section |
| --- | --- |
| `upperUpper`, `upperUnder` | Parallel tracks at the upper station |
| `middleCrit` | Upper crossing, junction and switches |
| `middleUpper`, `middleUnder` | Parallel tracks in the middle |
| `underCrit` | Lower junction and switches |
| `underTrack`, `underUnder` | Parallel tracks at the lower station |

At a fork, the controller first attempts to reserve the default track. If it is occupied, the train takes the alternative track. A train is stopped before it blocks on an occupied critical section, and the relevant switch is configured before it starts again.

Two sensors were added at `(7,9)` and `(7,10)`. They let a train reserve or release the lower junction at the right time without locking the entire middle section.

At a station, a train stops for `1000 + 20 × |speed|` milliseconds and then reverses. The implementation accepts speeds in the inclusive range `0–17`.

## Quick start

You only need [Java JDK 17 or newer](https://adoptium.net/) to run the project. Download or clone the repository, extract it if needed, and open the project folder.

### Windows

Double-click `run.bat`.

### Linux, macOS or WSL

Open a terminal in the project folder and run:

```bash
./run.sh
```

The start script compiles the project and opens the graphical train simulator. The default train speeds are 5 and 10. The simulator is included in the repository, so no separate TSim installation is needed.

If the script reports that Java is missing, install JDK 17 or newer, restart the terminal and try again.

## Test without opening the simulator

Developers can compile and run the headless integration test with one command:

```bash
make test
```

A successful test reports `fatal=NONE`, two controller threads and eight binary semaphores.

## Project structure

```text
src/Lab1.java                 Train controller and synchronization logic
src/Main.java                 Application entry points
src/TSim/                     Bundled Java simulator and API
tests/HeadlessLab1Runner.java Headless integration runner
Lab1.map                      Railway map and sensor positions
run.bat                       Start the project on Windows
run.sh                        Start the project on Linux, macOS or WSL
```
