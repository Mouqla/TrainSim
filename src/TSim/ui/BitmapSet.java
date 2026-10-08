package tsim.ui;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

/**
 * Loads the XBM bitmaps of the original C tsim and composites them the same
 * way: XOR overlays for rail tiles and foreground/mask pairs for trains.
 * Bitmaps are classpath resources under {@code tsim/ui/bitmaps/}.
 */
public final class BitmapSet {
    /** A parsed XBM bitmap as a bit matrix; set bits are foreground pixels. */
    public static final class Bitmap {
        private final int width;
        private final int height;
        private final boolean[] bits;

        private Bitmap(int width, int height, boolean[] bits) {
            this.width = width;
            this.height = height;
            this.bits = bits;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public boolean get(int x, int y) {
            return bits[y * width + x];
        }

        public Bitmap copy() {
            return new Bitmap(width, height, bits.clone());
        }

        /** XORs {@code other} onto this bitmap, like the C GXor draw mode. */
        public void xor(Bitmap other) {
            int rows = Math.min(height, other.height);
            int columns = Math.min(width, other.width);
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < columns; x++) {
                    bits[y * width + x] ^= other.bits[y * other.width + x];
                }
            }
        }

        /** Set bits become {@code foreground}; unset bits become {@code background}. */
        public BufferedImage toImage(Color foreground, Color background) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, (get(x, y) ? foreground : background).getRGB());
                }
            }
            return image;
        }
    }

    private static final Pattern SIZE = Pattern.compile("#define\\s+\\w+_(width|height)\\s+(\\d+)");
    private static final Pattern HEX = Pattern.compile("0x([0-9a-fA-F]{1,2})");
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final Map<String, Optional<Bitmap>> BITS = new HashMap<>();
    private static final Map<String, Image> IMAGES = new HashMap<>();
    private static final Map<String, Image> SPRITES = new HashMap<>();

    private BitmapSet() {
    }

    /** Returns the raw bit matrix of a bitmap, or {@code null} if not found. */
    public static Bitmap bits(String name) {
        return BITS.computeIfAbsent(name, BitmapSet::load).orElse(null);
    }

    public static ImageIcon icon(String name) {
        Image image = image(name);
        return image == null ? null : new ImageIcon(image);
    }

    /** Renders a bitmap with black foreground and transparent background. */
    public static Image image(String name) {
        return IMAGES.computeIfAbsent(name, key -> {
            Bitmap bitmap = bits(key);
            return bitmap == null ? null : bitmap.toImage(Color.BLACK, TRANSPARENT);
        });
    }

    /**
     * Renders a train bitmap combined with its {@code <name>Mask} shape, like
     * the C sprite windows: foreground bits are black, mask-only bits are the
     * white body, and pixels outside the mask stay transparent.
     */
    public static Image sprite(String name) {
        return SPRITES.computeIfAbsent(name, key -> {
            Bitmap content = bits(key);
            if (content == null) {
                return null;
            }
            Bitmap mask = bits(key + "Mask");
            if (mask == null) {
                return image(key);
            }
            BufferedImage image = new BufferedImage(content.width(), content.height(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < content.height(); y++) {
                for (int x = 0; x < content.width(); x++) {
                    Color color = content.get(x, y) ? Color.BLACK
                            : mask.get(x, y) ? Color.WHITE
                            : TRANSPARENT;
                    image.setRGB(x, y, color.getRGB());
                }
            }
            return image;
        });
    }

    private static Optional<Bitmap> load(String name) {
        try (InputStream in = BitmapSet.class.getResourceAsStream("bitmaps/" + name)) {
            if (in != null) {
                return Optional.ofNullable(parseXbm(new String(in.readAllBytes())));
            }
        } catch (IOException ex) {
            return Optional.empty();
        }
        Path source = Path.of("src/tsim/ui/bitmaps", name);
        if (Files.exists(source)) {
            try {
                return Optional.ofNullable(parseXbm(Files.readString(source)));
            } catch (IOException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Bitmap parseXbm(String text) {
        Matcher sizeMatcher = SIZE.matcher(text);
        int width = 0;
        int height = 0;
        while (sizeMatcher.find()) {
            if (sizeMatcher.group(1).equals("width")) {
                width = Integer.parseInt(sizeMatcher.group(2));
            } else {
                height = Integer.parseInt(sizeMatcher.group(2));
            }
        }
        if (width <= 0 || height <= 0) {
            return null;
        }
        boolean[] bits = new boolean[width * height];
        int bytesPerRow = (width + 7) / 8;
        Matcher hexMatcher = HEX.matcher(text);
        int byteIndex = 0;
        while (hexMatcher.find()) {
            int value = Integer.parseInt(hexMatcher.group(1), 16);
            int row = byteIndex / bytesPerRow;
            int byteColumn = byteIndex % bytesPerRow;
            for (int bit = 0; bit < 8; bit++) {
                int x = byteColumn * 8 + bit;
                if (x < width && row < height) {
                    bits[row * width + x] = (value & (1 << bit)) != 0;
                }
            }
            byteIndex++;
        }
        return new Bitmap(width, height, bits);
    }
}
