package tsim.model;

import tsim.logic.DirectionTable;
import tsim.logic.DirectionUtils;

public final class RailGrid {
    public static final int MAX_SIZE = 200;

    private final int width;
    private final int height;
    private final RailCell[][] cells;

    public RailGrid(int width, int height) {
        if (width <= 0 || height <= 0 || width > MAX_SIZE || height > MAX_SIZE) {
            throw new IllegalArgumentException("Grid must be between 1 and 200 cells in each dimension");
        }
        this.width = width;
        this.height = height;
        this.cells = new RailCell[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells[x][y] = new RailCell();
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean isInBounds(Pos pos) {
        return pos.x() >= 0 && pos.y() >= 0 && pos.x() < width && pos.y() < height;
    }

    public RailCell get(int x, int y) {
        Pos pos = new Pos(x, y);
        if (!isInBounds(pos)) {
            throw new IndexOutOfBoundsException("Rail position out of bounds: " + pos);
        }
        return cells[x][y];
    }

    public void placeRail(int x, int y, RailType type) {
        RailCell cell = get(x, y);
        if (type.isStop() || type == RailType.NO_RAIL || cell.isStop() || cell.isEmpty() || cell.isComposite()) {
            cell.reset(type);
            return;
        }

        RailType current = cell.primaryType();
        if ((current == RailType.UP_LEFT && type == RailType.DOWN_RIGHT)
                || (current == RailType.UP_RIGHT && type == RailType.DOWN_LEFT)
                || (current == RailType.DOWN_LEFT && type == RailType.UP_RIGHT)
                || (current == RailType.DOWN_RIGHT && type == RailType.UP_LEFT)
                || current == type) {
            cell.reset(type);
            return;
        }

        cell.setSecondary(type);
        if (!cell.isCrossing()) {
            sortSwitch(cell);
        }
    }

    public void clearRail(int x, int y) {
        get(x, y).reset(RailType.NO_RAIL);
    }

    public void placeCustomBitmap(int x, int y, String name) {
        get(x, y).setCustomBitmap(name);
    }

    public boolean setSensor(int x, int y, SensorState state) {
        RailCell cell = get(x, y);
        if (state == SensorState.NONE || (cell.customBitmap() == null && !cell.isEmpty() && !cell.isComposite() && !cell.isStop())) {
            cell.setSensor(state);
            return true;
        }
        return false;
    }

    public Status setSwitch(int x, int y, SwitchState state) {
        RailCell cell = get(x, y);
        if (!cell.isSwitch()) {
            return Status.NO_SWITCH;
        }
        cell.setSwitchState(state);
        return Status.SUCCESS;
    }

    private static void sortSwitch(RailCell cell) {
        Direction inDir = DirectionUtils.commonEntry(cell);
        Direction dir = inDir.opposite().clockwise();
        Direction start = dir;
        do {
            if (DirectionTable.resolve(cell.primaryType(), inDir) == dir) {
                return;
            }
            if (DirectionTable.resolve(cell.secondaryType(), inDir) == dir) {
                RailType left = cell.primaryType();
                RailType right = cell.secondaryType();
                cell.reset(right);
                cell.setSecondary(left);
                return;
            }
            dir = dir.clockwise();
        } while (dir != start);
    }
}
