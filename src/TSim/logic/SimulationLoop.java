package tsim.logic;

import java.util.ArrayList;
import java.util.List;

import tsim.model.SimulationState;
import tsim.model.Train;

public final class SimulationLoop {
    private final SimulationState state;
    private final int railWidth;
    private final float tickSeconds;
    private final float acceleration;

    public SimulationLoop(SimulationState state, int railWidth, float tickSeconds, float acceleration) {
        this.state = state;
        this.railWidth = railWidth;
        this.tickSeconds = tickSeconds;
        this.acceleration = acceleration;
    }

    public List<MoveTrainResult> tick() {
        List<MoveTrainResult> results = new ArrayList<>();
        for (Train train : state.trains()) {
            train.setSpeed(SpeedController.updateSpeed(train.speed(), train.wantedSpeed(), tickSeconds, acceleration));
            train.setOffsetFraction(train.offsetFraction() + tickSeconds * Math.abs(train.speed()));
            if (train.offsetFraction() >= 1.0f) {
                int pixels = (int) train.offsetFraction();
                train.setOffsetFraction(train.offsetFraction() - pixels);
                int signedPixels = (int) Math.signum(train.speed()) * pixels;
                MoveTrainResult result = TrainMover.moveTrain(state, train.no(), signedPixels, railWidth);
                results.add(result);
            }
        }
        return results;
    }
}

