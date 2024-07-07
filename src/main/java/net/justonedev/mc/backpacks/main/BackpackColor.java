package net.justonedev.mc.backpacks.main;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BackpackColor implements Listener {

	static final int baseColorModelData = 5000;
	private static final List<Material> sortedColors = Arrays.asList(
			Material.WATER_BUCKET,		// 0
			Material.BLACK_DYE,			// 1
			Material.BLUE_DYE,			// 2
			Material.BROWN_DYE,			// 3
			Material.CYAN_DYE,			// 4
			Material.GRAY_DYE,			// 5
			Material.GREEN_DYE,			// 6
			Material.LIGHT_BLUE_DYE,	// 7
			Material.LIGHT_GRAY_DYE,	// 8
			Material.LIME_DYE,			// 9
			Material.MAGENTA_DYE,		// 10
			Material.ORANGE_DYE,		// 11
			Material.PINK_DYE,			// 12
			Material.PURPLE_DYE,		// 13
			Material.RED_DYE,			// 14
			Material.YELLOW_DYE,		// 15
			Material.WHITE_DYE			// 16
	);
	
	// Players to receive a bucket, stores which slot
	private static final Map<UUID, Integer> bucketList = new HashMap<>();
	
	@EventHandler
	public void prepareCraftItem(PrepareItemCraftEvent e) {
		// Top to Bottom, Left to Right. Null if empty
		ItemStack[] items = e.getInventory().getMatrix();
		ItemStack backpack = null, color = null;
		for (ItemStack item : items) {
			if (item == null) continue;
			if (item.getAmount() > 1) return;
			if (Backpacks.isBackpack(item)) {
				if (backpack == null) backpack = item;
				else return;
			} else if (item != null && sortedColors.contains(item.getType())) {
				if (color == null) color = item;
				else return;
			}
		}
		if (backpack == null || color == null) return;
		
		ItemStack newBackpack = new ItemStack(backpack);
		ItemMeta meta = newBackpack.getItemMeta();
		assert meta != null;
		
		if (meta.getCustomModelData() == baseColorModelData + sortedColors.indexOf(color.getType())) return;	// Already this color
		
		meta.setCustomModelData(baseColorModelData + sortedColors.indexOf(color.getType()));
		newBackpack.setItemMeta(meta);
		
		e.getInventory().setResult(newBackpack);
	}
	
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player)) return;
		Player p = (Player) e.getWhoClicked();
		
		if (e.getClickedInventory() == null) return;
		InventoryType type = e.getClickedInventory().getType();
		if (type != InventoryType.CRAFTING && type != InventoryType.WORKBENCH) return;
		if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
		
		ItemStack itemStack = e.getCurrentItem();
		if (!Backpacks.isBackpack(itemStack)) return;
		
		ItemMeta meta = itemStack.getItemMeta();
		if (meta == null) return;
		
		// If it's a colored backpack, return.
		if (meta.hasCustomModelData()
				&& meta.getCustomModelData() > baseColorModelData
				&& meta.getCustomModelData() < baseColorModelData + sortedColors.size()) return;
		
		Inventory inventory = e.getClickedInventory();
		ItemStack[] items = inventory.getStorageContents();
		
		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (item == null) continue;
			if (item.getType() == Material.WATER_BUCKET) {
				bucketList.put(e.getWhoClicked().getUniqueId(), i);
				Bukkit.getScheduler().scheduleSyncDelayedTask(BackpacksMain.main, () -> {
					int slot = bucketList.getOrDefault(p.getUniqueId(), -500);
					if (slot >= 0) {
						bucketList.remove(p.getUniqueId());
						ItemStack bucket = new ItemStack(Material.BUCKET, 1);
						try {
							inventory.setItem(slot, bucket);
						} catch (Exception ignored) {
							// Anything went wrong with the inventory
							addItem(p, bucket);
						}
					}
				}, 1);
			}
			/*
			if (item.getType() == Material.WATER_BUCKET) {
				items[i] = new ItemStack(Material.BUCKET);
			} else if (sortedColors.contains(item.getType())) {
				item.setAmount(item.getAmount() - 1);
				if (item.getAmount() <= 0) items[i] = null;
			}
			 */
		}
		// If the Player is disconnected here, the event doesn't go through, so they lose their items.
		// Honestly this is not my problem I guess. If this happens for some reason, tough luck.
		
		// If the player's inventory is closed immediately, the crafting also doesn't go through. In this case, the water bucket
		// is still emptied, but that is a thing that'll just happen.
	}
	
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (!(e.getPlayer() instanceof Player)) return;
		
		if (e.getInventory().getType() != InventoryType.CRAFTING && e.getInventory().getType() != InventoryType.WORKBENCH) return;
		
		Player p = (Player) e.getPlayer();
		int slot = bucketList.getOrDefault(p.getUniqueId(), -500);
		if (slot >= 0) {
			bucketList.remove(p.getUniqueId());
			// This is the crafting table of: 0: Result, 1 - 9 (or 4): Matrix
			// See if player is receiving a water bucket
			ItemStack[] items = e.getInventory().getContents();
			for (ItemStack item : items) {
				if (item.getType().equals(Material.WATER_BUCKET))
					return;	// Cancel. Player is already receiving this water bucket.
			}
			addItem(p, new ItemStack(Material.BUCKET, 1));
		}
	}
	
	
	private static void addItem(Player player, ItemStack item) {
		Map<Integer, ItemStack> remainder = player.getInventory().addItem(item);
		if (remainder.isEmpty()) return;
		player.getWorld().dropItemNaturally(player.getLocation(), item);
	}
	
	
	//@EventHandler
	public void onInventoryClick_old(InventoryClickEvent e) {
		if (e.getClickedInventory() == null) return;
		InventoryType type = e.getClickedInventory().getType();
		if (type != InventoryType.CRAFTING && type != InventoryType.WORKBENCH) return;
		if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
		
		ItemStack itemStack = e.getCurrentItem();
		if (itemStack == null) return;
		
		if (!Backpacks.isBackpack(itemStack)) return;
		ItemMeta meta = itemStack.getItemMeta();
		assert meta != null;
		if (!meta.hasCustomModelData()) return;
		
		
		int amount = itemStack.getAmount();
		
		ItemStack[] items = e.getInventory().getStorageContents();
		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (item == null) continue;
			if (item.getType() == Material.WATER_BUCKET) {
				items[i] = new ItemStack(Material.BUCKET);
			} else if (sortedColors.contains(item.getType())) {
				ItemStack color = new ItemStack(item);
				color.setAmount(item.getAmount() - amount);
				items[i] = color;
			}
		}
		e.getInventory().setStorageContents(items);
	}
	
	/*
	@EventHandler
	public void completeCraftItem(CraftItemEvent e) {
		Bukkit.broadcastMessage("1");
		if (!Backpacks.isBackpack(e.getInventory().getResult())) return;
		Bukkit.broadcastMessage("2");
		ItemMeta meta = e.getInventory().getResult().getItemMeta();
		assert meta != null;
		Bukkit.broadcastMessage("3");
		if (!meta.hasCustomModelData()) return;
		Bukkit.broadcastMessage("4");
		if (meta.getCustomModelData() != baseColorModelData) return;
		Bukkit.broadcastMessage("5");
		
		ItemStack[] items = e.getInventory().getMatrix();
		for (ItemStack item : items) {
			Bukkit.broadcastMessage("item: " + item);
		}
		Bukkit.broadcastMessage("§dmaming");
	}*/

}
