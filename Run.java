import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Compiles the project and starts the bundled graphical simulator. */
public final class Run {
  private static final int DEFAULT_TRAIN_1_SPEED = 5;
  private static final int DEFAULT_TRAIN_2_SPEED = 10;
  private static final int MAX_TRAIN_SPEED = 17;
  private static final int SIMULATOR_DELAY_MS = 20;

  private Run() {
  }

  public static void main(String[] args) throws Exception {
    boolean compileOnly = args.length == 1 && args[0].equals("--compile-only");
    int train1Speed = DEFAULT_TRAIN_1_SPEED;
    int train2Speed = DEFAULT_TRAIN_2_SPEED;

    if (!compileOnly && args.length != 0) {
      if (args.length != 2) {
        printUsage();
        System.exit(1);
      }
      try {
        train1Speed = parseSpeed(args[0]);
        train2Speed = parseSpeed(args[1]);
      } catch (IllegalArgumentException exception) {
        System.err.println(exception.getMessage());
        printUsage();
        System.exit(1);
      }
    }

    Path project = Path.of("").toAbsolutePath().normalize();
    Path sourceDirectory = project.resolve("src");
    Path outputDirectory = project.resolve("bin");

    if (!Files.isDirectory(sourceDirectory)
        || !Files.isRegularFile(project.resolve("Lab1.map"))) {
      System.err.println("Run this command from the TrainSim project folder: java Run.java");
      System.exit(1);
    }

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      System.err.println("Java JDK 17 or newer is required. Install a JDK and try again.");
      System.exit(1);
    }

    List<Path> sources;
    try (var paths = Files.walk(sourceDirectory)) {
      sources = paths
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }

    Files.createDirectories(outputDirectory);
    try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
      var compilationUnits = files.getJavaFileObjectsFromPaths(sources);
      boolean compiled = compiler.getTask(
          null,
          files,
          null,
          List.of("-d", outputDirectory.toString()),
          null,
          compilationUnits).call();
      if (!compiled) {
        System.exit(1);
      }
    }

    copyDirectory(
        sourceDirectory.resolve("TSim/ui/bitmaps"),
        outputDirectory.resolve("tsim/ui/bitmaps"));

    if (compileOnly) {
      return;
    }

    boolean windows = System.getProperty("os.name")
        .toLowerCase(Locale.ROOT)
        .contains("win");
    Path javaExecutable = Path.of(
        System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");

    System.out.printf(
        "Starting TrainSim with train speeds %d and %d.%n",
        train1Speed,
        train2Speed);
    ProcessBuilder processBuilder = new ProcessBuilder(
        javaExecutable.toString(),
        "-cp",
        outputDirectory.toString(),
        "Main",
        "Lab1.map",
        Integer.toString(train1Speed),
        Integer.toString(train2Speed),
        Integer.toString(SIMULATOR_DELAY_MS));
    processBuilder.environment().put("TSIM_JAVA", "true");
    Process simulator = processBuilder
        .directory(project.toFile())
        .inheritIO()
        .start();
    System.exit(simulator.waitFor());
  }

  private static int parseSpeed(String value) {
    try {
      int speed = Integer.parseInt(value);
      if (speed < 0 || speed > MAX_TRAIN_SPEED) {
        throw new IllegalArgumentException();
      }
      return speed;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Train speeds must be whole numbers from 0 to 17.");
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Train speeds must be between 0 and 17.");
    }
  }

  private static void printUsage() {
    System.err.println("Usage: java Run.java [train-1-speed train-2-speed]");
    System.err.println("Example: java Run.java 17 10");
  }

  private static void copyDirectory(Path source, Path target) throws IOException {
    try (var paths = Files.walk(source)) {
      for (Path path : (Iterable<Path>) paths::iterator) {
        Path destination = target.resolve(source.relativize(path));
        if (Files.isDirectory(path)) {
          Files.createDirectories(destination);
        } else {
          Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }
}
