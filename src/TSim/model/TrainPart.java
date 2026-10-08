package tsim.model;

public final class TrainPart {
    public enum Type { ENGINE, CAR }

    private Pos pos;
    private Direction direction;
    private final Type type;

    public TrainPart(Pos pos, Direction direction, Type type) {
        this.pos = pos;
        this.direction = direction;
        this.type = type;
    }

    public Pos pos() {
        return pos;
    }

    public void setPos(Pos pos) {
        this.pos = pos;
    }

    public Direction direction() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Type type() {
        return type;
    }
}

