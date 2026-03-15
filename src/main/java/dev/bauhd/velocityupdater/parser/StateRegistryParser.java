package dev.bauhd.velocityupdater.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import dev.bauhd.velocityupdater.MinecraftVersion;
import dev.bauhd.velocityupdater.checker.PacketIdChecker;
import java.util.Locale;

public final class StateRegistryParser extends Parser {

  public StateRegistryParser() {
    super("proxy/src/main/java/com/velocitypowered/proxy/protocol/StateRegistry.java");
  }

  // TODO: linebreak and copy encode only
  @Override
  public boolean parse(CompilationUnit compilationUnit, MinecraftVersion version) {
    final var stateRegistryEnum = compilationUnit.getEnumByName("StateRegistry");
    if (stateRegistryEnum.isPresent()) {
      final var versionConstant = version.toConstant();
      final var declaration = stateRegistryEnum.get();
      for (final var enumEntry : declaration.getEntries()) {
        String name = enumEntry.getNameAsString().toLowerCase(Locale.ROOT);
        if (name.equals("config")) {
          name = "configuration";
        }
        final var ids = PacketIdChecker.PACKET_IDS.get(name);
        if (ids != null) {
          final var classBody = enumEntry.getClassBody().getFirst();
          classBody.ifPresent(bodyDeclaration -> {
            final var initializerDeclaration = bodyDeclaration.asInitializerDeclaration();
            for (final var methodCall : initializerDeclaration.findAll(MethodCallExpr.class)) {
              for (final var entry : ids.entrySet()) {
                final var scope = methodCall.getScope();
                if (scope.isPresent() && !scope.get().toString().equals(entry.getKey())) {
                  continue;
                }
                for (final var packetEntry : entry.getValue().entrySet()) {
                  if (methodCall.getNameAsString().equals("register") && methodCall.getArgument(0)
                      .toString().equals(packetEntry.getKey() + ".class")) {
                    methodCall.addArgument(
                        "map(" + this.toHex(packetEntry.getValue()) + ", " + versionConstant
                            + ", false)");
                  }
                }
              }
            }
          });
        }
      }
      return true;
    }
    return false;
  }

  private String toHex(final int i) {
    var id = Integer.toHexString(i).toUpperCase(Locale.ROOT);
    if (id.length() == 1) {
      id = "0" + id;
    }
    return "0x" + id;
  }
}
