package dev.bauhd.velocityupdater;

import java.util.List;
import java.util.regex.Pattern;

public final class Extractor {

  public static String constant(final List<String> lines, final String name) {
    final var pattern = Pattern.compile("public static final .* " + name + " = (.*);");
    for (final var line : lines) {
      final var matcher = pattern.matcher(line);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    throw new AssertionError("constant " + name + " not found");
  }
}
