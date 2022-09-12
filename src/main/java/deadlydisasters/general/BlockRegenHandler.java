package deadlydisasters.general;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

public class BlockRegenHandler {
	
	private static Map<Block,Object[]> blocks = new ConcurrentHashMap<>();
	
	private static int max_blocks_tick;
	
	public BlockRegenHandler(Main plugin) {
		reload(plugin);
		plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
			@Override
			public void run() {
				if (blocks.isEmpty())
					return;
				Iterator<Entry<Block, Object[]>> it = blocks.entrySet().iterator();
				int updates = 0;
				while (it.hasNext()) {
					Entry<Block, Object[]> entry = it.next();
					Block b = entry.getKey();
					Object[] data = entry.getValue();
					b.setBlockData((BlockData) data[0]);
					if (data[1] != null)
						((BlockState) data[1]).update();
					if (updates++ >= max_blocks_tick)
						break;
				}
			}
		}, 0, 1);
	}
	public static void reload(Main plugin) {
		max_blocks_tick = plugin.getConfig().getInt("general.max_regen_blocks_per_tick");
	}
	public static void queueBlockRegen(Block block, Object[] data) {
		blocks.put(block, data);
	}
}
