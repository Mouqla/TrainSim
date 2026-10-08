package tsim.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import tsim.api.EmbeddedTSim;
import tsim.io.MapFileReader;
import tsim.io.MapFileWriter;
import tsim.io.PipeProtocol;
import tsim.logic.CollisionDetector;
import tsim.logic.CreateTrainResult;
import tsim.logic.DirectionUtils;
import tsim.logic.MovementEngine;
import tsim.logic.TrainManager;
import tsim.model.Direction;
import tsim.model.Pos;
import tsim.model.RailCell;
import tsim.model.RailGrid;
import tsim.model.RailType;
import tsim.model.SensorState;
import tsim.model.SimulationEvent;
import tsim.model.SimulationState;
import tsim.model.Status;
import tsim.model.SwitchState;
import tsim.model.Train;
import tsim.model.TrainPart;

public final class TSimApp {
    private static final int RAIL_WIDTH = MovementEngine.DEFAULT_RAIL_WIDTH;
    private static final int WHEEL_OFFSET = 11;
    private static final int TRAIN_DRAW_ADJUST = RAIL_WIDTH - WHEEL_OFFSET - 1;
    private static final Path DEFAULT_SAVE = Path.of("java-tsim-save.map");
    private static final int DEFAULT_TIMER_DELAY_MS = 100;

    private TSimApp() {
    }

    public static void main(String[] args) {
        AppOptions options;
        try {
            options = parseOptions(args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.err.println("Usage: java-tsim [-s milliseconds|--speed milliseconds] [mapfile]");
            System.exit(1);
            return;
        }
        SwingUtilities.invokeLater(() -> start(options));
    }

    private static void start(AppOptions options) {
        SimulationState state = loadState(options);
        RailPanel panel = openWindow(state, options.mapPath(), true);
        EmbeddedTSim simulator = new EmbeddedTSim(state, panel::repaint, new EmbeddedTSim.Listener() {
            @Override
            public void sensorEvent(int trainNo, int x, int y, boolean active) {
                writePipeLine(PipeProtocol.formatSensorEvent(trainNo, new Pos(x, y), active));
            }

            @Override
            public void trainEvent(int trainNo, SimulationEvent event) {
                writePipeLine(PipeProtocol.formatTrainEvent(trainNo, event));
            }
        });
        startPipeReader(simulator);
        simulator.startTimer(options.timerDelayMs());
    }

    public static void startStandalone(Path mapPath, int timerDelayMs) {
        SwingUtilities.invokeLater(() -> {
            RailPanel panel = openWindow(loadState(new AppOptions(timerDelayMs, mapPath)), mapPath, true);
            new EmbeddedTSim(panel.state, panel::repaint, new EmbeddedTSim.Listener() {
            }).startTimer(timerDelayMs);
        });
    }

    public static EmbeddedTSim startEmbedded(Path mapPath, int timerDelayMs, EmbeddedTSim.Listener listener) {
        RailPanel panel = openWindow(loadState(new AppOptions(timerDelayMs, mapPath)), mapPath, false);
        EmbeddedTSim simulator = new EmbeddedTSim(panel.state, panel::repaint, listener);
        simulator.startTimer(timerDelayMs);
        return simulator;
    }

    private static RailPanel openWindow(SimulationState state, Path mapPath, boolean showControls) {
        JFrame frame = new JFrame("TrainSim");
        RailPanel panel = new RailPanel(state, mapPath != null ? mapPath : DEFAULT_SAVE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (showControls) {
            frame.add(toolbar(panel), BorderLayout.NORTH);
        }
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        return panel;
    }

    private static AppOptions parseOptions(String[] args) {
        int timerDelayMs = DEFAULT_TIMER_DELAY_MS;
        Path mapPath = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s") || arg.equals("--speed")) {
                if (++i >= args.length) {
                    throw new IllegalArgumentException("Speed option requires an integer.");
                }
                timerDelayMs = parseTimerDelay(args[i]);
            } else if (arg.startsWith("--speed=")) {
                timerDelayMs = parseTimerDelay(arg.substring("--speed=".length()));
            } else if (arg.startsWith("-")) {
                throw new IllegalArgumentException("Unknown option: " + arg);
            } else if (mapPath == null) {
                mapPath = Path.of(arg);
            } else {
                throw new IllegalArgumentException("Too many non-option arguments.");
            }
        }
        return new AppOptions(timerDelayMs, mapPath);
    }

