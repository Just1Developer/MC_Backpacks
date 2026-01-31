package net.justonedev.mc.backpacks.main;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("StaticNonFinalField")
public final class BackpacksMain extends JavaPlugin implements Listener {
	
	public static BackpacksMain main;
	public static  Backpacks backpacks;
	public static String PLUGIN_PREFIX = "&7[&6Backpacks&7] &e";
	public static int RECIPE_LOAD_DELAY_TICKS = 10;
    public static final String TEXTURE_PACK_NAMESPACE = "backpacks";
    private static final List<Integer> LEGACY_MODELS_VERSION_CUTOFF = List.of(1, 21, 3);

    public static boolean USE_NEW_TEXTURE_PACK_SYSTEM;

    private static boolean isDevelopmentBuild = false;
	
	static boolean useResourcePack = true;
	private static final UUID RESOURCE_PACK_UUID = UUID.randomUUID();

    private static final String RESOURCE_PACK_URL = "https://www.dropbox.com/scl/fi/ovyvky4rxqwm7b1bz5zjf/bColored-Backpacks-1.21.4.zip?rlkey=wgvbpxzc9kwxww8vq5noiuln3&st=9qdfw9ax&dl=1";
	private static final String RESOURCE_PACK_SHA1_STR = "e01b7085ca9b1a0f544dc8ae40492c85e4d1aa82";
	private static final byte[] RESOURCE_PACK_SHA1 = hexStringToByteArray(RESOURCE_PACK_SHA1_STR);

	private static final String LEGACY_RESOURCE_PACK_URL = "https://www.dropbox.com/scl/fi/238l79hi64kbh0v24j8oc/bColored-Backpacks-1.20.zip?rlkey=zg0jt6bb05chn4stqme5v1ybk&st=by8p5wl3&dl=1";
	private static final String LEGACY_RESOURCE_PACK_SHA1_STR = "ad9f8ab80e9a14c9eecb78a196d89622669d47e1";
	private static final byte[] LEGACY_RESOURCE_PACK_SHA1 = hexStringToByteArray(LEGACY_RESOURCE_PACK_SHA1_STR);
	// Generated using [Windows: certutil -hashfile "§9Tardis Pack.zip" SHA1   MacOS: shasum -a 1 "§9Tardis Pack.zip"]

    private static String inUseResourcePackUrl;
    private static byte[] inUseResourcePackSHA1;

