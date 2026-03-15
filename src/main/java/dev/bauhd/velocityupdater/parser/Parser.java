package dev.bauhd.velocityupdater.parser;

import com.github.javaparser.ast.CompilationUnit;
import dev.bauhd.velocityupdater.MinecraftVersion;

public abstract class Parser {

  protected final String path;

  public Parser(final String path) {
    this.path = path;
  }

  public abstract boolean parse(final CompilationUnit compilationUnit, final MinecraftVersion version);

  public String path() {
    return this.path;
  }
}
