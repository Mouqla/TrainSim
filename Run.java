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
  private Run() {
  }

  public static void main(String[] args) throws Exception {
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

    if (args.length > 0 && args[0].equals("--compile-only")) {
      return;
    }

    boolean windows = System.getProperty("os.name")
        .toLowerCase(Locale.ROOT)
        .contains("win");
    Path javaExecutable = Path.of(
        System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");

    Process simulator = new ProcessBuilder(
        javaExecutable.toString(), "-cp", outputDirectory.toString(), "JavaMain")
        .directory(project.toFile())
        .inheritIO()
        .start();
    System.exit(simulator.waitFor());
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
