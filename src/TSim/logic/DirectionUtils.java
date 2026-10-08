package tsim.logic;

import tsim.model.Direction;
import tsim.model.RailCell;

public final class DirectionUtils {
    private DirectionUtils() {
    }

    public static Direction commonEntry(RailCell cell) {
        Direction dir = Direction.UP;
        do {
            if (DirectionTable.resolve(cell.primaryType(), dir) != Direction.NONE
                    && DirectionTable.resolve(cell.secondaryType(), dir) != Direction.NONE) {
                return dir;
            }
            dir = dir.clockwise();
        } while (dir != Direction.UP);
        return Direction.NONE;
    }
}

