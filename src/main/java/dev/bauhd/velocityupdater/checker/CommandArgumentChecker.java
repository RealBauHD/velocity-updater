package dev.bauhd.velocityupdater.checker;

import static dev.bauhd.velocityupdater.VelocityUpdater.GSON;

import com.google.gson.JsonObject;
import dev.bauhd.velocityupdater.MinecraftVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public final class CommandArgumentChecker {

  public CommandArgumentChecker(final MinecraftVersion[] versions, final Path outputDirectory)
      throws IOException {
    final var map = new HashMap<String, Integer>();
    for (final var version : versions) {
      final var registries = version.reports().resolve("registries.json");
      try (final var reader = Files.newBufferedReader(registries)) {
        final var json = GSON.fromJson(reader, JsonObject.class);
        final var changes = new StringBuilder();
        for (final var entry : json
            .getAsJsonObject("minecraft:command_argument_type")
            .getAsJsonObject("entries")
            .entrySet()) {
          final String key = entry.getKey();
          final JsonObject value = entry.getValue().getAsJsonObject();
          final var id = value.get("protocol_id").getAsInt();
          final var prev = map.put(key, id);
          if (prev != null && prev != id) {
            changes.append("Changed id ").append(key).append(" - ")
                .append(Integer.toHexString(prev)).append(" -> ")
                .append(Integer.toHexString(id))
                .append('\n');
          } else if (prev == null) {
            changes.append("Added ").append(key).append('\n');
          }
        }
        Files.writeString(outputDirectory.resolve("command_arguments"), changes.toString());
      }
    }
  }
}
