package tsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class SimulationState {
    private final RailGrid grid;
    private final List<Train> trains = new ArrayList<>();
    private int nextTrainNo = 1;

    public SimulationState(RailGrid grid) {
        this.grid = grid;
    }

    public RailGrid grid() {
        return grid;
    }

    public List<Train> trains() {
        return Collections.unmodifiableList(trains);
    }

    public Optional<Train> train(int no) {
        return trains.stream().filter(train -> train.no() == no).findFirst();
    }

    public Train newTrain() {
        Train train = new Train(nextTrainNo++);
        trains.add(train);
        return train;
    }

    public void removeTrain(Train train) {
        trains.remove(train);
    }
}
