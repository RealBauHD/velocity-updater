package dev.bauhd.velocityupdater;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
        final var versions = new MinecraftVersion[]{release, snapshot};
        final var manifestVersions = manifest.get("versions").getAsJsonArray();
        for (final var manifestVersion : manifestVersions) {
            final var object = manifestVersion.getAsJsonObject();
            for (final var version : versions) {
                if (version.id().equals(object.get("id").getAsString())) {
                    version.resolve(jsonFromUrl(object.get("url").getAsString()));
                }
            }
        }
        return versions;
    }

    public static void downloadVersion(final MinecraftVersion version, final Path path) {
        final var server = version.specifications().get("downloads").getAsJsonObject().get("server").getAsJsonObject();
        try {
            final var url = URI.create(server.get("url").getAsString()).toURL();
            final var connection = url.openConnection();
            connection.connect();
            System.out.println("Downloading " + url);
            Files.copy(connection.getInputStream(), path.resolve(version.id() + ".jar"), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied " + url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
