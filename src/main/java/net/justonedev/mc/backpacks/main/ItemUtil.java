package net.justonedev.mc.backpacks.main;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public enum ItemUtil {
    ;

    //region General Item Utility | Custom Models

    public static boolean hasCustomModelData(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        if (BackpacksMain.USE_NEW_TEXTURE_PACK_SYSTEM) {
            NamespacedKey modelKey = meta.getItemModel();
            return modelKey != null;
        }
        return hasCustomModelDataLegacy(itemStack);
    }

    private static boolean hasCustomModelDataLegacy(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        try {
            if (!meta.hasCustomModelData()) return false;
            int data = meta.getCustomModelData();
            return data != 0;
        } catch (Exception e) {
            // in case the method is not on the server side
            return false;
        }
    }

    //endregion

}
