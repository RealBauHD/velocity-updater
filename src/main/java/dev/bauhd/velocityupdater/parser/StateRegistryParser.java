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
      final var versionConstant = version.toConstant();
      compilationUnit.addImport(new ImportDeclaration(
          "com.velocitypowered.api.network.ProtocolVersion." + versionConstant, true, false));

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
                final var arguments = methodCall.getArguments();
                for (final var packetEntry : entry.getValue().entrySet()) {
                  if (methodCall.getNameAsString().equals("register") && arguments.get(0)
                      .toString().equals(packetEntry.getKey() + ".class")) {
                    final var id = this.toHex(packetEntry.getValue());
                    final var encodeOnly = arguments.get(arguments.size() - 1)
                        .asMethodCallExpr().getArgument(2);
                    methodCall.addArgument(new MethodCallExpr("map",
                        new IntegerLiteralExpr(id),
                        new NameExpr(versionConstant),
                        encodeOnly));
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
