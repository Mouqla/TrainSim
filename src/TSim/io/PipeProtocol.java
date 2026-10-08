package tsim.io;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tsim.model.Pos;
import tsim.model.SimulationEvent;
import tsim.model.Status;
import tsim.model.SwitchState;

public final class PipeProtocol {
    private static final Pattern INT_PREFIX = Pattern.compile("[+-]?\\d+");
    private static final Pattern FLOAT_PREFIX = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");

    private PipeProtocol() {
    }

    public sealed interface Command permits SetSpeedCommand, SetSwitchCommand, UnknownCommand {
    }

    public record SetSpeedCommand(int trainNo, float speed) implements Command {
    }

    public record SetSwitchCommand(int x, int y, SwitchState state) implements Command {
    }

    public record UnknownCommand(String name) implements Command {
    }

    public static Command parseCommand(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length > 0 && parts[0].equalsIgnoreCase("SetSpeed")) {
            return new SetSpeedCommand(parseInt(parts, 1), parseFloat(parts, 2));
        }
        if (parts.length > 0 && parts[0].equalsIgnoreCase("SetSwitch")) {
            return new SetSwitchCommand(parseInt(parts, 1), parseInt(parts, 2), parseSwitchState(parts, 3));
        }
        return new UnknownCommand(parts.length == 0 ? "" : parts[0]);
    }

    private static int parseInt(String[] parts, int index) {
        String value = token(parts, index);
        Matcher matcher = INT_PREFIX.matcher(value);
        if (matcher.lookingAt()) {
            return Integer.parseInt(matcher.group());
        }
        System.err.println("Warning: Illegal integer value: \"" + value + "\"");
        return 0;
    }

    private static float parseFloat(String[] parts, int index) {
        String value = token(parts, index);
        Matcher matcher = FLOAT_PREFIX.matcher(value);
        if (matcher.lookingAt()) {
            return Float.parseFloat(matcher.group());
        }
        System.err.println("Warning: Illegal float value: \"" + value + "\"");
        return 0.0f;
    }

    private static SwitchState parseSwitchState(String[] parts, int index) {
        String value = token(parts, index);
        if (value.equalsIgnoreCase("LeftSwitch")) {
            return SwitchState.LEFT;
        }
        if (value.equalsIgnoreCase("RightSwitch")) {
            return SwitchState.RIGHT;
        }
        System.err.println("Warning: Cannot recognize \"" + value + "\"");
        return SwitchState.LEFT;
    }

    private static String token(String[] parts, int index) {
        return index < parts.length ? parts[index] : "";
    }

    public static String formatSensorEvent(int trainNo, Pos pos, boolean active) {
        return "Sensor " + trainNo + " " + pos.x() + " " + pos.y() + " " + (active ? "active" : "inactive");
    }

    public static String formatTrainEvent(int trainNo, SimulationEvent event) {
        return switch (event) {
            case NONE -> "NoEvent " + trainNo;
            case TRAIN_COLLISION -> "TrainCollisionEvent " + trainNo;
            case STOP_COLLISION -> "StopCollisionEvent " + trainNo;
            case DERAILMENT -> "DerailmentEvent " + trainNo;
        };
    }

    public static String formatStatus(Status status) {
        return switch (status) {
            case SUCCESS -> "SuccessStatus";
            case NO_SWITCH -> "NoSwitchStatus";
            case ILLEGAL_REVERSE -> "IllegalReverseStatus";
            case ILLEGAL_TRAIN_NO -> "IllegalTrainNoStatus";
            case ILLEGAL_RAIL_POS -> "IllegalRailPosStatus";
            case ILLEGAL_SWITCHING -> "IllegalSwitchingStatus";
        };
    }
}
