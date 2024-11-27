package dev.bauhd.velocityupdater;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VelocityUpdater {

  public static final Gson GSON = new Gson();

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Found latest versions:");
    final var latestVersions = PistonMeta.latestVersions();
    for (final var version : latestVersions) {
      System.out.println("- " + version.id() + " (" + version.type() + ")");
    }
    final var oldVersion = latestVersions[0];
    final var newVersion = latestVersions[1];

    final var directory = Path.of("mache");

    cloneMacheAndApplyPatches(directory, oldVersion, newVersion);
    createDiff(directory, oldVersion, newVersion);
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
    execute(directoryFile, "./gradlew.bat", "applyPatches");
    execute(directoryFile, "git", "checkout", newVersion.branchName());
    execute(directoryFile, "./gradlew.bat", "applyPatches");
  }

  private static void createDiff(
      final Path directoryPath,
      final MinecraftVersion oldVersion,
      final MinecraftVersion newVersion
  ) throws IOException, InterruptedException {
    final var diffFolder = Path.of("diff");
    final var diffFolderFile = diffFolder.toFile();
    if (Files.notExists(diffFolder)) {
      Files.createDirectory(diffFolder);
    }
    final var versionsFolder = directoryPath.resolve("versions");
    execute(diffFolderFile, "git", "init");

    final var target = diffFolder.resolve("net");
    PathUtil.copyFileTree(versionsFolder.resolve(oldVersion.id())
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

    PathUtil.copyFileTree(versionsFolder.resolve(newVersion.id())
        .resolve("src").resolve("main").resolve("java").resolve("net"), target);

    execute(diffFolderFile, "git", "add", "*");
    execute(diffFolderFile, "git", "commit", "-m", "2");

    execute(diffFolderFile, "cmd", "/C", "git", "diff", "--patch", commitHash, "HEAD", ">",
        "patch");
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
