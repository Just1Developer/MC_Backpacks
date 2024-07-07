package net.justonedev.mc.backpacks.main;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BackpacksMain extends JavaPlugin implements Listener {
	
	public static BackpacksMain main;
	public static Backpacks backpacks;
	public static String prefix = "&7[&6Backpacks&7] &e";
	public static int recipeLoadDelayTicks = 10;
	
	static boolean useResourcePack = true;
	private static final String RESOURCE_PACK_URL = "https://www.dropbox.com/scl/fi/238l79hi64kbh0v24j8oc/bColored-Backpacks-1.20.zip?rlkey=zg0jt6bb05chn4stqme5v1ybk&st=by8p5wl3&dl=1";
	private static final UUID RESOURCE_PACK_UUID = UUID.randomUUID();
	private static final String RESOURCE_PACK_SHA1_STR = "ad9f8ab80e9a14c9eecb78a196d89622669d47e1";
	private static final byte[] RESOURCE_PACK_SHA1 = hexStringToByteArray(RESOURCE_PACK_SHA1_STR);
	// Generated using [Windows: certutil -hashfile "§9Tardis Pack.zip" SHA1   MacOS: shasum -a 1 "§9Tardis Pack.zip"]
	
	@Override
	public void onEnable() {
		main = this;
		if (this.getDescription().getVersion().toLowerCase().contains("dev"))
			Bukkit.getLogger().warning("[" + this.getDescription().getPrefix() + "] Please note that this is a developer build and could contain bugs or untested features.");
		
		backpacks = new Backpacks();
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getPluginManager().registerEvents(backpacks, this);
		Bukkit.getPluginManager().registerEvents(new BackpackColor(), this);
		this.getCommand("backpacks").setTabCompleter(this);
		this.getCommand("backpacks").setExecutor(this);
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> backpacks.reloadRecipes(), 10);
	}
	
	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (useResourcePack) event.getPlayer().addResourcePack(RESOURCE_PACK_UUID, RESOURCE_PACK_URL, RESOURCE_PACK_SHA1, "This server uses a resouce pack for (colored) backpacks. Usage is recommended.", false);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof ConsoleCommandSender) && !sender.isOp())
		{
			sender.sendMessage(prefix + "§cYou don't have permission for that!");
			return true;
		}
		if(args.length < 1 || args.length > 3)
		{
			sendHelp(sender);
			return true;
		}
		
		String subcmd = args[0].toLowerCase();
		switch(subcmd)
		{
			case "reloadrecipes":
				backpacks.reloadRecipes();
				if(args.length != 1)
				{
					sender.sendMessage(prefix + "/backpacks reloadRecipes §c<no other arguments here>");
					sender.sendMessage(prefix + "Recipes reloaded anyway.");
				}
				else sender.sendMessage(prefix + "Recipes reloaded.");
				break;
			case "giveender":
			case "give":
				int amount = 1;
				if(args.length == 1)
				{
					sender.sendMessage(prefix + "/backpacks " + subcmd + " §6<Player> §9[amount]");
					return false;
				}
				if(args.length == 3)
				{
					try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored)
					{ sender.sendMessage(prefix + "/backpacks " + subcmd + " §6<Player> §9[amount]§e. §cThe amount should be an integer though :)"); return false; }
				}
				Player p = Bukkit.getPlayer(args[1]);
				if(p == null)
				{
					sender.sendMessage(prefix + "§cSorry, but §6" + args[1] + "§c isn't online right now.");
					return true;
				}
				ItemStack t;
				if(subcmd.length() == 4) t = backpacks.backpackItem.clone();
				else t = backpacks.enderbackpackItem.clone();
				t.setAmount(amount);
				p.getInventory().addItem(t);
				if(subcmd.length() == 4) sender.sendMessage(prefix + "Gave " + amount + " Backpack" + (amount == 1 ? "" : "s") + " to §c" + p.getName());
				else sender.sendMessage(prefix + "Gave " + amount + " Ender-Backpack" + (amount == 1 ? "" : "s") + " to §c" + p.getName());
				break;
			default:
				sendHelp(sender);
		}
		return true;
	}
	
	void sendHelp(CommandSender sender)
	{
		sender.sendMessage(prefix + "§cUnknown command. Here's some help:");
		sender.sendMessage(prefix + "/backpacks reloadRecipes");
		sender.sendMessage(prefix + "/backpacks give §6<Player> §9[amount]");
		sender.sendMessage(prefix + "/backpacks giveender §6<Player> §9[amount]");
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
