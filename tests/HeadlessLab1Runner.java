import TSim.TSimInterface;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import tsim.api.EmbeddedTSim;
import tsim.io.MapFileReader;
import tsim.logic.MovementEngine;
import tsim.model.SimulationEvent;
import tsim.model.SimulationState;
import tsim.model.Train;

/**
 * Headless integration runner for Lab1. This is deliberately outside src/ so
 * it is not part of the submitted implementation.
 *
 * Usage:
 *   java -cp bin HeadlessLab1Runner <speed1> <speed2> [duration-ms] [timer-ms]
 */
public final class HeadlessLab1Runner {
  private static final int DEFAULT_DURATION_MS = 8_000;
  private static final int DEFAULT_TIMER_MS = 1;

  private HeadlessLab1Runner() {
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");

    int speed1 = Integer.parseInt(args[0]);
    int speed2 = Integer.parseInt(args[1]);
    int durationMs = args.length >= 3 ? Integer.parseInt(args[2]) : DEFAULT_DURATION_MS;
    int timerMs = args.length >= 4 ? Integer.parseInt(args[3]) : DEFAULT_TIMER_MS;

    SimulationState simulation = MapFileReader.read(
        Path.of("Lab1.map"), MovementEngine.DEFAULT_RAIL_WIDTH);
    Recorder recorder = new Recorder(simulation);
    TSimInterface interfaceInstance = TSimInterface.getInstance();

    Method acceptSensor = privateMethod(
        TSimInterface.class, "acceptSensorEvent",
        int.class, int.class, int.class, boolean.class);
    Method acceptTrain = privateMethod(
        TSimInterface.class, "acceptTrainEvent",
        int.class, SimulationEvent.class);

    EmbeddedTSim embedded = new EmbeddedTSim(simulation, recorder::sampleSpeeds,
        new EmbeddedTSim.Listener() {
          @Override
          public void sensorEvent(int trainNo, int x, int y, boolean active) {
            recorder.sensorEvent(trainNo, x, y, active);
            invoke(acceptSensor, interfaceInstance, trainNo, x, y, active);
          }

          @Override
          public void trainEvent(int trainNo, SimulationEvent event) {
            recorder.trainEvent(trainNo, event);
            invoke(acceptTrain, interfaceInstance, trainNo, event);
          }
        });

    Field embeddedField = TSimInterface.class.getDeclaredField("embedded");
    embeddedField.setAccessible(true);
    embeddedField.set(interfaceInstance, embedded);

    embedded.startTimer(timerMs);
    Lab1 controller = new Lab1(speed1, speed2);

    Thread.sleep(durationMs);
    SwingUtilities.invokeAndWait(recorder::sampleSpeeds);

    int controllerThreads = countLab1ControllerThreads();
    SemaphoreCheck semaphoreCheck = inspectSemaphores(controller);
    System.out.println(recorder.result(
        speed1, speed2, durationMs, timerMs, controllerThreads, semaphoreCheck));

    boolean movingTrainFailed = (speed1 != 0 && recorder.activeSensors[1] == 0)
        || (speed2 != 0 && recorder.activeSensors[2] == 0);
    boolean failed = recorder.fatal.get() != null
        || movingTrainFailed
        || controllerThreads != 2
        || semaphoreCheck.count >= 10
        || !semaphoreCheck.binary;
    System.exit(failed ? 1 : 0);
  }

  private static Method privateMethod(Class<?> owner, String name, Class<?>... parameters)
      throws ReflectiveOperationException {
    Method method = owner.getDeclaredMethod(name, parameters);
    method.setAccessible(true);
    return method;
  }

