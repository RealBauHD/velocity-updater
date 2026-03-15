package dev.bauhd.velocityupdater;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import dev.bauhd.velocityupdater.parser.Parser;
import dev.bauhd.velocityupdater.parser.ProtocolVersionParser;
import dev.bauhd.velocityupdater.parser.StateRegistryParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UpdateParser {

  public UpdateParser(final MinecraftVersion version) throws IOException {
    final var velocityPath = Path.of("Velocity");
    if (Files.notExists(velocityPath)) {
      return;
    }

    final var javaParser = new JavaParser(new ParserConfiguration()
        .setLanguageLevel(LanguageLevel.JAVA_21));

    final var parsers = new Parser[]{
        new ProtocolVersionParser(),
        new StateRegistryParser()
    };

    for (final var parser : parsers) {
      final var path = velocityPath.resolve(parser.path());
      if (Files.notExists(path)) {
        continue;
      }

      final var compilationUnit = javaParser.parse(path).getResult().orElseThrow();
      LexicalPreservingPrinter.setup(compilationUnit);

      if (parser.parse(compilationUnit, version)) {
        try {
          Files.writeString(path, LexicalPreservingPrinter.print(compilationUnit));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
