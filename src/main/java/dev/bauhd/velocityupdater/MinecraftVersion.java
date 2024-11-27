package dev.bauhd.velocityupdater;

public record MinecraftVersion(
    String id,
    String type
) {

    public String branchName() {
        return this.type + '/' + this.id;
    }
}
