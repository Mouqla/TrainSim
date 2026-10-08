package tsim.model;

public enum SensorState {
    NONE("NoSensor"), SENSOR("Sensor");

    private final String cName;

    SensorState(String cName) {
        this.cName = cName;
    }

    public String cName() {
        return cName;
    }

    public static SensorState fromCName(String name) {
        for (SensorState state : values()) {
            if (state.cName.equalsIgnoreCase(name)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown sensor state: " + name);
    }
}

