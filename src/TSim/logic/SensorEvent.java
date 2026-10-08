package tsim.logic;

import tsim.model.Pos;

public record SensorEvent(int trainNo, Pos pos, boolean active) {
}

