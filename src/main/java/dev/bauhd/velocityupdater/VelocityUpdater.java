package dev.bauhd.velocityupdater;

import com.google.gson.Gson;
import dev.bauhd.velocityupdater.checker.CommandArgumentChecker;
import dev.bauhd.velocityupdater.checker.PacketIdChecker;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class VelocityUpdater {

  public static final Gson GSON = new Gson();

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Found latest versions:");
    final var latestVersions = PistonMeta.latestVersions();
    for (final var version : latestVersions) {
      System.out.println("- " + version.id() + " (" + version.type() + ")");
    }
    latestVersions[0] = new MinecraftVersion("1.21.5", "release");
    final var oldVersion = latestVersions[0];
    final var newVersion = latestVersions[1];

    final var macheDirectory = Path.of("mache");
    final var versionsDirectory = macheDirectory.resolve("versions");

    for (final var version : latestVersions) {
      version.setPath(versionsDirectory.resolve(version.id()));
    }

    final var outputDirectory = Path.of("output");
    if (Files.exists(outputDirectory)) {
      System.err.println("Please delete the old output directory!");
      return;
    } else {
      Files.createDirectory(outputDirectory);
    }

    cloneMacheAndApplyPatches(macheDirectory, oldVersion, newVersion);
    deleteUnusefulThings(latestVersions);
    createDiff(oldVersion, newVersion, outputDirectory);
    createReports(latestVersions);

    new CommandArgumentChecker(latestVersions, outputDirectory);
    new PacketIdChecker(latestVersions, outputDirectory);
  }

  private static void cloneMacheAndApplyPatches(
      final Path directory,
      final MinecraftVersion oldVersion,
      final MinecraftVersion newVersion
  ) throws IOException, InterruptedException {
    final var directoryFile = directory.toFile();
    if (Files.notExists(directory)) {
      execute(null, "git", "clone", "https://github.com/PaperMC/mache.git",
          "-b", oldVersion.branchName());
    } else {
      execute(directoryFile, "git", "checkout", oldVersion.branchName());
    }
    execute(directoryFile, "cmd", "/C", "gradlew", "applyPatches");
    execute(directoryFile, "git", "checkout", newVersion.branchName());
    execute(directoryFile, "cmd", "/C", "gradlew", "applyPatches");
  }

  private static void deleteUnusefulThings(final MinecraftVersion[] versions) throws IOException {
    final var unusefulThings = List.of(
        "advancements",
        "commands",
        "data",
        "gametest",
        "locale",
        "obfuscate",
        "recipebook",
        "sounds",
        "stats",
        "tags",
        "world"
    );

    for (final var version : versions) {
      final var minecraftDirectory = version.path()
          .resolve("src").resolve("main").resolve("java")
          .resolve("net").resolve("minecraft");
      for (final var unusefulThing : unusefulThings) {
        PathUtil.deleteDirectory(minecraftDirectory.resolve(unusefulThing));
      }
    }

    System.out.println("Deleted unuseful things");
  }

  private static void createDiff(
      final MinecraftVersion oldVersion,
      final MinecraftVersion newVersion,
      final Path outputDirectory
  ) throws IOException, InterruptedException {
    final var diffFolder = outputDirectory.resolve("diff");
    final var diffFolderFile = diffFolder.toFile();
    if (Files.notExists(diffFolder)) {
      Files.createDirectory(diffFolder);
    }
    execute(diffFolderFile, "git", "init");

    final var target = diffFolder.resolve("net");
    PathUtil.copyFileTree(oldVersion.path()
        .resolve("src").resolve("main").resolve("java").resolve("net"), target);

    execute(diffFolderFile, "git", "add", "*");
    execute(diffFolderFile, "git", "commit", "-m", "1");

    PathUtil.deleteDirectory(target);

    final var process = new ProcessBuilder("git", "rev-parse", "HEAD")
        .directory(diffFolderFile)
        .start();
    String commitHash;
    try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      commitHash = reader.readLine();
    }

    PathUtil.copyFileTree(newVersion.path()
        .resolve("src").resolve("main").resolve("java").resolve("net"), target);

    execute(diffFolderFile, "git", "add", "*");
    execute(diffFolderFile, "git", "commit", "-m", "2");

    execute(diffFolderFile, "cmd", "/C", "git", "diff", "--patch", commitHash, "HEAD", ">",
        "../patch");
  }

  private static void createReports(final MinecraftVersion[] versions)
      throws IOException, InterruptedException {
    for (final var version : versions) {
      final Path serverJar = version.path()
          .resolve(".gradle")
          .resolve("mache")
          .resolve("input");

      execute(serverJar.toFile(),
          "java", "-DbundlerMainClass=net.minecraft.data.Main",
          "-jar", "download_input.jar",
          "--reports"
      );

      version.setReports(serverJar.resolve("generated").resolve("reports"));
    }
  }

  private static void execute(final File directory, final String... command)
      throws IOException, InterruptedException {
    new ProcessBuilder(command)
        .directory(directory)
        .inheritIO()
        .start()
        .waitFor();
  }
}