  private static void invoke(Method method, Object receiver, Object... args) {
    try {
      method.invoke(receiver, args);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static int countLab1ControllerThreads() {
    int count = 0;
    for (var entry : Thread.getAllStackTraces().entrySet()) {
      if (!entry.getKey().isAlive()) {
        continue;
      }
      boolean lab1Frame = Arrays.stream(entry.getValue())
          .anyMatch(frame -> frame.getClassName().equals("Lab1"));
      if (lab1Frame) {
        count++;
      }
    }
    return count;
  }

  private static SemaphoreCheck inspectSemaphores(Lab1 controller)
      throws IllegalAccessException {
    List<String> permits = new ArrayList<>();
    boolean binary = true;
    int count = 0;
    for (Field field : Lab1.class.getDeclaredFields()) {
      if (field.getType() != Semaphore.class) {
        continue;
      }
      count++;
      field.setAccessible(true);
      int available = ((Semaphore) field.get(controller)).availablePermits();
      permits.add(field.getName() + "=" + available);
      binary &= available >= 0 && available <= 1;
    }
    return new SemaphoreCheck(count, binary, permits);
  }

  private record SemaphoreCheck(int count, boolean binary, List<String> permits) {
  }

  private static final class Recorder {
    private final SimulationState simulation;
    private final long startedNanos = System.nanoTime();
    private final int[] activeSensors = new int[3];
    private final int[] stationArrivals = new int[3];
    private final int[] middleRow9 = new int[3];
    private final int[] middleRow10 = new int[3];
    private final List<String>[] activeTrace;
    private final Journey[] currentJourney = new Journey[3];
    private final List<Journey> completedJourneys = new ArrayList<>();
    private final float[] lastWantedSpeed = new float[3];
    private final boolean[] stationStopPending = new boolean[3];
    private final long[] stationStopCommandNanos = new long[3];
    private final List<Long>[] pauseMillis;
    private final AtomicReference<String> fatal = new AtomicReference<>();
    private long sensorEventSequence;

    @SuppressWarnings("unchecked")
    Recorder(SimulationState simulation) {
      this.simulation = simulation;
      pauseMillis = new List[] {new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
      activeTrace = new List[] {new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    }

    void sensorEvent(int trainNo, int x, int y, boolean active) {
      long sequence = ++sensorEventSequence;
      trackMiddleJourney(trainNo, x, y, active, sequence);
      if (!active) {
        return;
      }
      activeSensors[trainNo]++;
      if (activeTrace[trainNo].size() < 40) {
        activeTrace[trainNo].add(x + ":" + y);
      }
      if (x == 13 && y == 9) {
        middleRow9[trainNo]++;
      } else if (x == 12 && y == 10) {
        middleRow10[trainNo]++;
      }
      if (isStationSensor(x, y)) {
        stationArrivals[trainNo]++;
        stationStopPending[trainNo] = true;
      }
    }

    private void trackMiddleJourney(
        int trainNo, int x, int y, boolean active, long sequence) {
      Journey journey = currentJourney[trainNo];
      if (active && journey == null && x == 18 && y == 9) {
        currentJourney[trainNo] = new Journey(trainNo, "D", sequence);
      } else if (active && journey == null && x == 2 && y == 9) {
        currentJourney[trainNo] = new Journey(trainNo, "U", sequence);
      } else if (active && journey != null && x == 13 && y == 9) {
        journey.track = "M";
      } else if (active && journey != null && x == 12 && y == 10) {
        journey.track = "S";
      } else if (!active && journey != null
          && ((journey.direction.equals("D") && x == 2 && y == 9)
              || (journey.direction.equals("U") && x == 18 && y == 9))) {
        journey.exitSequence = sequence;
        completedJourneys.add(journey);
        currentJourney[trainNo] = null;
      }
    }

    void trainEvent(int trainNo, SimulationEvent event) {
      List<String> positions = new ArrayList<>();
      for (Train train : simulation.trains()) {
        positions.add("t" + train.no()
            + "=" + train.parts().stream()
                .map(part -> part.pos().x() + ":" + part.pos().y())
                .toList()
            + "/speed=" + train.speed()
            + "/wanted=" + train.wantedSpeed());
      }
      fatal.compareAndSet(null,
          "train=" + trainNo + ",event=" + event + ",positions=" + positions);
    }

    void sampleSpeeds() {
      for (Train train : simulation.trains()) {
        int id = train.no();
        float wanted = train.wantedSpeed();
        float previous = lastWantedSpeed[id];
        if (wanted == 0.0f && previous != 0.0f) {
          if (stationStopPending[id]) {
            stationStopCommandNanos[id] = System.nanoTime();
            stationStopPending[id] = false;
          }
        } else if (wanted != 0.0f
            && previous == 0.0f
            && stationStopCommandNanos[id] != 0L) {
          pauseMillis[id].add(
              (System.nanoTime() - stationStopCommandNanos[id]) / 1_000_000L);
          stationStopCommandNanos[id] = 0L;
        }
        lastWantedSpeed[id] = wanted;
      }
    }

    String result(
        int speed1,
        int speed2,
        int durationMs,
        int timerMs,
        int controllerThreads,
        SemaphoreCheck semaphoreCheck) {
      long actualDurationMs = (System.nanoTime() - startedNanos) / 1_000_000L;
      return String.format(Locale.ROOT,
          "RESULT speed1=%d speed2=%d requestedMs=%d actualMs=%d timerMs=%d "
              + "fatal=%s controllers=%d semaphores=%d binary=%s permits=%s "
              + "activeSensors=[%d,%d] stations=[%d,%d] "
              + "middleRow9=[%d,%d] middleRow10=[%d,%d] "
              + "stationPausesMs=%s/%s overtakes=%d journeys=%s "
              + "trace=%s/%s finalWanted=[%.1f,%.1f]",
          speed1, speed2, durationMs, actualDurationMs, timerMs,
          fatal.get() == null ? "NONE" : fatal.get(), controllerThreads,
          semaphoreCheck.count, semaphoreCheck.binary, semaphoreCheck.permits,
          activeSensors[1], activeSensors[2], stationArrivals[1], stationArrivals[2],
          middleRow9[1], middleRow9[2], middleRow10[1], middleRow10[2],
          pauseMillis[1], pauseMillis[2], countOvertakes(), completedJourneys,
          activeTrace[1], activeTrace[2],
          lastWantedSpeed[1], lastWantedSpeed[2]);
    }

    private int countOvertakes() {
      int count = 0;
      for (int i = 0; i < completedJourneys.size(); i++) {
        Journey first = completedJourneys.get(i);
        for (int j = i + 1; j < completedJourneys.size(); j++) {
          Journey second = completedJourneys.get(j);
          if (!first.direction.equals(second.direction)
              || first.trainNo == second.trainNo) {
            continue;
          }
          boolean reversedOrder = (first.entrySequence < second.entrySequence
              && first.exitSequence > second.exitSequence)
              || (second.entrySequence < first.entrySequence
                  && second.exitSequence > first.exitSequence);
          if (reversedOrder) {
            count++;
          }
        }
      }
      return count;
    }

    private static boolean isStationSensor(int x, int y) {
      return (x == 16 && y == 3)
          || (x == 15 && y == 5)
          || (x == 16 && y == 11)
          || (x == 15 && y == 13);
    }
  }

  private static final class Journey {
    final int trainNo;
    final String direction;
    final long entrySequence;
    long exitSequence;
    String track = "?";

    Journey(int trainNo, String direction, long entrySequence) {
      this.trainNo = trainNo;
      this.direction = direction;
      this.entrySequence = entrySequence;
    }

    @Override
    public String toString() {
      return trainNo + direction + track + "@" + entrySequence + "-" + exitSequence;
    }
  }
}
