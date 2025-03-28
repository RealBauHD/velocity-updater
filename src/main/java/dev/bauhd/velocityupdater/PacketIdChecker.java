package dev.bauhd.velocityupdater;

import static dev.bauhd.velocityupdater.VelocityUpdater.GSON;
import static dev.bauhd.velocityupdater.VelocityUpdater.execute;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;

public final class PacketIdChecker {

  public PacketIdChecker(final MinecraftVersion[] versions)
      throws IOException, InterruptedException {
    JsonObject mappings;
    try (final var input = this.getClass().getClassLoader()
        .getResourceAsStream("type_to_class_mappings.json")) {
      mappings = GSON.fromJson(new InputStreamReader(Objects.requireNonNull(input)),
          JsonObject.class);
    }

    final var map = new HashMap<String, Integer>();
    for (final var version : versions) {
      final Path serverJar = version.path()
          .resolve(".gradle")
          .resolve("mache")
          .resolve("input");

      execute(serverJar.toFile(),
          "java", "-DbundlerMainClass=net.minecraft.data.Main",
          "-jar", "download_input.jar",
          "--reports"
      );
      final var packetJson = serverJar.resolve("generated").resolve("reports")
          .resolve("packets.json");
      try (final var reader = Files.newBufferedReader(packetJson)) {
        // what am I doing here
        final var json = GSON.fromJson(reader, JsonObject.class);
        for (final var phases : json.entrySet()) {
          for (final var bound : phases.getValue().getAsJsonObject().entrySet()) {
            for (final var packets : bound.getValue().getAsJsonObject().entrySet()) {
              final var packetId = packets.getKey()
                  .substring(packets.getKey().indexOf(':') + 1);
              final var key = phases.getKey() + '/' + bound.getKey() + '/' + packetId;
              final var id = packets.getValue().getAsJsonObject().get("protocol_id").getAsInt();
              final var prev = map.put(key, id);
              final var phase = mappings.get(phases.getKey())
                  .getAsJsonObject().get(bound.getKey()).getAsJsonObject();
              final var clazz = phase.get(packetId);
              if (clazz == null) {
                continue;
              }
              if (prev != null && prev != id) {
                System.out.println("Changed id " + clazz.getAsString() + " - " +
                    Integer.toHexString(prev) + " -> " + Integer.toHexString(id) +
                    " (" + phases.getKey() + ')');
              }
            }
          }
        }
      }
    }
  }
}
