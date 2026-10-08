package tsim.model;

public enum Direction {
    NONE, UP, RIGHT, DOWN, LEFT;

    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case RIGHT -> LEFT;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case NONE -> NONE;
        };
    }

    public Direction clockwise() {
        return switch (this) {
            case UP -> RIGHT;
            case RIGHT -> DOWN;
            case DOWN -> LEFT;
            case LEFT -> UP;
            case NONE -> NONE;
        };
    }
}

