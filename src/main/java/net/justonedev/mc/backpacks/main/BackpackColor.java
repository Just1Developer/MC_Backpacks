package net.justonedev.mc.backpacks.main;

import org.bukkit.Material;

import java.util.Optional;

public enum BackpackColor {
    DEFAULT(Material.WATER_BUCKET, "backpack", 0),
    BLACK(Material.BLACK_DYE, "backpack_black", 1),
    BLUE(Material.BLUE_DYE, "backpack_blue", 2),
    BROWN(Material.BROWN_DYE, "backpack_brown", 3),
    CYAN(Material.CYAN_DYE, "backpack_cyan", 4),
    GRAY(Material.GRAY_DYE, "backpack_gray", 5),
    GREEN(Material.GREEN_DYE, "backpack_green", 6),
    LIGHT_BLUE(Material.LIGHT_BLUE_DYE, "backpack_light_blue", 7),
    LIGHT_GRAY(Material.LIGHT_GRAY_DYE, "backpack_light_gray", 8),
    LIME(Material.LIME_DYE, "backpack_lime", 9),
    MAGENTA(Material.MAGENTA_DYE, "backpack_magenta", 10),
    ORANGE(Material.ORANGE_DYE, "backpack_orange", 11),
    PINK(Material.PINK_DYE, "backpack_pink", 12),
    PURPLE(Material.PURPLE_DYE, "backpack_purple", 13),
    RED(Material.RED_DYE, "backpack_red", 14),
    YELLOW(Material.YELLOW_DYE, "backpack_yellow", 15),
    WHITE(Material.WHITE_DYE, "backpack_white", 16),
    ENDER(Material.VOID_AIR, "ender_backpack", 0),
    ;

    private final Material craftingMaterial;
    private final String modelName;
    private final int legacyModelDataOffset;

    BackpackColor(Material craftingMaterial, String modelName, int legacyModelDataOffset) {
        this.craftingMaterial = craftingMaterial;
        this.modelName = modelName;
        this.legacyModelDataOffset = legacyModelDataOffset;
    }

    public Material getCraftingMaterial() {
        return craftingMaterial;
    }

    public String getModelName() {
        return modelName;
    }

    public int getLegacyModelDataOffset() {
        return legacyModelDataOffset;
    }

    public boolean isColor() {
        return this != DEFAULT && this != ENDER;
    }

    public boolean isCraftableByColoring() {
        return this != ENDER;
    }

    public static Optional<BackpackColor> getByModelName(String modelName) {
        for (var color : values()) {
            if (color.getModelName().equals(modelName)) {
                return Optional.of(color);
            }
        }
        return Optional.empty();
    }

    public static Optional<BackpackColor> getByCraftingMaterial(Material craftingMaterial) {
        for (var color : values()) {
            if (color.getCraftingMaterial().equals(craftingMaterial)) {
                return Optional.of(color);
            }
        }
        return Optional.empty();
    }

    public static BackpackColor fromCustomModelDataInteger(int customModelData) {
        return values()[customModelData % values().length];
    }
}
