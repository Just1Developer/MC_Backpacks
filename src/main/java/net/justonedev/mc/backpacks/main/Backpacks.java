package net.justonedev.mc.backpacks.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class Backpacks implements Listener {
	
	public static File f = new File(BackpacksMain.main.getDataFolder(), "config.yml");
	public static FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
	
	public static Material BackpackMat;
	public static Material EnderBackpackMat;
	public static int size;
	public static boolean enableBackpacks = true;
	public static boolean enableEnderbackpacks = true;
	public static boolean allowBackpacksInBackpacks = false;
	
	static String bpLore1 = "§7Backpack", bpTitleStart = "§8Backpack - ", bpTitleSepatator = " - ";
	
	public Backpacks() {
		
		BackpackMat = Material.CLOCK;
		EnderBackpackMat = Material.POLAR_BEAR_SPAWN_EGG;
		size = 36;
		
		if(!f.exists()) {
			cfg.options().copyDefaults(true);
			cfg.addDefault("Backpacks.Material", BackpackMat.toString());
			cfg.addDefault("Backpacks.Ender_Material", EnderBackpackMat.toString());
			cfg.addDefault("Backpacks.Size", 36);
			cfg.addDefault("Enable Backpacks", true);
			cfg.addDefault("Enable Ender-Backpacks", true);
			cfg.addDefault("Allow Backpacks in Backpacks", false);
			saveCfg();
		}
		
		enableBackpacks = cfg.getBoolean("Enable Backpacks");
		enableEnderbackpacks = cfg.getBoolean("Enable Ender-Backpacks");
		allowBackpacksInBackpacks = cfg.getBoolean("Allow Backpacks in Backpacks");
		
		try { BackpackMat = Material.getMaterial(getCfgString("Backpacks.Material"));
		} catch(ClassCastException e) { System.out.print("Could not convert " + cfg.get("Backpacks.Material") + " to a material: unknown material"); }
		
		try { EnderBackpackMat = Material.getMaterial(getCfgString("Backpacks.Ender_Material"));
		} catch(ClassCastException e) { System.out.print("Could not convert " + cfg.get("Backpacks.Ender_Material") + " to a material: unknown material"); }
		
		try { size = (int) (Math.ceil((double) cfg.getInt("Backpacks.Size") / 9.0) * 9);
		} catch(Exception e) { System.out.print("There was an unknown error while importing the size of the backpacks. The size has been set to default (36)"); }
		
		Iterator<Recipe> it = BackpacksMain.main.getServer().recipeIterator();
		Recipe recipe;
		while(it.hasNext()) {
			recipe = it.next();
			if (recipe != null) {
				if(recipe.getResult().getType().equals(BackpackMat) && enableBackpacks) it.remove();
				if(recipe.getResult().getType().equals(EnderBackpackMat) && enableEnderbackpacks) it.remove();
			}
		}
		
		// Add Backpack Recipe
		
		ItemStack bp = new ItemStack(BackpackMat, 1);
		ItemMeta bpm = bp.getItemMeta();
		assert bpm != null;
		bpm.setDisplayName("§fBackpack");
		
		ArrayList<String> lore = new ArrayList<>();
		lore.add("§7Backpack");
		lore.add("§cBetter not stack");
		
		bpm.setLore(lore);
		bp.setItemMeta(bpm);
		
		
		ShapedRecipe Backpack = new ShapedRecipe(new NamespacedKey(BackpacksMain.main, "backpack"), bp);
		Backpack.shape("S", "E", "S");
		Backpack.setIngredient('E', Material.ENDER_PEARL);
		Backpack.setIngredient('S', Material.SHULKER_SHELL);
		
		// Add Ender-Backpack Recipe
		
		ItemStack ebp = new ItemStack(EnderBackpackMat, 1);
		ItemMeta ebpm = ebp.getItemMeta();
		assert ebpm != null;
		ebpm.setDisplayName("§5Ender-Backpack");
		
		lore.clear();
		lore.add("§5Ender-Backpack");
		
		ebpm.setLore(lore);
		ebp.setItemMeta(ebpm);
		
		
		ShapedRecipe EnderBackpack = new ShapedRecipe(new NamespacedKey(BackpacksMain.main, "ender_backpack"), ebp);
		EnderBackpack.shape("S", "C", "S");
		EnderBackpack.setIngredient('S', Material.SHULKER_SHELL);
		EnderBackpack.setIngredient('C', Material.ENDER_CHEST);

		Bukkit.addRecipe(Backpack);
		logInfo("Added the Backpack Crafting Recipe");
		Bukkit.addRecipe(EnderBackpack);
		logInfo("Added the Ender-Backpack Crafting Recipe");
	}
	
	private String getCfgString(String name)
	{
		String s = cfg.getString(name);
		if(s == null) s = "";
		return s;
	}
	
	private static void logInfo(String s)
	{
		BackpacksMain.main.getLogger().info(s);
	}
	
	
	
	@EventHandler
	public void onInvClose(InventoryCloseEvent e) {
		
		Inventory inv = e.getInventory();
		String[] title = e.getView().getTitle().split(" - ");
		
		if(inv.getSize() == size && inv.getHolder() == null && title[0].equals(bpTitleStart.replace(bpTitleSepatator, ""))) {
			
			int uuid;
			uuid = Integer.parseInt(title[1]);
			
			saveInvToFile(inv, uuid);
		}
	}
	
	
	
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		
		Player p = e.getPlayer();
		if(e.getItem() == null) return;
		ItemStack it = e.getItem();
		
		if(!it.hasItemMeta()) return;
		assert it.getItemMeta() != null;
		if(!it.getItemMeta().hasDisplayName()) return;
		
		ItemMeta m = it.getItemMeta();
		if(m == null) return;
		if(e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;
		
		if(m.hasLore()) {
			assert m.getLore() != null;
			if(m.getLore().get(0) == null) return;
			if(m.getLore().get(0).equals(bpLore1)) {
				
				if(!it.getType().equals(BackpackMat)) return;
				if(!enableBackpacks) return;
				
				int uuid = 0;
				
				if(m.hasLore()) {
					List<String> l = m.getLore();
					if(l.get(1) != null && !l.get(1).startsWith("§c")) {
						try {
							uuid = Integer.parseInt(l.get(1).replaceFirst("§7", ""));
						} catch(NumberFormatException ex) {
							ex.printStackTrace();
						}
					}
				}
				
				if(uuid == 0) {
					
					uuid = getRndUUID();
					ArrayList<String> lore = new ArrayList<>();
					lore.add(bpLore1);
					lore.add("§7" + uuid);
					
					if(it.getAmount() > 1) {
						
						int amount = it.getAmount();
						ItemStack it2 = it.clone();
						it2.setAmount(amount-1);
						it.setAmount(1);
						
						m.setLore(lore);
						it.setItemMeta(m);
						
						int i = 0;
						while(true) {
							Inventory inv = p.getInventory();
							if(inv.getItem(i) == null) {
								p.getInventory().addItem(it2);
								break;
							} else i++;
							if(i == 36) {
								// Originally: p.getWorld().dropItemNaturally(p.getLocation(), it2); break;
								// Now, drop the newly generated backpack:
								e.setCancelled(true);
								p.getInventory().setItemInMainHand(it);
								p.dropItem(true);	// Sure why not
								p.getInventory().setItemInMainHand(it2);
								// Don't open inv
								return;
							}
						}
						
					} else {
						
						m.setLore(lore);
						it.setItemMeta(m);
						
					}
					
				}
				
				e.setCancelled(true);
				// Doesn't work :(
				ItemStack itemStack = p.getInventory().getItemInMainHand();
				p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));	// Update item in main hand, just for the lil animation for interaction
				p.getInventory().setItemInMainHand(itemStack);
				p.openInventory(GetFromFile(uuid));
				p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 7.0F, 1.0F);
				
			} else if(m.getLore().get(0).equals("§5Ender-Backpack") && it.getType().equals(EnderBackpackMat)) {
				
				if(!enableEnderbackpacks) return;
				
				if(m.getLore().size() >= 2) {
					ArrayList<String> lore = new ArrayList<>();
					lore.add("§5Ender-Backpack");
					
					m.setLore(lore);
					it.setItemMeta(m);
				}
				
				e.setCancelled(true);
				ItemStack itemStack = p.getInventory().getItemInMainHand();
				p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));	// Update item in main hand, just for the lil animation for interaction
				p.getInventory().setItemInMainHand(itemStack);
				p.openInventory(p.getEnderChest());
				p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 7.0F, 1.0F);
				
			}
		}
	}
	
	
	@EventHandler
	public void onItemDropped(PlayerDropItemEvent e)
	{
		Player p = e.getPlayer();
		if(!p.getOpenInventory().getTitle().startsWith("§8Backpack - ")) return;
		ItemStack it = e.getItemDrop().getItemStack();
		
		if(!it.hasItemMeta()) return;
		assert it.getItemMeta() != null;
		if(!it.getItemMeta().hasLore()) return;
		assert it.getItemMeta().getLore() != null;
		if(it.getItemMeta().getLore().size() != 2) return;
		
		if(!it.getItemMeta().getLore().get(0).equals(bpLore1)) return;
		// Compare IDs
		if(!it.getItemMeta().getLore().get(1).substring(2).equals(p.getOpenInventory().getTitle().substring(13))) return;
		
		p.closeInventory();
	}
	
	
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		
		ItemStack it = e.getCurrentItem();
		ItemStack cursor = e.getCursor();
		Inventory inv = e.getClickedInventory();
		
		if(it == null && cursor == null) return;
		
		if(e.getView().getTitle().startsWith(bpTitleStart)) {

			int uuid = Integer.parseInt(e.getView().getTitle().split(bpTitleSepatator)[1]);
			
			if(!(inv instanceof PlayerInventory) || e.isShiftClick()) {
				// Shift Click Context
				if((isItemBackpackAndDisallowed(cursor) || isTheBackpackFromInventory(uuid, cursor)) && !e.isShiftClick() ||
						(isItemBackpackAndDisallowed(it) || isTheBackpackFromInventory(uuid, it)) && e.isShiftClick())
				{
					e.setCancelled(true);
				}
			}
		}
	}
	
	
	
	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		
		if(e.getInventory().getHolder() != null) return;
		if(!(e.getWhoClicked() instanceof Player)) return;
		
		if(e.getView().getTitle().startsWith(bpTitleStart)) {
			
			int uuid = Integer.parseInt(e.getView().getTitle().split(bpTitleSepatator)[1]);

			if(!(e.getInventory() instanceof PlayerInventory)) {
				if(isTheBackpackFromInventory(uuid, e.getOldCursor()) || isTheBackpackFromInventory(uuid, e.getCursor()) || isEitherItemBackpackAndDisallowed(e.getOldCursor(), e.getCursor())) {
					Player p = (Player) e.getWhoClicked();
					for(int i : e.getRawSlots())
					{
						// Backpack inv is open, get size and check if affected slots contains size;
						if(i >= p.getOpenInventory().getTopInventory().getSize()) continue;
						e.setCancelled(true);
						e.setResult(Result.DENY);
					}
				}
			}
		}
	}
	
	
	
	public boolean isTheBackpackFromInventory(int BackpackID, ItemStack i) {
		
		if(i == null) return false;
		if(!i.hasItemMeta()) return false;
		ItemMeta m = i.getItemMeta();
		assert m != null;
		if(!m.hasLore()) return false;
		List<String> l = m.getLore();
		assert l != null;
		if(l.isEmpty()) return false;
		String s = l.get(0);
		
		if(s.equals(bpLore1) && l.size() == 2) {
			String id = l.get(1).replaceFirst("§7", "");
			return id.equals(BackpackID + "");
		}
		
		return false;
	}
	
	// If the current item is a backpack and putting backpacks inside backpacks is forbidden
	public boolean isEitherItemBackpackAndDisallowed(ItemStack it1, ItemStack it2) { return isItemBackpackAndDisallowed(it1) || isItemBackpackAndDisallowed(it2); }
	public boolean isItemBackpackAndDisallowed(ItemStack it)
	{
		if(allowBackpacksInBackpacks) return false;
		
		if(it == null) return false;
		if(!it.hasItemMeta()) return false;
		assert it.getItemMeta() != null;
		if(!it.getItemMeta().hasLore()) return false;
		assert it.getItemMeta().getLore() != null;
		if(it.getItemMeta().getLore().size() != 2) return false;
		
		return it.getItemMeta().getLore().get(0).equals(bpLore1);
	}
	
	
	//----------------------------------------------------FILE STUFF--------------------------------------------------------------------------
	
	
	public void saveInvToFile(Inventory inv, int UUID) {
		
		File f = new File(BackpacksMain.main.getDataFolder() + "/backpacks/", UUID + ".yml");
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
		
		cfg.set("inv.size", inv.getSize());
		
		
		for(int i = 0; i < inv.getSize(); i++) {
			cfg.set("invSlot." + i, inv.getContents()[i]);
		}
		
		
		cfg.set("invStorage", inv.getStorageContents());
		
		
		try {
			cfg.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	public Inventory GetFromFile(int UUID) {
		
		File f = new File(BackpacksMain.main.getDataFolder() + "/backpacks/", UUID + ".yml");
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
		
		
		if(!f.exists()) {
			try {
				cfg.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return Bukkit.createInventory(null, 36, "§8Backpack - " + UUID);
		}
		
		
		Inventory inv = Bukkit.createInventory(null, 36, "§8Backpack - " + UUID);
		
		
		for(int i = 0; i < inv.getSize(); i++) {
			inv.setItem(i, cfg.getItemStack("invSlot." + i));
		}
		
		return inv;
		
	}
	
	
	
	public int getRndUUID() {
		int id = rndInt(100000000, 999999999);
		while(true) {
			if(!new File(BackpacksMain.main.getDataFolder() + "/backpacks/", id + ".yml").exists()) break;
			else id = rndInt(100000000, 999999999);
		}
		return id;
	}
	
	
	
	public int rndInt(int min, int max) {
		Random r = new Random();
		return r.nextInt((max-min) + 1) + min;
	}
	
	
	
	public static void saveCfg() {
		try {
			cfg.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
