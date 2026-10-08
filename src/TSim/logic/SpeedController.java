package tsim.logic;

public final class SpeedController {
    private SpeedController() {
    }

    public static float updateSpeed(float current, float wanted, float deltaTime, float acceleration) {
        if (current == wanted) {
            return current;
        }
        float diff = acceleration * deltaTime;
        float direction = Math.signum(wanted - current);
        float next = current + direction * diff;
        if (Math.signum(wanted - next) != direction) {
            return wanted;
        }
        return next;
    }
}

