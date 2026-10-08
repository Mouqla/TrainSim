package tsim.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import tsim.model.RailCell;
import tsim.model.RailGrid;
import tsim.model.SimulationState;
import tsim.model.Train;

public final class MapFileWriter {
    private MapFileWriter() {
    }

    public static void write(SimulationState state, Path path) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path))) {
            RailGrid grid = state.grid();
            out.println("TrainLineFile 2");
            out.printf("%d %d%n", grid.width(), grid.height());
            for (int x = 0; x < grid.width(); x++) {
                for (int y = 0; y < grid.height(); y++) {
                    RailCell cell = grid.get(x, y);
                    if (cell.customBitmap() != null) {
                        out.printf("R %d %d Custom %s%n", x, y, cell.customBitmap());
                    } else if (!cell.isEmpty()) {
                        if (cell.isComposite()) {
                            out.printf("R %d %d 2 %s %s %s%n",
                                    x, y, cell.primaryType().cName(), cell.secondaryType().cName(), cell.sensor().cName());
                        } else {
                            out.printf("R %d %d 1 %s %s%n", x, y, cell.primaryType().cName(), cell.sensor().cName());
                        }
                    }
                }
            }
            for (Train train : state.trains()) {
                out.printf("T %d %d %d %s%n",
                        train.engine().pos().x(),
                        train.engine().pos().y(),
                        train.parts().size(),
                        toCName(train.engine().direction()));
            }
            out.println(".");
        }
    }

    private static String toCName(tsim.model.Direction direction) {
        return switch (direction) {
            case NONE -> "NoDir";
            case UP -> "Up";
            case RIGHT -> "Right";
            case DOWN -> "Down";
            case LEFT -> "Left";
        };
    }
}
