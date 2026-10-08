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

## Run locally

Requirements:

- JDK 17 or newer
- GNU Make

Compile the project:

```bash
make
```

Start the graphical Java simulator with train speeds 5 and 10:

```bash
TSIM_JAVA=true java -cp bin Main Lab1.map 5 10 20
```

The last argument is the simulator timer delay in milliseconds. A lower value runs the simulation faster.

## Run the headless integration test

The headless runner uses the real map, movement engine, sensor delivery, switch commands and collision detector:

```bash
javac -cp bin -sourcepath src:tests -d bin tests/HeadlessLab1Runner.java
java -cp bin HeadlessLab1Runner 5 10 8000 1
```

The arguments are train speed 1, train speed 2, test duration in milliseconds and simulator timer delay.

The documented test scenarios include `17/17`, `17/0`, `0/17`, `17/10`, `10/17`, `17/5` and `5/17`, each run for 30 seconds without a collision or derailment.

## Project structure

```text
src/Lab1.java                 Train controller and synchronization logic
src/Main.java                 Application entry points
src/TSim/                     Bundled Java simulator and API
tests/HeadlessLab1Runner.java Headless integration runner
Lab1.map                      Railway map and sensor positions
```
