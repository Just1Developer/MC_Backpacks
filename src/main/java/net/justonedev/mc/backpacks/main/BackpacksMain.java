package net.justonedev.mc.backpacks.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BackpacksMain extends JavaPlugin {
	
	public static BackpacksMain main;
	
	@Override
	public void onEnable() {
		main = this;
		Bukkit.getPluginManager().registerEvents(new Backpacks(), this);
	}
	
	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
}
