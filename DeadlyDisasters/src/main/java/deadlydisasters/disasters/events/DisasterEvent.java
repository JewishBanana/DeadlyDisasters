package deadlydisasters.disasters.events;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import deadlydisasters.disasters.Disaster;

public class DisasterEvent {
	
	public Disaster type;
	public int level;
	
	public static Queue<DisasterEvent> ongoingDisasters = new ArrayDeque<>();
	public static Map<UUID,Map<DisasterEvent,Integer>> countdownMap = new HashMap<>();
	
	public Map<Block,Object[]> testDamagedBlocks = new LinkedHashMap<>();
	
	public void inputPlayerToMap(int seconds, Player p) {
		if (!countdownMap.containsKey(p.getUniqueId()))
			countdownMap.put(p.getUniqueId(), new HashMap<>());
		countdownMap.get(p.getUniqueId()).put(this, seconds);
	}
}
