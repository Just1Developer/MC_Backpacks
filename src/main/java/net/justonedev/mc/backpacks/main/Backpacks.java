package net.justonedev.mc.backpacks.main;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
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
	
	public static Material BackpackMat;
	public static Material EnderBackpackMat;
	public static int size;
	public static boolean enableBackpacks = true;
	public static boolean enableEnderbackpacks = true;
	public static boolean allowBackpacksInBackpacks = false;

	private static final int DefaultSize = 36;

	static String bpName = "§fBackpack", bpNameEnder = "§5Ender-Backpack";
	static String bpLore1 = "§7Backpack", bpLore2 = "§5Ender-Backpack", bpTitleStart = "§8Backpack - ", bpTitleSepatator = " - ";
	
	public Backpacks() {
		
		File f = new File(BackpacksMain.main.getDataFolder(), "config.yml");
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
		
		BackpacksMain.prefix = ChatColor.translateAlternateColorCodes('&', getCfgString(f, cfg, "Plugin prefix", BackpacksMain.prefix));
		BackpacksMain.recipeLoadDelayTicks = getCfgInt(f, cfg, "Recipe Load Delay in Ticks", BackpacksMain.recipeLoadDelayTicks);
		
		BackpackMat = Material.CLOCK;
		EnderBackpackMat = Material.POLAR_BEAR_SPAWN_EGG;
		size = 36;
		
		if(!f.exists()) {
			cfg.options().copyDefaults(true);
			cfg.addDefault("Backpacks.Material", BackpackMat.toString());
			cfg.addDefault("Backpacks.Ender_Material", EnderBackpackMat.toString());
			cfg.addDefault("Backpacks.Size", DefaultSize);
			cfg.addDefault("Enable Backpacks", true);
			cfg.addDefault("Enable Ender-Backpacks", true);
			cfg.addDefault("Allow Backpacks in Backpacks", false);
			saveCfg(f, cfg);
		}
		
		enableBackpacks = cfg.getBoolean("Enable Backpacks");
		enableEnderbackpacks = cfg.getBoolean("Enable Ender-Backpacks");
		allowBackpacksInBackpacks = cfg.getBoolean("Allow Backpacks in Backpacks");
		
		bpName = getCfgString(f, cfg, "Backpack Displayname", "§fBackpack");
		bpNameEnder = getCfgString(f, cfg, "Ender-Backpack Displayname", "§5Ender-Backpack");
		
		try { BackpackMat = Material.getMaterial(getCfgString(f, cfg, "Backpacks.Material", "CLOCK"));
		} catch(ClassCastException e) { System.out.print("Could not convert " + cfg.get("Backpacks.Material") + " to a material: unknown material"); }
		
		try { EnderBackpackMat = Material.getMaterial(getCfgString(f, cfg, "Backpacks.Ender_Material", "POLAR_BEAR_SPAWN_EGG"));
		} catch(ClassCastException e) { System.out.print("Could not convert " + cfg.get("Backpacks.Ender_Material") + " to a material: unknown material"); }
		
		try { size = (int) Math.max(Math.ceil((double) cfg.getInt("Backpacks.Size") / 9.0) * 9, 9);	// Size >= 9
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
		
		// Set the items:
		
		backpackItem = new ItemStack(BackpackMat, 1);
		ItemMeta bpm = backpackItem.getItemMeta();
		assert bpm != null;
		bpm.setDisplayName(bpName);
		bpm.setCustomModelData(BackpackColor.baseColorModelData);
		
		bpm.setLore(Collections.singletonList(bpLore1));
		backpackItem.setItemMeta(bpm);
		
		
		enderbackpackItem = new ItemStack(EnderBackpackMat, 1);
		ItemMeta ebpm = enderbackpackItem.getItemMeta();
		assert ebpm != null;
		ebpm.setDisplayName(bpNameEnder);
		
		ebpm.setLore(Collections.singletonList(bpLore2));
		enderbackpackItem.setItemMeta(ebpm);
		
		// Recipes are loaded 0.5 seconds after
	}
	
	ShapedRecipe Recipe_Backpack, Recipe_Enderbackpack;
	ItemStack backpackItem, enderbackpackItem;
	
	public void reloadRecipes()
	{
		if(Recipe_Backpack != null) Bukkit.removeRecipe(Recipe_Backpack.getKey());
		if(Recipe_Enderbackpack != null) Bukkit.removeRecipe(Recipe_Enderbackpack.getKey());
		
		// Add Backpack Recipe
		
		Recipe_Backpack = recipeOf("Backpack", backpackItem, "backpack", Material.ENDER_PEARL);
		
		// Add Ender-Backpack Recipe
		
		Recipe_Enderbackpack = recipeOf("EnderBackpack", enderbackpackItem, "ender_backpack", Material.ENDER_CHEST);
		
		Bukkit.addRecipe(Recipe_Backpack);
		logInfo("Added the Backpack Crafting Recipe");
		Bukkit.addRecipe(Recipe_Enderbackpack);
		logInfo("Added the Ender-Backpack Crafting Recipe");
	}
	
	private ShapedRecipe recipeOf(String recipeEntry, ItemStack result, String recipeKey, Material FallBackCenterMaterial)
	{
		File f = new File(BackpacksMain.main.getDataFolder(), "recipes.yml");
		YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
		if(!f.exists()) createRecipesFile(f, cfg);
		List<String> shape = (List<String>) cfg.getList(recipeEntry + ".shape");
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(BackpacksMain.main, recipeKey), result);
		try {
			assert shape != null;
			recipe.shape(shape.toArray(new String[0]));
			Set<Character> entries = new HashSet<>();
			for(String c2 : shape) {
				for (String c1 : c2.split("")) {
					if (c1.length() != 1) continue;
					char c = c1.charAt(0);
					if (entries.contains(c)) continue;
					entries.add(c);
					recipe.setIngredient(c, Material.valueOf(cfg.getString(recipeEntry + ".materials." + c)));
				}
			}
		} catch (Exception e)
		{
			Bukkit.getLogger().severe("Failed to load recipe for " + result.getItemMeta().getDisplayName() + ". Loading default crafting recipe instead. Stack trace:");
			e.printStackTrace();
			recipe.shape("S", "E", "S");
			recipe.setIngredient('E', FallBackCenterMaterial);
			recipe.setIngredient('S', Material.SHULKER_SHELL);
		}
		return recipe;
	}
	
	private void createRecipesFile(File f, YamlConfiguration cfg)
	{
		cfg.set("Backpack.shape", Arrays.asList("S", "E", "S"));
		cfg.set("Backpack.materials.S", Material.SHULKER_SHELL.toString());
		cfg.set("Backpack.materials.E", Material.ENDER_PEARL.toString());
		
		cfg.set("EnderBackpack.shape", Arrays.asList("S", "E", "S"));
		cfg.set("EnderBackpack.materials.S", Material.SHULKER_SHELL.toString());
		cfg.set("EnderBackpack.materials.E", Material.ENDER_CHEST.toString());
		
		try {
			cfg.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getCfgString(File f, FileConfiguration cfg, String name, String _default)
	{
		String s = cfg.getString(name);
		if(s == null)
		{
			s = _default;
			cfg.set(name, _default);
			saveCfg(f, cfg);
		}
		return s;
	}
	
	public static int getCfgInt(File f, FileConfiguration cfg, String name, int _default)
	{
		if(!cfg.contains(name))
		{
			cfg.set(name, _default);
			saveCfg(f, cfg);
			return _default;
		}
		return cfg.getInt(name);
	}
	
	private static void logInfo(String s)
	{
		BackpacksMain.main.getLogger().info(s);
	}
	
	
	
	@EventHandler
	public void onInvClose(InventoryCloseEvent e) {
		
		Inventory inv = e.getInventory();
		String[] title = e.getView().getTitle().split(" - ");
		
		if(inv.getHolder() == null && title[0].equals(bpTitleStart.replace(bpTitleSepatator, ""))) {
			
			int uuid;
			uuid = Integer.parseInt(title[1]);
			
			saveInvToFile(inv, uuid);
		}
	}
	
	/** Every block that supersedes interaction with an item
	 */
	final List<Material> Interactables = Arrays.asList(Material.CHEST, Material.TRAPPED_CHEST, Material.BREWING_STAND, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.LOOM, Material.SMITHING_TABLE, Material.ENDER_CHEST, Material.STONECUTTER, Material.GRINDSTONE, Material.FURNACE, Material.BLAST_FURNACE, Material.HOPPER,
			//Material.MINECART, Material.FURNACE_MINECART, Material.FURNACE_MINECART, Material.CHEST_MINECART, Material.COMMAND_BLOCK_MINECART,
			Material.COMMAND_BLOCK, Material.BEACON, Material.CRAFTING_TABLE, Material.SMOKER, Material.DAYLIGHT_DETECTOR,
			Material.LEVER, Material.STONE_BUTTON, Material.ARMOR_STAND, Material.ITEM_FRAME, Material.BELL, Material.ENCHANTING_TABLE, Material.CHISELED_BOOKSHELF, Material.LECTERN, Material.CARTOGRAPHY_TABLE, Material.BARREL,
			Material.WHITE_BED, Material.LIGHT_GRAY_BED, Material.GRAY_BED, Material.BLACK_BED, Material.BROWN_BED, Material.RED_BED, Material.ORANGE_BED, Material.YELLOW_BED, Material.LIME_BED, Material.GREEN_BED, Material.CYAN_BED, Material.LIGHT_BLUE_BED, Material.BLUE_BED, Material.PURPLE_BED, Material.MAGENTA_BED, Material.PINK_BED,
			Material.DARK_OAK_DOOR, Material.ACACIA_DOOR, Material.BAMBOO_DOOR, Material.BIRCH_DOOR, Material.OAK_DOOR, Material.CHERRY_DOOR, Material.CRIMSON_DOOR, Material.JUNGLE_DOOR, Material.MANGROVE_DOOR, Material.SPRUCE_DOOR, Material.WARPED_DOOR,
			// These are entities smh
			//Material.DARK_OAK_BOAT, Material.ACACIA_BOAT, Material.BIRCH_BOAT, Material.OAK_BOAT, Material.CHERRY_BOAT, Material.JUNGLE_BOAT, Material.MANGROVE_BOAT, Material.SPRUCE_BOAT,
			//Material.DARK_OAK_CHEST_BOAT, Material.ACACIA_CHEST_BOAT, Material.BIRCH_CHEST_BOAT, Material.OAK_CHEST_BOAT, Material.CHERRY_CHEST_BOAT, Material.JUNGLE_CHEST_BOAT, Material.MANGROVE_CHEST_BOAT, Material.SPRUCE_CHEST_BOAT,
			Material.DARK_OAK_WALL_SIGN, Material.ACACIA_WALL_SIGN, Material.BAMBOO_WALL_SIGN, Material.BIRCH_WALL_SIGN, Material.OAK_WALL_SIGN, Material.CHERRY_WALL_SIGN, Material.CRIMSON_WALL_SIGN, Material.JUNGLE_WALL_SIGN, Material.MANGROVE_WALL_SIGN, Material.SPRUCE_WALL_SIGN, Material.WARPED_WALL_SIGN,
			Material.DARK_OAK_HANGING_SIGN, Material.ACACIA_HANGING_SIGN, Material.BAMBOO_HANGING_SIGN, Material.BIRCH_HANGING_SIGN, Material.OAK_HANGING_SIGN, Material.CHERRY_HANGING_SIGN, Material.CRIMSON_HANGING_SIGN, Material.JUNGLE_HANGING_SIGN, Material.MANGROVE_HANGING_SIGN, Material.SPRUCE_HANGING_SIGN, Material.WARPED_HANGING_SIGN,
			Material.DARK_OAK_SIGN, Material.ACACIA_SIGN, Material.BAMBOO_SIGN, Material.BIRCH_SIGN, Material.OAK_SIGN, Material.CHERRY_SIGN, Material.CRIMSON_SIGN, Material.JUNGLE_SIGN, Material.MANGROVE_SIGN, Material.SPRUCE_SIGN, Material.WARPED_SIGN,
			Material.DARK_OAK_BUTTON, Material.ACACIA_BUTTON, Material.BAMBOO_BUTTON, Material.BIRCH_BUTTON, Material.OAK_BUTTON, Material.CHERRY_BUTTON, Material.CRIMSON_BUTTON, Material.JUNGLE_BUTTON, Material.MANGROVE_BUTTON, Material.SPRUCE_BUTTON, Material.WARPED_BUTTON,
			Material.DARK_OAK_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.BAMBOO_FENCE_GATE, Material.BIRCH_FENCE_GATE, Material.OAK_FENCE_GATE, Material.CHERRY_FENCE_GATE, Material.CRIMSON_FENCE_GATE, Material.JUNGLE_FENCE_GATE, Material.MANGROVE_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.WARPED_FENCE_GATE,
			Material.DARK_OAK_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.BAMBOO_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.OAK_TRAPDOOR, Material.CHERRY_TRAPDOOR, Material.CRIMSON_TRAPDOOR, Material.JUNGLE_TRAPDOOR, Material.MANGROVE_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.WARPED_TRAPDOOR);
	
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		
		Player p = e.getPlayer();
		if (e.getItem() == null) return;
		ItemStack it = e.getItem();
		
		if (!it.hasItemMeta()) return;
		assert it.getItemMeta() != null;
		if (!it.getItemMeta().hasDisplayName()) return;
		
		ItemMeta m = it.getItemMeta();
		if (m == null) return;
		if (e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;
		
		if (!m.hasLore()) return;
		if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !e.getPlayer().isSneaking() && Interactables.contains(Objects.requireNonNull(e.getClickedBlock()).getType())) return;
		
		assert m.getLore() != null;
		if(m.getLore().get(0) == null) return;
		if(m.getLore().get(0).equals(bpLore1)) {
			
			if(!it.getType().equals(BackpackMat)) return;
			if(!enableBackpacks) return;
			
			int uuid = 0;
			
			if(m.hasLore()) {
				List<String> l = m.getLore();
				if(l.size() == 2 && l.get(1) != null && !l.get(1).startsWith("§c")) {
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
			/*
			if(e.getPlayer().isSneaking() && (e.getPlayer().getInventory().getChestplate() == null || e.getPlayer().getInventory().getChestplate().getType() == Material.AIR))
			{
				// Equip to back
				ArmorStand stand = (ArmorStand) p.getWorld().spawnEntity(p.getLocation(), EntityType.ARMOR_STAND);
				e.getPlayer().addPassenger(stand);
				assert stand.getEquipment() != null;
				stand.getEquipment().setHelmet(new ItemStack(Material.CHEST));
				return;
			}
			 */
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
				else if(e.getClick() == ClickType.NUMBER_KEY)
				{
					if(isItemBackpackAndDisallowed(e.getWhoClicked().getInventory().getItem(e.getHotbarButton()))) e.setCancelled(true);
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
		
		if (!isBackpack(i)) return false;
		return BackpackID != 0 && BackpackID == getIDofValidBackpack(i);
		
		/*
		
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
		
		 */
	}
	
	private static final int uuidLength = 9;
	private static final String uuidRegex = String.format("§7\\d{%d,%d}", uuidLength, uuidLength + 1);
	private static final Pattern pattern = Pattern.compile(uuidRegex);
	
	public static boolean isBackpack(ItemStack item) {
		
		if(item == null) return false;
		if(!item.hasItemMeta()) return false;
		ItemMeta m = item.getItemMeta();
		assert m != null;
		if(!m.hasLore()) return false;
		List<String> l = m.getLore();
		assert l != null;
		if(l.isEmpty()) return false;
		String s = l.get(0);
		
		return s.equals(bpLore1) && (l.size() == 1 || l.size() == 2 && pattern.matcher(l.get(1)).matches());
	}
	
	public static int getIDofValidBackpack(ItemStack item) {
		List<String> l = Objects.requireNonNull(item.getItemMeta()).getLore();
		assert l != null;
		return l.size() != 2 ? 0 : Integer.parseInt(l.get(1).substring(2));
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
		
		cfg.set("inv.size", inv.getSize());		// Todo perhaps improve this

		for(int i = 0; i < inv.getSize(); i++) {
			cfg.set("invSlot." + i, inv.getContents()[i]);
		}
		
		try {
			cfg.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	public Inventory GetFromFile(int UUID) {
		
		File f = new File(BackpacksMain.main.getDataFolder() + "/backpacks/", UUID + ".yml");
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);

		// Get size of inventory

		if(!f.exists()) {
			try {
				cfg.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}

			Inventory inv = Bukkit.createInventory(null, size, "§8Backpack - " + UUID);
			return inv;
		}

		int size = cfg.getInt("inv.size");
		if (size == 0) size = Backpacks.size;	// If no size is set yet

		ConfigurationSection slotSection = cfg.getConfigurationSection("invSlot");

		if (slotSection == null) return Bukkit.createInventory(null, size, "§8Backpack - " + UUID);	// Slots there

		// Iterate through all keys and find if something is in a slot that would be outside the bounds of the inventory
		for (String key : slotSection.getKeys(false))
		{
			int parse = Integer.parseInt(key);
			if (parse > size) size = parse;
		}

		size = (int) Math.max(Math.ceil((double) size / 9.0) * 9, 9);	// Size >= 9
		Inventory inv = Bukkit.createInventory(null, size, "§8Backpack - " + UUID);
		// Inventory size is either the preferred size, the set size, or the size it needs to be to fit all the items

		for(int i = 0; i < inv.getSize(); i++) {
			inv.setItem(i, cfg.getItemStack("invSlot." + i));
		}

		return inv;
		
	}
	
	
	
	public int getRndUUID() {
		int id = rndInt((int) Math.pow(10, uuidLength - 1), (int) Math.pow(10, uuidLength));
		while(true) {
			if(!new File(BackpacksMain.main.getDataFolder() + "/backpacks/", id + ".yml").exists()) break;
			else id = rndInt((int) Math.pow(10, uuidLength - 1), (int) Math.pow(10, uuidLength));
		}
		return id;
	}
	
	
	
	public int rndInt(int min, int max) {
		Random r = new Random();
		return r.nextInt(max - min) + min;
	}
	
	
	
	public static void saveCfg(File f, FileConfiguration cfg) {
		try {
			cfg.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
