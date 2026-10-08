import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import TSim.TSimInterface;
import tsim.ui.TSimApp;

public class Main {
    private static final String TSIM_PATH_ON_LAB_COMPUTERS = "/chalmers/groups/tda384/tsim-0.84/out/bin/tsim";
    private static final String DEFAULT_MAP = "Lab1.map";
    private static final int DEFAULT_TRAIN_1_SPEED = 5;
    private static final int DEFAULT_TRAIN_2_SPEED = 10;
    private static final int DEFAULT_JAVA_TSIM_SPEED = 1;

    /**
     * The main method expects 3-4 arguments, e.g.:
     * - command line: java -cp bin Main "Lab1.map" 5 10 20
     * - in Eclipse: add them from Run Configurations -> Arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 3 && args.length != 4) {
            System.err.println("Main method expects 3-4 arguments: Lab1.map <Train1Speed> <Train2Speed> [SimulatorSpeed]");
            System.exit(1);
        }

        String map = args[0];
        int train1_speed = Integer.parseInt(args[1]);
        int train2_speed = Integer.parseInt(args[2]);
        int tsim_speed = (args.length >= 4) ? Integer.parseInt(args[3]) : 20;

        run(map, train1_speed, train2_speed, tsim_speed, useJavaTsim());
    }

    public static void javaMain() throws IOException, InterruptedException {
        run(DEFAULT_MAP, DEFAULT_TRAIN_1_SPEED, DEFAULT_TRAIN_2_SPEED, DEFAULT_JAVA_TSIM_SPEED, true);
    }

    private static void run(
            String map,
            int train1Speed,
            int train2Speed,
            int tsimSpeed,
            boolean useJavaTsim) throws IOException, InterruptedException {
        Process process = null;
        if (useJavaTsim) {
            TSimInterface.initEmbedded(Path.of(map), tsimSpeed);
        } else {
            process = startInstalledTsim(map, tsimSpeed);
            TSimInterface.init(process.getInputStream(), process.getOutputStream());
        }

        TSimInterface.getInstance().setDebug(true);
        new Lab1(train1Speed, train2Speed);
        // new Lab1Extra(train1_speed, train2_speed);
        if (process != null) {
            process.waitFor();
        }
    }

    private static Process startInstalledTsim(String map, int tsimSpeed) throws IOException {
        String tsim;
        if (Files.exists(Paths.get(TSIM_PATH_ON_LAB_COMPUTERS))) {
            tsim = TSIM_PATH_ON_LAB_COMPUTERS;
        } else {
            // Otherwise tsim must be in your $PATH
            tsim = "tsim";
        }

        return new ProcessBuilder(tsim, "--speed=" + tsimSpeed, map).start();
    }

    private static boolean useJavaTsim() {
        String value = System.getenv("TSIM_JAVA");
        return value != null && switch (value.toLowerCase()) {
            case "1", "true", "yes" -> true;
            default -> false;
        };
    }
}

class JavaMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        Main.javaMain();
    }
}

class StandaloneTsimMain {
    public static void main(String[] args) {
        if (args.length == 0) {
            TSimApp.startStandalone(Path.of("Lab1.map"), 1);
        } else {
            TSimApp.main(args);
        }
    }
}
