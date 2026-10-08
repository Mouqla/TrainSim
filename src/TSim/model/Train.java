package tsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Train {
    private final int no;
    private int offset;
    private float speed;
    private float wantedSpeed;
    private float offsetFraction;
    private final List<TrainPart> parts = new ArrayList<>();

    public Train(int no) {
        this.no = no;
    }

    public int no() {
        return no;
    }

    public int offset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public float speed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float wantedSpeed() {
        return wantedSpeed;
    }

    public void setWantedSpeed(float wantedSpeed) {
        this.wantedSpeed = wantedSpeed;
    }

    public float offsetFraction() {
        return offsetFraction;
    }

    public void setOffsetFraction(float offsetFraction) {
        this.offsetFraction = offsetFraction;
    }

    public List<TrainPart> parts() {
        return Collections.unmodifiableList(parts);
    }

    public void addPart(TrainPart part) {
        parts.add(part);
    }

    public TrainPart engine() {
        return parts.get(0);
    }
}

