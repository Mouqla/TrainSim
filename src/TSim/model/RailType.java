package tsim.model;

public enum RailType {
    NO_RAIL("NoRail"),
    HORIZONTAL("HorizontalRail"),
    VERTICAL("VerticalRail"),
    UP_LEFT("UpLeftRail"),
    UP_RIGHT("UpRightRail"),
    DOWN_LEFT("DownLeftRail"),
    DOWN_RIGHT("DownRightRail"),
    UP_STOP("UpStopRail"),
    RIGHT_STOP("RightStopRail"),
    DOWN_STOP("DownStopRail"),
    LEFT_STOP("LeftStopRail");

    private final String cName;

    RailType(String cName) {
        this.cName = cName;
    }

    public boolean isStop() {
        return this == UP_STOP || this == RIGHT_STOP || this == DOWN_STOP || this == LEFT_STOP;
    }

    public String cName() {
        return cName;
    }

    public static RailType fromCName(String name) {
        for (RailType type : values()) {
            if (type.cName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown rail type: " + name);
    }
}

