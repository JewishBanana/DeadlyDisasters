package deadlydisasters.listeners;

import java.util.Random;

import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Main;

public class LootGenerateListener implements Listener {
	
	private Random rand = new Random();
	
	public LootGenerateListener(Main plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onLootGen(LootGenerateEvent e) {
		if (e.getInventoryHolder() instanceof Chest) {
			if (rand.nextDouble()*100 < ItemsHandler.basicBookDroprate) {
				e.getLoot().add(ItemsHandler.basicBook);
			}
		}
	}
}
