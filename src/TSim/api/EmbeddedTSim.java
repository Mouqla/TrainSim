package tsim.api;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import tsim.logic.MoveTrainResult;
import tsim.logic.MovementEngine;
import tsim.logic.SensorEvent;
import tsim.logic.SimulationLoop;
import tsim.logic.TrainManager;
import tsim.model.SimulationEvent;
import tsim.model.SimulationState;
import tsim.model.Status;
import tsim.model.SwitchState;
import tsim.ui.TSimApp;

public final class EmbeddedTSim {
    public interface Listener {
        default void sensorEvent(int trainNo, int x, int y, boolean active) {
        }

        default void trainEvent(int trainNo, SimulationEvent event) {
        }
    }

    private static final int RAIL_WIDTH = MovementEngine.DEFAULT_RAIL_WIDTH;

    private final SimulationState state;
    private final SimulationLoop loop;
    private final Runnable repaint;
    private final Listener listener;

    public EmbeddedTSim(SimulationState state, Runnable repaint, Listener listener) {
        this.state = state;
        this.loop = new SimulationLoop(state, RAIL_WIDTH, 0.1f, 5.0f);
        this.repaint = repaint;
        this.listener = listener;
    }

    public static EmbeddedTSim start(Path mapPath, int timerDelayMs, Listener listener) {
        AtomicReference<EmbeddedTSim> simulator = new AtomicReference<>();
        runOnEventThreadAndWait(() -> simulator.set(TSimApp.startEmbedded(mapPath, timerDelayMs, listener)));
        return simulator.get();
    }

    public void startTimer(int timerDelayMs) {
        new Timer(timerDelayMs, event -> tick()).start();
    }

    public Status setSpeed(int trainNo, float speed) {
        AtomicReference<Status> status = new AtomicReference<>();
        runOnEventThreadAndWait(() -> {
            status.set(TrainManager.setSpeed(state, trainNo, speed));
            repaint.run();
        });
        return status.get();
    }

    public Status setSwitch(int x, int y, SwitchState switchState) {
        AtomicReference<Status> status = new AtomicReference<>();
        runOnEventThreadAndWait(() -> {
            status.set(TrainManager.setSwitch(state, x, y, switchState, RAIL_WIDTH));
            repaint.run();
        });
        return status.get();
    }

    private void tick() {
        for (MoveTrainResult result : loop.tick()) {
            for (SensorEvent sensor : result.sensorEvents()) {
                listener.sensorEvent(result.trainNo(), sensor.pos().x(), sensor.pos().y(), sensor.active());
            }
            if (result.event() != SimulationEvent.NONE) {
                listener.trainEvent(result.trainNo(), result.event());
            }
        }
        repaint.run();
    }

    private static void runOnEventThreadAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(task);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while communicating with TSim", ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("Could not communicate with TSim", ex.getCause());
        }
    }
}
