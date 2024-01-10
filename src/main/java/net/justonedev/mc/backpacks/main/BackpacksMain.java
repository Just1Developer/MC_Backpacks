package net.justonedev.mc.backpacks.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BackpacksMain extends JavaPlugin {
	
	public static BackpacksMain main;
	
	@Override
	public void onEnable() {
		main = this;
		Bukkit.getPluginManager().registerEvents(new Backpacks(), this);
		if (this.getDescription().getVersion().endsWith("DEVBUILD"))
			Bukkit.getLogger().warning("[" + this.getDescription().getPrefix() + "] Please note that this is a developer build and could contain bugs or untested features.");
	}
	
	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
}
