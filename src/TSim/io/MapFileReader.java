package tsim.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import tsim.logic.TrainManager;
import tsim.model.Direction;
import tsim.model.Pos;
import tsim.model.RailGrid;
import tsim.model.RailType;
import tsim.model.SensorState;
import tsim.model.SimulationState;

public final class MapFileReader {
    private MapFileReader() {
    }

    public static SimulationState read(Path path, int railWidth) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String header = requireLine(reader, "header").trim();
            if (!header.startsWith("TrainLineFile ")) {
                throw new IOException("Illegal header: " + header);
            }
            String[] size = requireLine(reader, "size").trim().split("\\s+");
            if (size.length != 2) {
                throw new IOException("Expected grid size line");
            }
            SimulationState state = new SimulationState(new RailGrid(Integer.parseInt(size[0]), Integer.parseInt(size[1])));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equals(".")) {
                    return state;
                }
                String[] parts = line.split("\\s+");
                if (parts[0].equals("R")) {
                    readRail(state, parts);
                } else if (parts[0].equals("T")) {
                    readTrain(state, parts, railWidth);
                } else {
                    throw new IOException("Unexpected map line: " + line);
                }
            }
            throw new IOException("Missing end marker");
        }
    }

    private static void readRail(SimulationState state, String[] parts) throws IOException {
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        if (parts[3].equalsIgnoreCase("Custom")) {
            if (parts.length != 5) {
                throw new IOException("Malformed custom bitmap line");
            }
            state.grid().placeCustomBitmap(x, y, parts[4]);
            return;
        }
        if (parts.length < 6) {
            throw new IOException("Malformed rail line");
        }
        int count = Integer.parseInt(parts[3]);
        if (count != 1 && count != 2) {
            throw new IOException("Unsupported rail count: " + count);
        }
        state.grid().placeRail(x, y, RailType.fromCName(parts[4]));
        int sensorIndex = 5;
        if (count == 2) {
            state.grid().placeRail(x, y, RailType.fromCName(parts[5]));
            sensorIndex = 6;
        }
        if (parts.length <= sensorIndex) {
            throw new IOException("Missing sensor state");
        }
        state.grid().setSensor(x, y, SensorState.fromCName(parts[sensorIndex]));
    }

    private static void readTrain(SimulationState state, String[] parts, int railWidth) throws IOException {
        if (parts.length != 5) {
            throw new IOException("Malformed train line");
        }
        Direction direction = switch (parts[4]) {
            case "Up" -> Direction.UP;
            case "Right" -> Direction.RIGHT;
            case "Down" -> Direction.DOWN;
            case "Left" -> Direction.LEFT;
            default -> throw new IOException("Unknown direction: " + parts[4]);
        };
        TrainManager.createTrain(
                state,
                new Pos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])),
                direction,
                Integer.parseInt(parts[3]),
                railWidth);
    }

    private static String requireLine(BufferedReader reader, String description) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Missing " + description);
        }
        return line;
    }
}
