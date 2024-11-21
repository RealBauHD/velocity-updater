package dev.bauhd.velocityupdater;

import com.google.gson.JsonObject;

public final class MinecraftVersion {

    private final String id;
    private final String type;
    private JsonObject specifications;

    public MinecraftVersion(final String id, final String type) {
        this.id = id;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public String type() {
        return type;
    }

    public JsonObject specifications() {
        return specifications;
    }

    void resolve(final JsonObject specifications) {
        this.specifications = specifications;
    }
}
