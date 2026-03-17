package dev.bauhd.velocityupdater;

import com.google.gson.Gson;
import dev.bauhd.velocityupdater.checker.CommandArgumentChecker;
import dev.bauhd.velocityupdater.checker.PacketIdChecker;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class VelocityUpdater {

  public static final Gson GSON = new Gson();

  static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Found latest versions:");
    final var latestVersions = new MinecraftVersion[]{
        new MinecraftVersion(args[0], args[1]),
        new MinecraftVersion(args[2], args[3])
    };
    for (final var version : latestVersions) {
      System.out.println("- " + version.id() + " (" + version.type() + ")");
    }
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
    createDiff(oldVersion, newVersion, macheDirectory);
    createReports(latestVersions);

    new CommandArgumentChecker(latestVersions, outputDirectory);
    new PacketIdChecker(latestVersions, outputDirectory);

    new UpdateParser(newVersion);
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
        final var path = minecraftDirectory.resolve(unusefulThing);
        if (Files.exists(path)) {
          PathUtil.deleteDirectory(path);
        }
      }
    }

    System.out.println("Deleted unuseful things");
  }

  private static void createDiff(
      final MinecraftVersion oldVersion,
      final MinecraftVersion newVersion,
      final Path macheDirectory
  ) throws IOException, InterruptedException {
    System.out.println("Diff changes");
    execute(macheDirectory.resolve("versions").toFile(), "cmd", "/C", "git", "diff", "--no-index",
        oldVersion.id() + "/src/main/java/net",
        newVersion.id() + "/src/main/java/net",
        ">", "../../output/diff.patch");
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
