package tsim.logic;

import tsim.model.Direction;
import tsim.model.Pos;
import tsim.model.SimulationEvent;

public record MoveResult(int offset, Direction direction, Pos pos, SimulationEvent event) {
}

