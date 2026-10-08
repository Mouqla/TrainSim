package tsim.logic;

import java.util.List;

import tsim.model.SimulationEvent;

public record MoveTrainResult(int trainNo, SimulationEvent event, List<SensorEvent> sensorEvents) {
}
