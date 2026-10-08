package tsim.logic;

import java.util.Optional;

import tsim.model.Direction;
import tsim.model.Pos;
import tsim.model.SimulationEvent;
import tsim.model.SimulationState;
import tsim.model.Status;
import tsim.model.SwitchState;
import tsim.model.Train;
import tsim.model.TrainPart;

public final class TrainManager {
    private TrainManager() {
    }

    public static CreateTrainResult createTrain(SimulationState state, Pos pos, Direction direction, int cars, int railWidth) {
        if (cars < 1 || !state.grid().isInBounds(pos)) {
            return new CreateTrainResult(SimulationEvent.DERAILMENT, 0);
        }
        MoveResult reverseCheck = MovementEngine.movePos(state.grid(), -railWidth, 0, direction, pos, railWidth);
        if (reverseCheck.event() != SimulationEvent.NONE) {
            return new CreateTrainResult(reverseCheck.event(), 0);
        }

        Train train = state.newTrain();
        Pos partPos = pos;
        Direction partDir = direction;
        for (int i = 1; i <= cars; i++) {
            train.addPart(new TrainPart(partPos, partDir, i == 1 ? TrainPart.Type.ENGINE : TrainPart.Type.CAR));
            if (i != cars) {
                MoveResult next = MovementEngine.movePos(state.grid(), -railWidth, 0, partDir, partPos, railWidth);
                if (next.event() != SimulationEvent.NONE) {
                    state.removeTrain(train);
                    return new CreateTrainResult(next.event(), 0);
                }
                partPos = next.pos();
                partDir = next.direction();
            }
        }
        return new CreateTrainResult(SimulationEvent.NONE, train.no());
    }

    public static Status deleteTrain(SimulationState state, int trainNo) {
        Optional<Train> train = state.train(trainNo);
        if (train.isEmpty()) {
            return Status.ILLEGAL_TRAIN_NO;
        }
        state.removeTrain(train.get());
        return Status.SUCCESS;
    }

    public static Status setSpeed(SimulationState state, int trainNo, float wantedSpeed) {
        Optional<Train> train = state.train(trainNo);
        if (train.isEmpty()) {
            return Status.ILLEGAL_TRAIN_NO;
        }
        if (train.get().speed() * wantedSpeed < 0.0f) {
            return Status.ILLEGAL_REVERSE;
        }
        train.get().setWantedSpeed(wantedSpeed);
        return Status.SUCCESS;
    }

    public static Status setSwitch(SimulationState state, int x, int y, SwitchState switchState) {
        return setSwitch(state, x, y, switchState, MovementEngine.DEFAULT_RAIL_WIDTH);
    }

    public static Status setSwitch(SimulationState state, int x, int y, SwitchState switchState, int railWidth) {
        Pos pos = new Pos(x, y);
        if (!state.grid().isInBounds(pos)) {
            return Status.ILLEGAL_RAIL_POS;
        }
        if (CollisionDetector.findCollision(
                state.trains(), null, pos.x() * railWidth, pos.y() * railWidth, railWidth).isPresent()) {
            return Status.ILLEGAL_SWITCHING;
        }
        return state.grid().setSwitch(x, y, switchState);
    }
}
