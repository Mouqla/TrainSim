package tsim.logic;

import tsim.model.Direction;
import tsim.model.Pos;
import tsim.model.RailCell;
import tsim.model.RailGrid;
import tsim.model.SimulationEvent;

public final class MovementEngine {
    public static final int DEFAULT_RAIL_WIDTH = 20;

    private MovementEngine() {
    }

    public static MoveResult movePos(RailGrid grid, int distance, int offset, Direction dir, Pos pos, int railWidth) {
        int newOffset = offset + distance;
        Direction newDir = dir;
        Pos newPos = pos;
        while (newOffset >= railWidth || newOffset < 0) {
            if (distance < 0) {
                newOffset += railWidth;
                RailCell current = grid.get(newPos.x(), newPos.y());
                if (current.isStop()) {
                    return new MoveResult(offset, dir, pos, SimulationEvent.STOP_COLLISION);
                }
                newDir = DirectionTable.resolve(current, newDir.opposite()).opposite();
                if (newDir == Direction.NONE) {
                    return new MoveResult(offset, dir, pos, SimulationEvent.DERAILMENT);
                }
                newPos = newPos.add(newDir, -1);
                if (!grid.isInBounds(newPos)) {
                    return new MoveResult(offset, dir, pos, SimulationEvent.DERAILMENT);
                }
            } else {
                newOffset -= railWidth;
                newPos = newPos.add(newDir, 1);
                if (!grid.isInBounds(newPos)) {
                    return new MoveResult(offset, dir, pos, SimulationEvent.DERAILMENT);
                }
                RailCell current = grid.get(newPos.x(), newPos.y());
                if (current.isStop()) {
                    return new MoveResult(offset, dir, pos, SimulationEvent.STOP_COLLISION);
                }
                newDir = DirectionTable.resolve(current, newDir);
                if (newDir == Direction.NONE) {
                    return new MoveResult(offset, dir, pos, SimulationEvent.DERAILMENT);
                }
            }
        }
        return new MoveResult(newOffset, newDir, newPos, SimulationEvent.NONE);
    }
}