	@Override
	public void onEnable() {
		main = this;
        USE_NEW_TEXTURE_PACK_SYSTEM = isNewModelVersion();

        if (USE_NEW_TEXTURE_PACK_SYSTEM) {
            inUseResourcePackUrl = RESOURCE_PACK_URL;
            inUseResourcePackSHA1 = RESOURCE_PACK_SHA1;
        } else {
            inUseResourcePackUrl = LEGACY_RESOURCE_PACK_URL;
            inUseResourcePackSHA1 = LEGACY_RESOURCE_PACK_SHA1;
        }

		if (this.getDescription().getVersion().toLowerCase().contains("dev")) {
            isDevelopmentBuild = true;
            Bukkit.getLogger().warning("[" + this.getDescription().getPrefix() + "] Please note that this is a developer build and could contain bugs or untested features.");
        }
		backpacks = new Backpacks();
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getPluginManager().registerEvents(backpacks, this);
		Bukkit.getPluginManager().registerEvents(new BackpackCrafting(), this);

        var command = this.getCommand("backpacks");
		if (command != null) {
            command.setTabCompleter(this);
            command.setExecutor(this);
        }
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> backpacks.reloadRecipes(), 10);
	}

    private boolean isNewModelVersion() {
        System.out.println("1 >>>>>>>>>>>>>>>>" + Bukkit.getVersion());
        System.out.println("2 >>>>>>>>>>>>>>>>" + Bukkit.getServer().getBukkitVersion());
        System.out.println("3 >>>>>>>>>>>>>>>>" + Bukkit.getServer().getVersion());

        String versionString = Bukkit.getVersion();
        Matcher matcher = Pattern.compile("^(\\d+(?:\\.\\d+)+).*").matcher(versionString);
        if (!matcher.matches()) return true;
        String parsedVersion = matcher.group(1);
        System.out.println(">>>>>>>>>>>>>>>>>>> Detected Minecraft version: " + parsedVersion);

        List<Integer> version = Arrays.stream(parsedVersion
                .split("\\."))
                .map(Integer::parseInt)
                .toList();
        for (int i = 0; i < Math.min(version.size(), LEGACY_MODELS_VERSION_CUTOFF.size()); i++) {
            System.out.println("[" + i + "] Comparing " + LEGACY_MODELS_VERSION_CUTOFF.get(i)+" and "+version.get(i) + " | new version: "+(LEGACY_MODELS_VERSION_CUTOFF.get(i) < version.get(i))+"inconclusive: "+(LEGACY_MODELS_VERSION_CUTOFF.get(i) > version.get(i)&&LEGACY_MODELS_VERSION_CUTOFF.get(i) < version.get(i)));
            if (LEGACY_MODELS_VERSION_CUTOFF.get(i) < version.get(i)) {
                return true;
            } else if (LEGACY_MODELS_VERSION_CUTOFF.get(i) > version.get(i)) {
                return false;
            }
        }
        return false;
    }
	
	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (useResourcePack) event.getPlayer().addResourcePack(RESOURCE_PACK_UUID, inUseResourcePackUrl, inUseResourcePackSHA1, "This server uses a resource pack for (colored) backpacks. Usage is recommended.", false);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof ConsoleCommandSender) && !sender.isOp())
		{
			sender.sendMessage(PLUGIN_PREFIX + "§cYou don't have permission for that!");
			return true;
		}
		if(args.length < 1 || args.length > 3)
		{
			sendHelp(sender);
			return true;
		}
		
		String subcmd = args[0].toLowerCase();
        Player p;
		switch(subcmd)
		{
			case "reloadrecipes":
				backpacks.reloadRecipes();
				if(args.length != 1)
				{
					sender.sendMessage(PLUGIN_PREFIX + "/backpacks reloadRecipes §c<no other arguments here>");
					sender.sendMessage(PLUGIN_PREFIX + "Recipes reloaded anyway.");
				}
				else sender.sendMessage(PLUGIN_PREFIX + "Recipes reloaded.");
				break;
			case "giveender":
			case "give":
				int amount = 1;
                if(args.length == 1) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(PLUGIN_PREFIX + "/backpacks " + subcmd + " §6<Player> §9[amount]");
                        return true;
                    }
                    p = (Player) sender;
				} else {
                    p = Bukkit.getPlayer(args[1]);
                }
                if(p == null) {
                    sender.sendMessage(PLUGIN_PREFIX + "§cSorry, but §6" + args[1] + "§c isn't online right now.");
                    return true;
                }

				if(args.length == 3)
				{
					try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored)
					{ sender.sendMessage(PLUGIN_PREFIX + "/backpacks " + subcmd + " §6<Player> §9[amount]§e. §cThe amount should be an integer though :)"); return false; }
				}
				ItemStack t;
				if(subcmd.length() == 4) t = backpacks.backpackItem.clone();
				else t = backpacks.enderbackpackItem.clone();
				t.setAmount(amount);
				p.getInventory().addItem(t);
				if(subcmd.length() == 4) sender.sendMessage(PLUGIN_PREFIX + "Gave " + amount + " Backpack" + (amount == 1 ? "" : "s") + " to §c" + p.getName());
				else sender.sendMessage(PLUGIN_PREFIX + "Gave " + amount + " Ender-Backpack" + (amount == 1 ? "" : "s") + " to §c" + p.getName());
				break;
            case "modeldata":
                if (isDevelopmentBuild) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(PLUGIN_PREFIX + "This command can only be executed by a player");
                        return true;
                    }
                    p = (Player) sender;
                    ItemStack item = p.getInventory().getItemInMainHand();
                    if (item.getType() == Material.AIR) item = p.getInventory().getItemInOffHand();
                    if (item.getType() == Material.AIR) {
                        p.sendMessage(PLUGIN_PREFIX + "You need to hold something.");
                        return true;
                    }

                    ItemMeta meta = item.getItemMeta();

                    assert meta != null;

                    var prev = meta.getItemModel();
                    if (prev == null) prev = new NamespacedKey("unknown", "unknown");
                    var _new = new NamespacedKey("backpacks", "backpack");

                    meta.setItemModel(_new);
                    item.setItemMeta(meta);
                    p.sendMessage("§eDone. | %s:%s -> %s:%s".formatted(prev.getNamespace(), prev.getKey(), _new.getNamespace(), _new.getKey()));
                    break;
                }
			default:
				sendHelp(sender);
		}
		return true;
	}
	
	void sendHelp(CommandSender sender)
	{
		sender.sendMessage(PLUGIN_PREFIX + "§cUnknown command. Here's some help:");
		sender.sendMessage(PLUGIN_PREFIX + "/backpacks reloadRecipes");
		sender.sendMessage(PLUGIN_PREFIX + "/backpacks give §6<Player> §9[amount]");
		sender.sendMessage(PLUGIN_PREFIX + "/backpacks giveender §6<Player> §9[amount]");
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if(!sender.isOp()) return new ArrayList<>();
		if(args.length == 1) return Arrays.asList("reloadRecipes", "give", "giveender");
		
		
		String subcmd = args[0].toLowerCase();
		if(!subcmd.equals("give") && !subcmd.equals("giveender")) return new ArrayList<>();
		
		if(args.length == 2)
		{
			ArrayList<String> s = new ArrayList<>();
			for(Player p : Bukkit.getOnlinePlayers()) s.add(p.getName());
			return s;
		}
		if(args.length == 3) return Collections.singletonList("[Amount]");
		return new ArrayList<>();
	}
	
	// Helper method to convert SHA-1 hash string to byte array
	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
}
