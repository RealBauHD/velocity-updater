package dev.bauhd.velocityupdater;

import java.nio.file.Path;

public final class MinecraftVersion {

  private final String id;
  private final String type;
  private Path path;

  public MinecraftVersion(final String id, final String type) {
    this.id = id;
    this.type = type;
  }

  public String id() {
    return this.id;
  }

  public String type() {
    return this.type;
  }

  public String branchName() {
    return this.type + '/' + this.id;
  }

  public void setPath(final Path path) {
    this.path = path;
  }

  public Path path() {
    return this.path;
  }
}
