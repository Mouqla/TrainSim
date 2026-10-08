package tsim.model;

public final class RailCell {
    private final RailType[] types = {RailType.NO_RAIL, RailType.NO_RAIL};
    private SensorState sensor = SensorState.NONE;
    private SwitchState switchState = SwitchState.LEFT;
    private String customBitmap;

    public RailType type(SwitchState state) {
        return types[state == SwitchState.LEFT ? 0 : 1];
    }

    public RailType primaryType() {
        return types[0];
    }

    public RailType secondaryType() {
        return types[1];
    }

    public void reset(RailType type) {
        types[0] = type;
        types[1] = RailType.NO_RAIL;
        switchState = SwitchState.LEFT;
        sensor = SensorState.NONE;
        customBitmap = null;
    }

    public void setSecondary(RailType type) {
        types[1] = type;
    }

    public SensorState sensor() {
        return sensor;
    }

    public void setSensor(SensorState sensor) {
        this.sensor = sensor;
    }

    public String customBitmap() {
        return customBitmap;
    }

    public void setCustomBitmap(String customBitmap) {
        types[0] = RailType.NO_RAIL;
        types[1] = RailType.NO_RAIL;
        switchState = SwitchState.LEFT;
        sensor = SensorState.NONE;
        this.customBitmap = customBitmap;
    }

    public SwitchState switchState() {
        return switchState;
    }

    public void setSwitchState(SwitchState switchState) {
        this.switchState = switchState;
    }

    public boolean isEmpty() {
        return primaryType() == RailType.NO_RAIL && customBitmap == null;
    }

    public boolean isComposite() {
        return secondaryType() != RailType.NO_RAIL;
    }

    public boolean isCrossing() {
        return (primaryType() == RailType.HORIZONTAL && secondaryType() == RailType.VERTICAL)
                || (primaryType() == RailType.VERTICAL && secondaryType() == RailType.HORIZONTAL);
    }

    public boolean isSwitch() {
        return isComposite() && !isCrossing();
    }

    public boolean isStop() {
        return primaryType().isStop();
    }
}
