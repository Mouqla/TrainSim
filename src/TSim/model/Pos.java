package tsim.model;

public record Pos(int x, int y) {
    public Pos add(Direction direction, int amount) {
        return switch (direction) {
            case UP -> new Pos(x, y - amount);
            case RIGHT -> new Pos(x + amount, y);
            case DOWN -> new Pos(x, y + amount);
            case LEFT -> new Pos(x - amount, y);
            case NONE -> this;
        };
    }
}

