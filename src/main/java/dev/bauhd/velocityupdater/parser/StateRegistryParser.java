package dev.bauhd.velocityupdater.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import dev.bauhd.velocityupdater.MinecraftVersion;
import dev.bauhd.velocityupdater.checker.PacketIdChecker;
import java.util.Locale;

public final class StateRegistryParser extends Parser {

  public StateRegistryParser() {
    super("proxy/src/main/java/com/velocitypowered/proxy/protocol/StateRegistry.java");
  }

  // TODO: linebreak
  @Override
  public boolean parse(final CompilationUnit compilationUnit, final MinecraftVersion version) {
    final var stateRegistryEnum = compilationUnit.getEnumByName("StateRegistry");
    if (stateRegistryEnum.isPresent() && !PacketIdChecker.PACKET_IDS.isEmpty()) {
      return false;
    }
    final var versionConstant = version.toConstant();
    compilationUnit.addImport(new ImportDeclaration(
        "com.velocitypowered.api.network.ProtocolVersion." + versionConstant, true, false));

    for (final var enumEntry : stateRegistryEnum.get().getEntries()) {
      final var name = normalizeStateName(enumEntry.getNameAsString());
      final var ids = PacketIdChecker.PACKET_IDS.get(name);
      if (ids != null) {
        final var classBody = enumEntry.getClassBody().getFirst();
        classBody.ifPresent(bodyDeclaration -> {
          final var initializerDeclaration = bodyDeclaration.asInitializerDeclaration();
          for (final var methodCall : initializerDeclaration.findAll(MethodCallExpr.class)) {
            if (!methodCall.getNameAsString().equals("register")) {
              continue;
            }
            final var scope = methodCall.getScope();
            if (scope.isEmpty()) {
              continue;
            }
            final var scopedIds = ids.get(scope.get().toString());
            if (scopedIds == null) {
              continue;
            }
            final var arguments = methodCall.getArguments();
            final var packetId = scopedIds.get(arguments.get(0).toString());
            if (packetId == null) {
              continue;
            }
            final var encodeOnly = arguments.get(arguments.size() - 1)
                .asMethodCallExpr().getArgument(2);
            methodCall.addArgument(new MethodCallExpr("map",
                new IntegerLiteralExpr(toHex(packetId)),
                new NameExpr(versionConstant),
                encodeOnly));
          }
        });
      }
    }
    return true;
  }

  private static String normalizeStateName(final String name) {
    final var stateName = name.toLowerCase(Locale.ROOT);
    return stateName.equals("config") ? "configuration" : stateName;
  }

  private static String toHex(final int i) {
    var id = Integer.toHexString(i).toUpperCase(Locale.ROOT);
    return "0x" + (id.length() == 1 ? "0" + id : id);
  }
}
