package dev.bauhd.velocityupdater.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import dev.bauhd.velocityupdater.Extractor;
import dev.bauhd.velocityupdater.MinecraftVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public final class ProtocolVersionParser extends Parser {

  public ProtocolVersionParser() {
    super("api/src/main/java/com/velocitypowered/api/network/ProtocolVersion.java");
  }

  @Override
  public boolean parse(final CompilationUnit compilationUnit, final MinecraftVersion version) {
    final List<String> lines;
    try {
      lines = Files.readAllLines(version.path()
          .resolve("src", "main", "java", "net", "minecraft", "SharedConstants.java"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    final var versionEnum = compilationUnit.getEnumByName("ProtocolVersion");
    if (versionEnum.isPresent()) {
      final var versionConstant = version.toConstant();
      final var declaration = versionEnum.get();
      EnumConstantDeclaration enumConstant = null;
      for (final var entry : declaration.getEntries()) {
        if (entry.getNameAsString().equals(versionConstant)) {
          enumConstant = entry;
          break;
        }
      }
      if (enumConstant == null) {
        enumConstant = declaration.addEnumConstant(versionConstant);
      } else {
        enumConstant.getArguments().clear();
      }
      if (version.type().equals("snapshot")) {
        enumConstant.addArgument(new IntegerLiteralExpr("-1"));
        enumConstant.addArgument(Extractor.constant(lines, "SNAPSHOT_NETWORK_PROTOCOL_VERSION"));
      } else {
        enumConstant.addArgument(Extractor.constant(lines, "RELEASE_NETWORK_PROTOCOL_VERSION"));
      }
      enumConstant.addArgument(new StringLiteralExpr(version.id()));
      return true;
    }
    return false;
  }
}
