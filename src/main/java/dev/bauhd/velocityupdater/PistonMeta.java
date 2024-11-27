package dev.bauhd.velocityupdater;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import static dev.bauhd.velocityupdater.VelocityUpdater.GSON;

public final class PistonMeta {

    private static final String PISTON_META_ENDPOINT = "https://piston-meta.mojang.com/";
    private static final String VERSION_MANIFEST = PISTON_META_ENDPOINT.concat(
            "mc/game/version_manifest_v2.json");

    public static MinecraftVersion[] latestVersions() {
        final var manifest = jsonFromUrl(VERSION_MANIFEST);
        final var latest = manifest.get("latest").getAsJsonObject();
        final var release = new MinecraftVersion(latest.get("release").getAsString(), "release");
        final var snapshot = new MinecraftVersion(latest.get("snapshot").getAsString(), "snapshot");
      return new MinecraftVersion[]{release, snapshot};
    }

    private static JsonObject jsonFromUrl(final String urlString) {
        try {
            final var url = URI.create(urlString).toURL();
            final var connection = url.openConnection();
            connection.connect();
            try (final var reader = new InputStreamReader(connection.getInputStream())) {
                return GSON.fromJson(reader, JsonObject.class);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