    private static int parseTimerDelay(String value) {
        try {
            int delay = Integer.parseInt(value);
            if (delay < 0 || delay > 100) {
                throw new IllegalArgumentException("Speed option requires an integer within [0..100].");
            }
            return delay;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Speed option requires an integer.");
        }
    }

    private static void startPipeReader(EmbeddedTSim simulator) {
        Thread reader = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = in.readLine()) != null) {
                    dispatchPipeCommand(simulator, line);
                }
                System.exit(0);
            } catch (IOException ex) {
                System.err.println("Warning: Could not read pipe command: " + ex.getMessage());
            }
        }, "tsim-pipe-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private static void dispatchPipeCommand(EmbeddedTSim simulator, String line) {
        if (line.isBlank()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Status status = Status.SUCCESS;
            PipeProtocol.Command command = PipeProtocol.parseCommand(line);
            if (command instanceof PipeProtocol.SetSpeedCommand speed) {
                status = simulator.setSpeed(speed.trainNo(), speed.speed());
            } else if (command instanceof PipeProtocol.SetSwitchCommand sw) {
                status = simulator.setSwitch(sw.x(), sw.y(), sw.state());
            } else if (command instanceof PipeProtocol.UnknownCommand unknown) {
                System.err.println("Warning: Illegal command received: " + unknown.name());
            }
            writePipeLine(PipeProtocol.formatStatus(status));
        });
    }

    private static void writePipeLine(String line) {
        System.out.println(line);
        System.out.flush();
    }

    private static JToolBar toolbar(RailPanel panel) {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        ButtonGroup group = new ButtonGroup();
        addModeButton(toolbar, group, "station", BitmapSet.icon("station"), () -> panel.setCustomBitmapTool("station"));
        for (RailType type : RailType.values()) {
            if (type != RailType.NO_RAIL) {
                JToggleButton rail = toolButton(type.cName(), BitmapSet.icon(type.cName()), true);
                rail.addActionListener(event -> panel.setRailTool(type));
                group.add(rail);
                toolbar.add(rail);
                if (type == RailType.HORIZONTAL) {
                    rail.setSelected(true);
                }
            }
        }
        addModeButton(toolbar, group, "Sensor", BitmapSet.icon("HorizontalSensor"), () -> panel.setTool(Tool.SENSOR));
        addModeButton(toolbar, group, "Train", BitmapSet.icon("LeftEngine"), () -> panel.setTool(Tool.TRAIN));
        addModeButton(toolbar, group, "Delete", null, () -> panel.setTool(Tool.DELETE));
        addModeButton(toolbar, group, "Switch", null, () -> panel.setTool(Tool.SWITCH));
        addModeButton(toolbar, group, "Speed", null, () -> panel.setTool(Tool.SPEED));
        toolbar.addSeparator();
        toolbar.add(new JLabel("cars"));
        toolbar.add(panel.carsSpinner);
        toolbar.add(panel.directionCombo);
        toolbar.add(new JLabel("speed"));
        toolbar.add(panel.speedSpinner);
        toolbar.addSeparator();
        JButton save = new JButton("Save");
        save.addActionListener((ActionEvent event) -> panel.save());
        JButton quit = new JButton("Quit");
        quit.addActionListener((ActionEvent event) -> System.exit(0));
        toolbar.add(save);
        toolbar.add(quit);
        toolbar.addSeparator();
        toolbar.add(panel.coordinates);
        toolbar.add(panel.message);
        return toolbar;
    }

    private static void addModeButton(JToolBar toolbar, ButtonGroup group, String label, ImageIcon icon, Runnable action) {
        JToggleButton button = toolButton(label, icon, icon != null);
        button.addActionListener(event -> action.run());
        group.add(button);
        toolbar.add(button);
    }

    private static JToggleButton toolButton(String label, ImageIcon icon, boolean iconOnly) {
        JToggleButton button = new JToggleButton(icon);
        button.setToolTipText(label);
        if (!iconOnly) {
            button.setText(label);
        }
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(iconOnly ? 28 : 70, 28));
        return button;
    }

    private static SimulationState loadState(AppOptions options) {
        if (options.mapPath() != null) {
            try {
                return MapFileReader.read(options.mapPath(), RAIL_WIDTH);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Could not load map: " + options.mapPath(), ex);
            }
        }
        SimulationState state = new SimulationState(new RailGrid(20, 20));
        for (int x = 2; x <= 8; x++) {
            state.grid().placeRail(x, 5, RailType.HORIZONTAL);
        }
        state.grid().placeRail(1, 5, RailType.RIGHT_STOP);
        state.grid().placeRail(9, 5, RailType.LEFT_STOP);
        TrainManager.createTrain(state, new Pos(3, 5), Direction.RIGHT, 2, RAIL_WIDTH);
        return state;
    }

    private record AppOptions(int timerDelayMs, Path mapPath) {
    }

    private static final class RailPanel extends JPanel {
        private final SimulationState state;
        private final Path savePath;
        private final JLabel coordinates = new JLabel("v 0.84");
        private final JLabel message = new JLabel(" ");
        private final JSpinner carsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        private final JSpinner speedSpinner = new JSpinner(new SpinnerNumberModel(20.0, -100.0, 100.0, 1.0));
        private final JComboBox<Direction> directionCombo = new JComboBox<>(
                new Direction[] {Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.DOWN});
        private final Map<TileKey, Image> tileCache = new HashMap<>();
        private RailType selectedRail = RailType.HORIZONTAL;
        private Tool tool = Tool.RAIL;
        private String selectedCustomBitmap;

        private record TileKey(RailType primary, RailType secondary, Direction switchEntry,
                SwitchState switchState, boolean sensor) {
        }

        private RailPanel(SimulationState state, Path savePath) {
            this.state = state;
            this.savePath = savePath;
            setPreferredSize(new Dimension(state.grid().width() * RAIL_WIDTH, state.grid().height() * RAIL_WIDTH));
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    handleClick(event);
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    updateCoordinates(event);
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    updateCoordinates(event);
                }
            });
        }

        private void setRailTool(RailType selectedRail) {
            this.selectedRail = selectedRail;
            this.selectedCustomBitmap = null;
            this.tool = Tool.RAIL;
        }

        private void setCustomBitmapTool(String selectedCustomBitmap) {
            this.selectedCustomBitmap = selectedCustomBitmap;
            this.tool = Tool.CUSTOM;
        }

        private void setTool(Tool tool) {
            this.tool = tool;
        }

        private void updateCoordinates(MouseEvent event) {
            int x = event.getX() / RAIL_WIDTH;
            int y = event.getY() / RAIL_WIDTH;
            coordinates.setText(state.grid().isInBounds(new Pos(x, y)) ? x + " " + y : "v 0.84");
        }

        private void handleClick(MouseEvent event) {
            int x = event.getX() / RAIL_WIDTH;
            int y = event.getY() / RAIL_WIDTH;
            if (!state.grid().isInBounds(new Pos(x, y))) {
                return;
            }
            if (SwingUtilities.isRightMouseButton(event)) {
                state.grid().clearRail(x, y);
                showStatus("Deleted rail");
            } else if (tool == Tool.TRAIN) {
                CreateTrainResult result = TrainManager.createTrain(
                        state,
                        new Pos(x, y),
                        (Direction) directionCombo.getSelectedItem(),
                        (Integer) carsSpinner.getValue(),
                        RAIL_WIDTH);
                showStatus(result.event().name());
            } else if (tool == Tool.SWITCH) {
                RailCell cell = state.grid().get(x, y);
                Status status = TrainManager.setSwitch(state, x, y, cell.switchState().flipped(), RAIL_WIDTH);
                showStatus(status.name());
            } else if (tool == Tool.DELETE) {
                state.grid().clearRail(x, y);
                showStatus("Deleted rail");
            } else if (tool == Tool.SENSOR) {
                RailCell cell = state.grid().get(x, y);
                if (!cell.isEmpty()) {
                    cell.setSensor(cell.sensor() == SensorState.NONE ? SensorState.SENSOR : SensorState.NONE);
                    showStatus(cell.sensor().cName());
                }
            } else if (tool == Tool.SPEED) {
                Train train = trainAt(new Pos(x, y));
                if (train != null) {
                    Status status = TrainManager.setSpeed(state, train.no(), ((Number) speedSpinner.getValue()).floatValue());
                    showStatus(status.name());
                }
            } else if (tool == Tool.CUSTOM) {
                state.grid().placeCustomBitmap(x, y, selectedCustomBitmap);
                showStatus(selectedCustomBitmap);
            } else {
                state.grid().placeRail(x, y, selectedRail);
                showStatus(selectedRail.cName());
            }
            repaint();
        }

        private Train trainAt(Pos pos) {
            for (Train train : state.trains()) {
                for (TrainPart part : train.parts()) {
                    if (part.pos().equals(pos)) {
                        return train;
                    }
                }
            }
            return null;
        }

        private void save() {
            try {
                MapFileWriter.write(state, savePath);
                showStatus("Saved " + savePath);
            } catch (IOException ex) {
                showStatus("Cannot save");
            }
        }

        private void showStatus(String text) {
            message.setText("  " + text);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintRails(g);
            paintTrains(g);
        }

        private void paintRails(Graphics g) {
            for (int x = 0; x < state.grid().width(); x++) {
                for (int y = 0; y < state.grid().height(); y++) {
                    RailCell cell = state.grid().get(x, y);
                    int px = x * RAIL_WIDTH;
                    int py = y * RAIL_WIDTH;
                    if (!cell.isEmpty()) {
                        if (cell.customBitmap() != null) {
                            drawBitmap(g, cell.customBitmap(), px, py);
                        } else {
                            Image tile = railTile(cell);
                            if (tile != null) {
                                g.drawImage(tile, px, py, null);
                            } else {
                                drawRailFallback(g, cell.primaryType(), px, py);
                                if (cell.isComposite()) {
                                    drawRailFallback(g, cell.secondaryType(), px, py);
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Composes the cell tile like the C DrawRail(): the primary rail is
         * copied, then the secondary rail, switch indicator, and sensor
         * bitmaps are XORed on top.
         */
        private Image railTile(RailCell cell) {
            Direction switchEntry = cell.isSwitch() ? DirectionUtils.commonEntry(cell) : Direction.NONE;
            TileKey key = new TileKey(cell.primaryType(), cell.secondaryType(), switchEntry,
                    cell.switchState(), cell.sensor() == SensorState.SENSOR);
            return tileCache.computeIfAbsent(key, RailPanel::composeTile);
        }

        private static Image composeTile(TileKey key) {
            BitmapSet.Bitmap primary = BitmapSet.bits(key.primary().cName());
            if (primary == null) {
                return null;
            }
            BitmapSet.Bitmap tile = primary.copy();
            if (key.secondary() != RailType.NO_RAIL) {
                xorBitmap(tile, key.secondary().cName());
            }
            if (key.switchEntry() != Direction.NONE) {
                xorBitmap(tile, key.switchState().cName() + directionName(key.switchEntry()));
            }
            if (key.sensor()) {
                xorBitmap(tile, sensorBitmap(key.primary()));
            }
            return tile.toImage(Color.BLACK, new Color(0, 0, 0, 0));
        }

        private static void xorBitmap(BitmapSet.Bitmap tile, String name) {
            BitmapSet.Bitmap overlay = BitmapSet.bits(name);
            if (overlay != null) {
                tile.xor(overlay);
            }
        }

        private static String directionName(Direction direction) {
            return switch (direction) {
                case UP -> "Up";
                case RIGHT -> "Right";
                case DOWN -> "Down";
                case LEFT -> "Left";
                case NONE -> "";
            };
        }

        private void drawRailFallback(Graphics g, RailType type, int px, int py) {
            g.setColor(type.isStop() ? new Color(170, 50, 50) : new Color(45, 70, 80));
            int midX = px + RAIL_WIDTH / 2;
            int midY = py + RAIL_WIDTH / 2;
            int left = px + 4;
            int right = px + RAIL_WIDTH - 4;
            int top = py + 4;
            int bottom = py + RAIL_WIDTH - 4;
            switch (type) {
                case HORIZONTAL, LEFT_STOP, RIGHT_STOP -> g.drawLine(left, midY, right, midY);
                case VERTICAL, UP_STOP, DOWN_STOP -> g.drawLine(midX, top, midX, bottom);
                case UP_LEFT -> { g.drawLine(left, midY, midX, midY); g.drawLine(midX, midY, midX, top); }
                case UP_RIGHT -> { g.drawLine(right, midY, midX, midY); g.drawLine(midX, midY, midX, top); }
                case DOWN_LEFT -> { g.drawLine(left, midY, midX, midY); g.drawLine(midX, midY, midX, bottom); }
                case DOWN_RIGHT -> { g.drawLine(right, midY, midX, midY); g.drawLine(midX, midY, midX, bottom); }
                case NO_RAIL -> { }
            }
        }

        private static String sensorBitmap(RailType type) {
            return switch (type) {
                case HORIZONTAL, LEFT_STOP, RIGHT_STOP -> "HorizontalSensor";
                case VERTICAL, UP_STOP, DOWN_STOP -> "VerticalSensor";
                case UP_LEFT -> "UpLeftSensor";
                case UP_RIGHT -> "UpRightSensor";
                case DOWN_LEFT -> "DownLeftSensor";
                case DOWN_RIGHT -> "DownRightSensor";
                case NO_RAIL -> "HorizontalSensor";
            };
        }

        private void paintTrains(Graphics g) {
            for (Train train : state.trains()) {
                for (TrainPart part : train.parts()) {
                    CollisionDetector.PixelPos pixel = CollisionDetector.pixelPosition(part, train.offset(), RAIL_WIDTH);
                    int px = pixel.x() + TRAIN_DRAW_ADJUST;
                    int py = pixel.y() - TRAIN_DRAW_ADJUST;
                    Image sprite = BitmapSet.sprite(trainBitmap(part));
                    if (sprite != null) {
                        g.drawImage(sprite, px, py, null);
                        continue;
                    }
                    g.setColor(part.type() == TrainPart.Type.ENGINE ? new Color(30, 90, 180) : new Color(40, 140, 90));
                    g.fillOval(px, py, RAIL_WIDTH - 16, RAIL_WIDTH - 16);
                    g.setColor(Color.WHITE);
                    g.drawString(part.direction().name().substring(0, 1), px + 9, py + 15);
                }
            }
        }

        private String trainBitmap(TrainPart part) {
            if (part.type() == TrainPart.Type.CAR) {
                return switch (part.direction()) {
                    case UP, DOWN -> "VerticalCar";
                    case RIGHT, LEFT, NONE -> "HorizontalCar";
                };
            }
            String direction = switch (part.direction()) {
                case UP -> "Up";
                case RIGHT -> "Right";
                case DOWN -> "Down";
                case LEFT, NONE -> "Left";
            };
            return direction + "Engine";
        }

        private boolean drawBitmap(Graphics g, String name, int px, int py) {
            Image image = BitmapSet.image(name);
            if (image == null) {
                return false;
            }
            g.drawImage(image, px, py, null);
            return true;
        }
    }

    private enum Tool {
        RAIL,
        CUSTOM,
        TRAIN,
        DELETE,
        SWITCH,
        SPEED,
        SENSOR
    }

}
