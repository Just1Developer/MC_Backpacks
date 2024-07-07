package net.justonedev.mc.backpacks.main;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BackpacksMain extends JavaPlugin {
	
	public static BackpacksMain main;
	public static Backpacks backpacks;
	public static String prefix = "&7[&6Backpacks&7] &e";
	public static int recipeLoadDelayTicks = 10;
	
	@Override
	public void onEnable() {
		main = this;
		backpacks = new Backpacks();
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
}
