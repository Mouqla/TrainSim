package tsim.logic;

import tsim.model.SimulationEvent;

public record CreateTrainResult(SimulationEvent event, int trainNo) {
}

