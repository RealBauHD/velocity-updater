package dev.bauhd.velocityupdater;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VelocityUpdater {

  public static final Gson GSON = new Gson();
  private static final Path PATH = Path.of("versions");

  public static void main(String[] args) throws IOException {
    if (Files.notExists(PATH)) {
      Files.createDirectory(PATH);
    }

    System.out.println("Found latest versions:");
    final var latestVersions = PistonMeta.latestVersions();
    for (final var version : latestVersions) {
      System.out.println("- " + version.id() + " (" + version.type() + ")");
    }

    for (final var version : latestVersions) {
      PistonMeta.downloadVersion(version, PATH);
    }
  }
}
