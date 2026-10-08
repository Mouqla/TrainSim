package tsim.logic;

import java.util.List;
import java.util.OptionalInt;

import tsim.model.Direction;
import tsim.model.Pos;
import tsim.model.Train;
import tsim.model.TrainPart;

public final class CollisionDetector {
    private CollisionDetector() {
    }

    public static OptionalInt findCollision(List<Train> trains, TrainPart exclude, Pos pos) {
        for (Train train : trains) {
            for (TrainPart part : train.parts()) {
                if (part != exclude && part.pos().equals(pos)) {
                    return OptionalInt.of(train.no());
                }
            }
        }
        return OptionalInt.empty();
    }

    public static OptionalInt findCollision(
            List<Train> trains,
            TrainPart exclude,
            int pixelX,
            int pixelY,
            int railWidth) {
        int minDistance = railWidth / 2;
        for (Train train : trains) {
            for (TrainPart part : train.parts()) {
                if (part == exclude) {
                    continue;
                }
                PixelPos partPixel = pixelPosition(part, train.offset(), railWidth);
                if (Math.abs(partPixel.x() - pixelX) < minDistance
                        && Math.abs(partPixel.y() - pixelY) < minDistance) {
                    return OptionalInt.of(train.no());
                }
            }
        }
        return OptionalInt.empty();
    }

    public static OptionalInt findCollision(
            List<Train> trains,
            TrainPart exclude,
            TrainPart checkedPart,
            int checkedOffset,
            int railWidth) {
        PixelPos pixel = pixelPosition(checkedPart, checkedOffset, railWidth);
        return findCollision(trains, exclude, pixel.x(), pixel.y(), railWidth);
    }

    public static PixelPos pixelPosition(TrainPart part, int offset, int railWidth) {
        int x = part.pos().x() * railWidth;
        int y = part.pos().y() * railWidth;
        return addOffset(x, y, part.direction(), offset);
    }

    private static PixelPos addOffset(int x, int y, Direction direction, int offset) {
        return switch (direction) {
            case UP -> new PixelPos(x, y - offset);
            case RIGHT -> new PixelPos(x + offset, y);
            case DOWN -> new PixelPos(x, y + offset);
            case LEFT -> new PixelPos(x - offset, y);
            case NONE -> new PixelPos(x, y);
        };
    }

    public record PixelPos(int x, int y) {
    }
}
