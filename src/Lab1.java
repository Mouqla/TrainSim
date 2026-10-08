import TSim.*;
import java.lang.Thread;
import java.util.concurrent.Semaphore;

public class Lab1 {
  public static final int MAX_SPEED = 17;

  // Parallel tracks near the upper station.
  private final Semaphore upperUpper = new Semaphore(1);
  private final Semaphore upperUnder = new Semaphore(1);

  // Shared tracks in the middle section.
  private final Semaphore middleCrit = new Semaphore(1);
  private final Semaphore middleUpper = new Semaphore(1);
  private final Semaphore middleUnder = new Semaphore(1);

  // Shared tracks near the lower station.
  private final Semaphore underCrit = new Semaphore(1);
  private final Semaphore underTrack = new Semaphore(1);
  private final Semaphore underUnder = new Semaphore(1);

  public Lab1(int speed1, int speed2) {
    validateSpeed(speed1);
    validateSpeed(speed2);
    CreateThread(1, speed1);
    CreateThread(2, speed2);
  }

  public void RunTrain(int trainId, int trainSpeed) {
    TSimInterface tsi = TSimInterface.getInstance();

    // Train 2 starts by travelling towards the upper station.
    TrainState state = new TrainState(trainSpeed, trainId == 2);

    try {
      // Reserve the starting track before moving.
      reserveStart(state, tsi);
      tsi.setSpeed(trainId, state.speed);

      while (true) {
        // React to the next sensor triggered by this train.
        SensorEvent event = tsi.getSensor(trainId);
        atStation(event, trainId, state);
        handleUpperTrack(event, trainId, state);
        handleMiddleTrack(event, trainId, state);
        handleLowerTrack(event, trainId, state);
      }
    }
    catch (CommandException e) {
      System.err.println(e.getMessage());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void CreateThread(int trainId, int trainSpeed) {
    Thread trainThread = new Thread(() -> RunTrain(trainId, trainSpeed));
    trainThread.setName(String.valueOf(trainId));
    trainThread.start();
  }
  
  // State owned by one train thread.
  private static final class TrainState {

    int speed;
    boolean goingToUpper;
    Semaphore upperTrack;
    Semaphore middleTrack;
    Semaphore lowerTrack;
    boolean hasMiddleCrit;
    boolean hasUnderCrit;

    TrainState(int initialSpeed, boolean startsGoingToUpper) {
      speed = initialSpeed;
      goingToUpper = startsGoingToUpper;
    }
  }

  // The train reserves its starting station track, even if its speed is 0.
  private void reserveStart(TrainState state, TSimInterface tsi)
      throws CommandException, InterruptedException {
    if (state.goingToUpper) {
      underTrack.acquire();
      state.lowerTrack = underTrack;
      if (state.speed != 0) { // A moving train also reserves the next critical section and sets the switch.
        underCrit.acquire();
        state.hasUnderCrit = true;
        setLowerSwitch(state.lowerTrack, tsi);
      }
    }
    else {
      upperUpper.acquire();
      state.upperTrack = upperUpper;
      if (state.speed != 0) { // A moving train also reserves the next critical section and sets the switch.
        middleCrit.acquire();
        state.hasMiddleCrit = true;
        setUpperSwitch(state.upperTrack, tsi);
      }
    }
  }

  public void atStation(SensorEvent event, int trainId, TrainState state)
      throws CommandException, InterruptedException {
    if (event.getStatus() != SensorEvent.ACTIVE) {
      return;
    }

    int x = event.getXpos();
    int y = event.getYpos();
    boolean upperStation = (x == 16 && y == 3) || (x == 15 && y == 5);
    boolean lowerStation = (x == 16 && y == 11) || (x == 15 && y == 13);

    if ((!upperStation && !lowerStation)
        || (upperStation != state.goingToUpper)) {
      return;
    }

    TSimInterface tsi = TSimInterface.getInstance();
    tsi.setSpeed(trainId, 0);

    if (lowerStation) {
      releaseUnderCrit(state);
    }

    // Wait at the station, then reverse direction.
    Thread.sleep(1000 + 20 * Math.abs(state.speed));
    state.speed = -state.speed;
    state.goingToUpper = !state.goingToUpper;

    // Reserve the next critical section before leaving.
    if (state.speed != 0) {
      if (upperStation) {
        acquire(middleCrit, trainId, state, tsi);
        setUpperSwitch(state.upperTrack, tsi);
      }
      else {
        acquire(underCrit, trainId, state, tsi);
        setLowerSwitch(state.lowerTrack, tsi);
      }
    }
    tsi.setSpeed(trainId, state.speed);
  }

  public void handleUpperTrack(
      SensorEvent event, int trainId, TrainState state)
      throws CommandException, InterruptedException {
    TSimInterface tsi = TSimInterface.getInstance();
    int x = event.getXpos();
    int y = event.getYpos();
    boolean active = event.getStatus() == SensorEvent.ACTIVE;

    if (((x == 6 && y == 6) || (x == 8 && y == 6))
        && !active && state.goingToUpper) {
      releaseMiddleCrit(state);
    }
    else if (x == 19 && y == 7 && active && state.goingToUpper) {
      state.upperTrack = chooseTrack(upperUpper, upperUnder);
      setUpperSwitch(state.upperTrack, tsi);
    }
    else if (x == 19 && y == 7 && !active && !state.goingToUpper) {
      state.upperTrack.release();
      state.upperTrack = null;
    }
  }

  public void handleMiddleTrack(
      SensorEvent event, int trainId, TrainState state)
      throws CommandException, InterruptedException {
    TSimInterface tsi = TSimInterface.getInstance();
    int x = event.getXpos();
    int y = event.getYpos();
    boolean active = event.getStatus() == SensorEvent.ACTIVE;

    if (x == 18 && y == 9) {
      if (active && !state.goingToUpper) {
        state.middleTrack = chooseTrack(middleUpper, middleUnder);
        setMiddleRightSwitch(state.middleTrack, tsi);
      }
      else if (!active && state.goingToUpper) {
        state.middleTrack.release();
        state.middleTrack = null;
      }
    }
    else if (((x == 13 && y == 9) || (x == 12 && y == 10)) && active) {
      if (state.goingToUpper) {
        acquire(middleCrit, trainId, state, tsi);
        setMiddleRightSwitch(state.middleTrack, tsi);
        tsi.setSpeed(trainId, state.speed);
      }
      else {
        releaseMiddleCrit(state);
      }
    }
    else if ((x == 7 && y == 9) || (x == 7 && y == 10)) {
      if (active && !state.goingToUpper) {
        acquire(underCrit, trainId, state, tsi);
        setMiddleLeftSwitch(state.middleTrack, tsi);
        tsi.setSpeed(trainId, state.speed);
      }
      else if (!active && state.goingToUpper) {
        releaseUnderCrit(state);
      }
    }
    else if (x == 2 && y == 9) {
      if (active && state.goingToUpper) {
        state.middleTrack = chooseTrack(middleUpper, middleUnder);
        setMiddleLeftSwitch(state.middleTrack, tsi);
      }
      else if (!active && !state.goingToUpper) {
        state.middleTrack.release();
        state.middleTrack = null;
      }
    }
  }

  public void handleLowerTrack(
      SensorEvent event, int trainId, TrainState state)
      throws CommandException, InterruptedException {
    int x = event.getXpos();
    int y = event.getYpos();
    boolean active = event.getStatus() == SensorEvent.ACTIVE;

    if (x == 1 && y == 11 && active && !state.goingToUpper) {
      state.lowerTrack = chooseTrack(underTrack, underUnder);
      setLowerSwitch(state.lowerTrack, TSimInterface.getInstance());
    }
    else if (x == 1 && y == 11 && !active && state.goingToUpper) {
      state.lowerTrack.release();
      state.lowerTrack = null;
    }
  }

  private void acquire(
      Semaphore semaphore, int trainId, TrainState state, TSimInterface tsi)
      throws CommandException, InterruptedException {
    // Stop only when the critical section is occupied.
    if (!semaphore.tryAcquire()) {
      tsi.setSpeed(trainId, 0);
      semaphore.acquire();
    }
    if (semaphore == middleCrit) {
      state.hasMiddleCrit = true;
    }
    else {
      state.hasUnderCrit = true;
    }
  }

  private Semaphore chooseTrack(Semaphore defaultTrack, Semaphore otherTrack)
      throws InterruptedException {
    // Prefer the main track and wait for the alternative if needed.
    if (defaultTrack.tryAcquire()) {
      return defaultTrack;
    }

    otherTrack.acquire();
    return otherTrack;
  }

  private void releaseMiddleCrit(TrainState state) {
    if (state.hasMiddleCrit) {
      middleCrit.release();
      state.hasMiddleCrit = false;
    }
  }

  private void releaseUnderCrit(TrainState state) {
    if (state.hasUnderCrit) {
      underCrit.release();
      state.hasUnderCrit = false;
    }
  }

  private void setUpperSwitch(Semaphore track, TSimInterface tsi)
      throws CommandException {
    tsi.setSwitch(17, 7, track == upperUpper
        ? TSimInterface.SWITCH_RIGHT : TSimInterface.SWITCH_LEFT);
  }

  private void setMiddleRightSwitch(Semaphore track, TSimInterface tsi)
      throws CommandException {
    tsi.setSwitch(15, 9, track == middleUpper
        ? TSimInterface.SWITCH_RIGHT : TSimInterface.SWITCH_LEFT);
  }

  private void setMiddleLeftSwitch(Semaphore track, TSimInterface tsi)
      throws CommandException {
    tsi.setSwitch(4, 9, track == middleUpper
        ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
  }

  private void setLowerSwitch(Semaphore track, TSimInterface tsi)
      throws CommandException {
    tsi.setSwitch(3, 11, track == underTrack
        ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
  }

  private static void validateSpeed(int speed) {
    if (speed < 0 || speed > MAX_SPEED) {
      throw new IllegalArgumentException("Speed must be in [0,17]");
    }
  }
}
