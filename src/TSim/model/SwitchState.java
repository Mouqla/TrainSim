package tsim.model;

public enum SwitchState {
    LEFT("LeftSwitch"), RIGHT("RightSwitch");

    private final String cName;

    SwitchState(String cName) {
        this.cName = cName;
    }

    public String cName() {
        return cName;
    }

    public SwitchState flipped() {
        return this == LEFT ? RIGHT : LEFT;
    }
}

