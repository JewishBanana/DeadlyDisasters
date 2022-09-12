package deadlydisasters.disasters.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import deadlydisasters.disasters.Disaster;
import deadlydisasters.utils.AsyncRepeatingTask;
import deadlydisasters.utils.RepeatingTask;

public class DisasterEvent {
	
	public Disaster type;
	public int level;
	
	public static Queue<DisasterEvent> ongoingDisasters = new ArrayDeque<>();
	public static Map<UUID,Map<DisasterEvent,Integer>> countdownMap = new HashMap<>();
	
	public static Map<RepeatingTask, Map<DisasterEvent, Map<Block,Material[]>>> regeneratingTasks = new HashMap<>();
	public static Map<AsyncRepeatingTask, Map<DisasterEvent, Map<Block,Object[]>>> testTasks = new HashMap<>();
	
	public Map<Block,Material[]> damagedBlocks = new LinkedHashMap<>();
	public Map<Block,BlockData> blocksData = new HashMap<>();
	
	public Map<Block,Object[]> testDamagedBlocks = new LinkedHashMap<>();
	
	public void addBlockToList(Block block, Material[] materials) {
		if (damagedBlocks.containsKey(block)) {
			damagedBlocks.replace(block, new Material[] {damagedBlocks.get(block)[0], materials[1]});
		} else {
			damagedBlocks.put(block, materials);
		}
		blocksData.putIfAbsent(block, block.getBlockData().clone());
	}
	public void reverseList() {
		if (damagedBlocks.size() <= 0)
			return;
		List<Block> blockList = new ArrayList<>(damagedBlocks.keySet());
		Collections.reverse(blockList);
		Map<Block,Material[]> blocks = new LinkedHashMap<>();
		for (Block b : blockList)
			blocks.put(b, damagedBlocks.get(b));
		damagedBlocks.clear();
		damagedBlocks.putAll(blocks);
	}
	public void adjustBlockData(Block block, BlockData blockData) {
		if (blocksData.containsKey(block))
			blocksData.replace(block, blockData.clone());
		else
			blocksData.put(block, blockData.clone());
	}
	public void inputPlayerToMap(int seconds, Player p) {
		if (!countdownMap.containsKey(p.getUniqueId()))
			countdownMap.put(p.getUniqueId(), new HashMap<>());
		countdownMap.get(p.getUniqueId()).put(this, seconds);
	}
}
