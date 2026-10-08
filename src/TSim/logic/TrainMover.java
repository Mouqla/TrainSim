package tsim.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import tsim.model.Pos;
import tsim.model.SensorState;
import tsim.model.SimulationEvent;
import tsim.model.SimulationState;
import tsim.model.Train;
import tsim.model.TrainPart;

public final class TrainMover {
    private TrainMover() {
    }

    public static MoveTrainResult moveTrain(SimulationState state, int trainNo, int distance, int railWidth) {
        Optional<Train> trainOptional = state.train(trainNo);
        if (trainOptional.isEmpty() || distance == 0) {
            return new MoveTrainResult(trainNo, SimulationEvent.NONE, List.of());
        }
        Train train = trainOptional.get();
        boolean reverse = distance < 0;
        int newOffset = train.offset();
        List<SensorEvent> sensorEvents = new ArrayList<>();

        for (TrainPart part : train.parts()) {
            Pos oldPos = part.pos();
            MoveResult result = MovementEngine.movePos(
                    state.grid(), distance, train.offset(), part.direction(), part.pos(), railWidth);
            if (result.event() != SimulationEvent.NONE) {
                train.setSpeed(0);
                train.setWantedSpeed(0);
                return new MoveTrainResult(trainNo, result.event(), sensorEvents);
            }
            newOffset = result.offset();
            part.setPos(result.pos());
            part.setDirection(result.direction());

            if (!oldPos.equals(part.pos())) {
                boolean engine = part == train.engine();
                boolean last = part == train.parts().get(train.parts().size() - 1);
                if (engine) {
                    if (reverse && sensorAt(state, oldPos)) {
                        sensorEvents.add(new SensorEvent(train.no(), oldPos, false));
                    } else if (!reverse && sensorAt(state, part.pos())) {
                        sensorEvents.add(new SensorEvent(train.no(), part.pos(), true));
                    }
                }
                if (last) {
                    if (reverse && sensorAt(state, part.pos())) {
                        sensorEvents.add(new SensorEvent(train.no(), part.pos(), true));
                    } else if (!reverse && sensorAt(state, oldPos)) {
                        sensorEvents.add(new SensorEvent(train.no(), oldPos, false));
                    }
                }
            }
        }
        train.setOffset(newOffset);

        TrainPart collisionPart = reverse ? train.parts().get(train.parts().size() - 1) : train.engine();
        CollisionDetector.PixelPos collisionPixel = CollisionDetector.pixelPosition(collisionPart, train.offset(), railWidth);
        if (CollisionDetector.findCollision(
                state.trains(), collisionPart, collisionPixel.x(), collisionPixel.y(), railWidth).isPresent()) {
            train.setSpeed(0);
            train.setWantedSpeed(0);
            return new MoveTrainResult(trainNo, SimulationEvent.TRAIN_COLLISION, sensorEvents);
        }
        return new MoveTrainResult(trainNo, SimulationEvent.NONE, sensorEvents);
    }

    private static boolean sensorAt(SimulationState state, Pos pos) {
        return state.grid().isInBounds(pos)
                && state.grid().get(pos.x(), pos.y()).sensor() == SensorState.SENSOR;
    }
}
