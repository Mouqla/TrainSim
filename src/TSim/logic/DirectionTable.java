package tsim.logic;

import tsim.model.Direction;
import tsim.model.RailType;

public final class DirectionTable {
    private DirectionTable() {
    }

    public static Direction resolve(RailType type, Direction incoming) {
        return switch (type) {
            case NO_RAIL -> Direction.NONE;
            case HORIZONTAL -> switch (incoming) {
                case RIGHT -> Direction.RIGHT;
                case LEFT -> Direction.LEFT;
                default -> Direction.NONE;
            };
            case VERTICAL -> switch (incoming) {
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
                default -> Direction.NONE;
            };
            case UP_LEFT -> switch (incoming) {
                case RIGHT -> Direction.UP;
                case DOWN -> Direction.LEFT;
                default -> Direction.NONE;
            };
            case UP_RIGHT -> switch (incoming) {
                case DOWN -> Direction.RIGHT;
                case LEFT -> Direction.UP;
                default -> Direction.NONE;
            };
            case DOWN_LEFT -> switch (incoming) {
                case UP -> Direction.LEFT;
                case RIGHT -> Direction.DOWN;
                default -> Direction.NONE;
            };
            case DOWN_RIGHT -> switch (incoming) {
                case UP -> Direction.RIGHT;
                case LEFT -> Direction.DOWN;
                default -> Direction.NONE;
            };
            case UP_STOP, RIGHT_STOP, DOWN_STOP, LEFT_STOP -> Direction.NONE;
        };
    }

    public static Direction resolve(tsim.model.RailCell cell, Direction incoming) {
        Direction direction = resolve(cell.type(cell.switchState()), incoming);
        if (direction == Direction.NONE && cell.isCrossing()) {
            direction = resolve(cell.type(cell.switchState().flipped()), incoming);
        }
        return direction;
    }
}

