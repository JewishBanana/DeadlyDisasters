package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedDevourer;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedWorm;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisasterEvent;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.utils.AsyncRepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class InfestedCaves extends DestructionDisaster implements Listener {
	
	private Random rand;
	private int size,time,maxHeight,threads,delay;
	private BlockData data;
	public Material material;
	private Set<Chunk> chunks = new HashSet<>();
	private Map<Block,Object[]> map = new LinkedHashMap<>();
	private Set<Block> blocks = new HashSet<>();
	private DustOptions dust = new DustOptions(Color.fromRGB(9, 74, 72), 1);
	private Map<Block,BlockData>[] rows;
	private boolean earlyBroadcast,canShriek;
	private double sizeSquared,wardenSpawnChance,spawnPocketChance;
	public LivingEntity warden;
	private Map<UUID, Integer> shriekCooldownMap = new HashMap<>();
	
	private Queue<InfestedDevourer> devourers = new ArrayDeque<>();
	private Queue<InfestedWorm> worms = new ArrayDeque<>();
	private Map<String, EntityType> speciesMap = new HashMap<>();
	
	public Map<UUID, UUID> targetMap = new HashMap<>();

	@SuppressWarnings("unchecked")
	public InfestedCaves(int level, World world) {
		super(level, world);
		this.rand = plugin.random;
		this.volume = configFile.getDouble("infestedcaves.volume");
		switch (level) {
		case 1:
			this.size = (int) (configFile.getDouble("infestedcaves.size") * 20);
			this.wardenSpawnChance = 10.0;
			break;
		case 2:
			this.size = (int) (configFile.getDouble("infestedcaves.size") * 30);
			this.wardenSpawnChance = 20.0;
			break;
		case 3:
			this.size = (int) (configFile.getDouble("infestedcaves.size") * 40);
			this.wardenSpawnChance = 30.0;
			break;
		case 4:
			this.size = (int) (configFile.getDouble("infestedcaves.size") * 50);
			this.wardenSpawnChance = 40.0;
			break;
		case 5:
			this.size = (int) (configFile.getDouble("infestedcaves.size") * 60);
			this.wardenSpawnChance = 50.0;
			break;
		case 6:
			this.size = (int) (configFile.getDouble("infestedcaves.size") * 80);
			this.wardenSpawnChance = 100.0;
			break;
		}
		this.maxHeight = configFile.getInt("infestedcaves.max_height");
		this.threads = configFile.getInt("infestedcaves.threads_for_operation");
		this.delay = configFile.getInt("infestedcaves.start_delay") * 20;
		this.spawnPocketChance = 40 * configFile.getDouble("infestedcaves.mob_spawn_multiplier");
		rows = new Map[size+1];
		if (plugin.mcVersion >= 1.19)
			material = Material.SCULK;
		else
			material = Material.NETHERRACK;
		data = material.createBlockData();
		this.time = configFile.getInt("infestedcaves.time.level "+level);
		
		speciesMap.put("infestedskeleton", EntityType.WITHER_SKELETON);
		speciesMap.put("infestedzombie", EntityType.ZOMBIE);
		speciesMap.put("infestedcreeper", EntityType.CREEPER);
		speciesMap.put("infestedenderman", EntityType.ENDERMAN);
		speciesMap.put("infestedspirit", EntityType.VEX);
		speciesMap.put("infestedtribesman", EntityType.ZOMBIE);
		speciesMap.put("infesteddevourer", EntityType.ZOMBIE);
		speciesMap.put("infestedhowler", EntityType.ZOMBIE);
		speciesMap.put("shadowleech", EntityType.ZOMBIE);
		
		this.type = Disaster.INFESTEDCAVES;
	}
	@Override
	public void start(Location loc, Player player) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, player);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		if (earlyBroadcast)
			delay = 0;
		final World world = loc.getWorld();
		this.loc = loc;
		this.sizeSquared = size * size;
		ongoingDisasters.add(this);
		DeathMessages.infestedcaves.add(this);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
		final BukkitScheduler scheduler = plugin.getServer().getScheduler();
		InfestedCaves instance = this;
		for (int x = (-size)-16; x <= size+16; x+=16)
			for (int y = (-size)-16; y <= size+16; y+=16)
				for (int z = (-size)-16; z <= size+16; z+=16) {
					Vector position = block.clone().add(new Vector(x, y, z));
					Block b = world.getBlockAt(position.toLocation(world));
					if (block.distance(position) > size || chunks.contains(b.getChunk()))
						continue;
					chunks.add(b.getChunk());
				}
		for (Chunk chunk : chunks)
			if (!chunk.isLoaded())
				chunk.load(true);
		for (int i=0; i < rows.length; i++)
			rows[i] = new ConcurrentHashMap<>();
		int[] finishedThreads = {0};
		for (int i=0; i < threads; i++) {
			final int progress = (((size*2)/threads)*i)-size;
			scheduler.runTaskAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					Set<Block> chosen = new HashSet<>();
					int last = progress+((size*2)/threads);
					for (int y = progress; y < last; y++) {
						if (block.getBlockY()+y > maxHeight)
							break;
						for (int x = -size; x < size; x++)
							for (int z = -size; z < size; z++) {
								Vector position = block.clone().add(new Vector(x, y, z));
								int distance = (int) block.distance(position);
								if (distance > size || (distance > size-8 && rand.nextInt((int) (((size-distance)*1.5)+1)) == 0))
									continue;
								Block b = world.getBlockAt(position.toLocation(world));
								if (chosen.contains(b) || b.getType() == Material.AIR || b.isLiquid() || Utils.isZoneProtected(b.getLocation()) || Utils.isBlockImmune(b.getType()))
									continue;
								chosen.add(b);
								if (b.isPassable())
									rows[distance].put(b, Material.AIR.createBlockData());
								else
									rows[distance].put(b, data);
							}
					}
					finishedThreads[0]++;
				}
			});
		}
		int[] tick = {0};
		new RepeatingTask(plugin, 0, 2) {
			@Override
			public void run() {
				for (Chunk chunk : chunks)
					if (!chunk.isLoaded())
						chunk.load(true);
				if (finishedThreads[0] >= threads) {
					cancel();
					if (!earlyBroadcast && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast"))
						Utils.broadcastEvent(level, "destructive", type, loc, p);
					if (rand.nextDouble()*100 < wardenSpawnChance)
						spawnWarden(loc);
					else if (plugin.mcVersion >= 1.19)
						plugin.getServer().getScheduler().runTaskLater(plugin, () -> canShriek = true, 100);
					new RepeatingTask(plugin, delay, 2) {
						@Override
						public void run() {
							if (tick[0] >= size) {
								cancel();
								return;
							}
							for (Entry<Block, BlockData> entry : rows[tick[0]].entrySet()) {
								Block b = entry.getKey();
								map.putIfAbsent(b, new Object[] {b.getBlockData(), b.getState()});
								b.setBlockData(entry.getValue());
								if (b.getType() != Material.AIR)
									blocks.add(b);
							}
							tick[0]++;
						}
					};
				}
			}
		};
		int[] spawning = {0};
		Set<UUID> playedSound = new HashSet<>();
		new RepeatingTask(plugin, 0, 20) {
			@Override
			public void run() {
				if (time <= 0) {
					cancel();
					if (map.isEmpty()) {
						ongoingDisasters.remove(instance);
						DeathMessages.infestedcaves.remove(instance);
						HandlerList.unregisterAll(instance);
						clearEntities();
						return;
					}
					playedSound.clear();
					Map<Block,Object[]> regenMap = Utils.reverseMap(map);
					final int blocksPerTick = regenMap.size() / (size*2);
					new RepeatingTask(plugin, 0, 1) {
						@Override
						public void run() {
							int current = 0;
							Iterator<Entry<Block, Object[]>> it = regenMap.entrySet().iterator();
							Block b = null;
							while (it.hasNext()) {
								Entry<Block, Object[]> entry = it.next();
								b = entry.getKey();
								blocks.remove(b);
								b.setBlockData((BlockData) entry.getValue()[0]);
								if (entry.getValue()[1] != null)
									((BlockState) entry.getValue()[1]).update();
								it.remove();
								current++;
								if (current >= blocksPerTick)
									break;
							}
							if (plugin.mcVersion >= 1.19 && b != null) {
								double dist = b.getLocation().distance(loc)+10;
								for (Entity e : world.getNearbyEntities(new BoundingBox(loc.getBlockX()-dist, loc.getBlockY()-dist, loc.getBlockZ()-dist, loc.getBlockX()+dist, loc.getBlockY()+dist, loc.getBlockZ()+dist), t -> (t instanceof Player)))
									if (e.getLocation().distance(loc) > dist-25 && !playedSound.contains(e.getUniqueId())) {
										((Player) e).playSound(e.getLocation(), Sound.ENTITY_WARDEN_DIG, 2, 0.5F);
										playedSound.add(e.getUniqueId());
									}
							}
							if (regenMap.isEmpty()) {
								ongoingDisasters.remove(instance);
								DeathMessages.infestedcaves.remove(instance);
								HandlerList.unregisterAll(instance);
								clearEntities();
								cancel();
							}
						}
					};
					return;
				}
				if (tick[0] >= size)
					time--;
				if (tick[0] == 0)
					return;
				spawning[0]++;
				for (Entity e : world.getNearbyEntities(new BoundingBox(loc.getBlockX()-tick[0]-10, loc.getBlockY()-tick[0]-10, loc.getBlockZ()-tick[0]-10, loc.getBlockX()+tick[0]+10, loc.getBlockY()+tick[0]+10, loc.getBlockZ()+tick[0]+10), t -> (t instanceof Player))) {
					Player p = (Player) e;
					if (Utils.isPlayerImmune(p))
						continue;
					if (p.getLocation().distance(loc) <= tick[0]) {
						if (spawning[0] % 8 == 0 && rand.nextDouble()*100 < spawnPocketChance)
							spawnPocket(p, 1.8+rand.nextDouble());
						if (spawning[0] % 5 == 0 && rand.nextInt(2) == 0) {
							spawnWorm(p);
							if (plugin.mcVersion >= 1.19)
								switch (rand.nextInt(4)) {
								case 0:
									p.playSound(p.getLocation().add(rand.nextInt(6)-3, rand.nextInt(7)-2, rand.nextInt(6)-3), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 2, rand.nextFloat()+0.5F);
									break;
								case 1:
									p.playSound(p.getLocation().add(rand.nextInt(6)-3, rand.nextInt(7)-2, rand.nextInt(6)-3), Sound.ENTITY_WARDEN_NEARBY_CLOSEST, 2, rand.nextFloat()+0.5F);
									break;
								case 2:
									p.playSound(p.getLocation().add(rand.nextInt(6)-3, rand.nextInt(7)-2, rand.nextInt(6)-3), Sound.ENTITY_WARDEN_AGITATED, 2, 0.5F);
									break;
								case 3:
									p.playSound(p.getLocation().add(rand.nextInt(6)-3, rand.nextInt(7)-2, rand.nextInt(6)-3), Sound.ENTITY_WARDEN_AMBIENT, 2, 0.5F);
									break;
								}
						}
						if (spawning[0] % 8 == 0)
							closeRoute(p, 8);
						if (plugin.mcVersion >= 1.19 && rand.nextInt(3) == 0)
							spawnVibration(p.getEyeLocation(), 1, 9, 5, 10);
					}
					if (tick[0] < size && plugin.mcVersion >= 1.19) {
						if (!playedSound.contains(p.getUniqueId())) {
							p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 2, 0.5F);
							playedSound.add(p.getUniqueId());
						}
						p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SPREAD, 2, (rand.nextFloat()/2)+0.5F);
						scheduler.runTaskLater(plugin, () -> p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SPREAD, 2, (rand.nextFloat()/2)+0.5F), 10);
					}
				}
				for (Map.Entry<UUID, UUID> entry : targetMap.entrySet()) {
					Mob entity = (Mob) Bukkit.getEntity(entry.getKey());
					if (entity == null || entity.isDead())
						return;
					if (entity.getTarget() == null && Bukkit.getEntity(entry.getValue()) == null)
						entity.setTarget((LivingEntity) Bukkit.getEntity(entry.getValue()));
					if (entity.getLocation().distanceSquared(loc) > sizeSquared) {
						entity.setVelocity(Utils.getVectorTowards(entity.getLocation(), loc).multiply(3));
						entity.damage(2);
					}
					if (warden != null)
						((org.bukkit.entity.Warden) warden).setAnger(entity, 0);
				}
				for (InfestedDevourer devourer : devourers) {
					if (devourer.getEntity() == null || devourer.getEntity().isDead())
						return;
					Mob entity = (Mob) devourer.getEntity();
					if (entity.getLocation().distanceSquared(loc) > sizeSquared) {
						entity.setVelocity(Utils.getVectorTowards(entity.getLocation(), loc).multiply(3));
						entity.damage(2);
					}
					if (warden != null)
						((org.bukkit.entity.Warden) warden).setAnger(entity, 0);
				}
				Iterator<Entry<UUID, Integer>> it = shriekCooldownMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
			}
		};
	}
	public void shriekEvent(LivingEntity target, boolean notifyWarden) {
		if (notifyWarden && canShriek) {
			if ((warden == null && rand.nextInt(3) == 0) || (warden != null && !loc.getWorld().getNearbyEntities(new BoundingBox(loc.getBlockX()-size-15, loc.getBlockY()-size-15, loc.getBlockZ()-size-15, loc.getBlockX()+size+15, loc.getBlockY()+size+15, loc.getBlockZ()+size+15), t -> (t instanceof Player))
				.stream().anyMatch(e -> ((org.bukkit.entity.Warden) warden).getAnger(e) >= 80))) {
				if (warden != null) {
					for (Entity entities : warden.getNearbyEntities(25, 25, 25))
						((org.bukkit.entity.Warden) warden).setAnger(entities, 0);
					Utils.mergeEntityData(warden, "{Brain:{memories:{\"minecraft:dig_cooldown\":{ttl:0L,value:{}},\"minecraft:sniff_cooldown\":{ttl:200L,value:{}},\"minecraft:vibration_cooldown\":{ttl:200L,value:{}}}}}");
					canShriek = false;
				}
				new RepeatingTask(plugin, 100, 10) {
					@Override
					public void run() {
						if (time <= 0 || !target.getWorld().equals(loc.getWorld()) || target.getLocation().distanceSquared(loc) > sizeSquared) {
							cancel();
							return;
						}
						if (warden == null || warden.isDead()) {
							spawnWarden(Utils.getSpotInSquareRadius(target.getLocation(), 5));
							((org.bukkit.entity.Warden) warden).setAnger(target, 60);
							cancel();
						}
					}
				};
			}
		} else if (warden != null)
			((org.bukkit.entity.Warden) warden).increaseAnger(target, 50);
		if (target instanceof Player && !shriekCooldownMap.containsKey(target.getUniqueId())) {
			spawnPocket(target, size);
			shriekCooldownMap.put(target.getUniqueId(), 8);
			if (plugin.mcVersion >= 1.19) {
				target.getWorld().playSound(Utils.getSpotInSquareRadius(target.getLocation(), 8), Sound.ENTITY_WARDEN_ROAR, 2, 0.5F);
				target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, true, false));
				for (Entity e : target.getNearbyEntities(15, 15, 15))
					if (e instanceof Player && !Utils.isPlayerImmune((Player) e))
						((Player) e).addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, true, false));
				int[] amount = {0};
				new AsyncRepeatingTask(plugin, 10, 5) {
					@Override
					public void run() {
						if (target != null)
							spawnVibration(target.getEyeLocation(), 25, 12, 10, 200);
						amount[0]++;
						if (amount[0] >= 8)
							cancel();
					}
				};
			}
		}
	}
	public void spawnVibration(Location entityLoc, int amount, int maxDistance, int maxInternalDistance, int maxFails) {
		int success = 0;
		label:
		for (int i=0; i < maxFails; i++) {
			Location tempLoc = entityLoc.clone();
			Vector vec = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1).normalize();
			for (int c=0; c < rand.nextInt(maxDistance)+1; c++) {
				tempLoc.add(vec);
				if (!tempLoc.getBlock().isPassable())
					if (c < 3)
						continue label;
					else {
						tempLoc.add(vec.clone().multiply(-1));
						break;
					}
			}
			if (!tempLoc.getWorld().equals(loc.getWorld()) || tempLoc.getBlockY() > maxHeight || tempLoc.distanceSquared(loc) > sizeSquared)
				continue;
			Location pos1 = null;
			Vector vecInternal = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1).normalize();
			for (int d=1; d < maxInternalDistance; d++) {
				Block tempBlock = tempLoc.clone().add(vecInternal.clone().multiply(d)).getBlock();
				if (!tempBlock.isPassable() && blocks.contains(tempBlock)) {
					pos1 = tempLoc.clone().add(vecInternal.clone().multiply(d));
					break;
				}
			}
			if (pos1 == null)
				break;
			Location pos2 = null;
			for (int d=1; d < maxInternalDistance; d++) {
				Block tempBlock = tempLoc.clone().add(vecInternal.clone().multiply(-d)).getBlock();
				if (!tempBlock.isPassable() && blocks.contains(tempBlock)) {
					pos2 = tempLoc.clone().add(vecInternal.clone().multiply(-d));
					break;
				}
			}
			if (pos2 == null || pos1.distanceSquared(pos2) <= 3)
				break;
			tempLoc.getWorld().spawnParticle(Particle.VIBRATION, pos1, 1, 0, 0, 0, 1, new org.bukkit.Vibration(pos1, new org.bukkit.Vibration.Destination.BlockDestination(pos2), rand.nextInt(10)+10));
			final Location soundPos = pos1;
			plugin.getServer().getScheduler().runTask(plugin, () -> tempLoc.getWorld().playSound(soundPos, Sound.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.BLOCKS, 0.5F, (rand.nextFloat()/2)+0.5F));
			success++;
			if (success >= amount)
				break;
		}
	}
	public void spawnPocket(LivingEntity p, double pocketSize) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Location playerLoc = p.getEyeLocation();
				final World world = p.getWorld();
				label:
				for (int i=0; i < 20; i++) {
					Vector vec = new Vector((rand.nextDouble()*2)-1, 0, (rand.nextDouble()*2)-1).normalize().setY(rand.nextDouble());
					Location tempLoc = playerLoc.clone().add(vec);
					for (int d=0; d < 15; d++) {
						if (!tempLoc.getBlock().isPassable())
							if (tempLoc.getBlock().getType() == material) {
								final Location opening = tempLoc.clone();
								for (int c=0; c < pocketSize*2; c++) {
									tempLoc.add(vec);
									if (tempLoc.getBlock().getType() != material)
										continue label;
								}
								tempLoc.add(vec.clone().multiply(-pocketSize));
								Block tempBlock = tempLoc.getBlock();
								if (tempBlock.getType() != material || tempBlock.getRelative(BlockFace.DOWN).getType() != material || tempBlock.getRelative(BlockFace.UP).getType() != material
										|| tempBlock.getRelative(BlockFace.NORTH).getType() != material || tempBlock.getRelative(BlockFace.EAST).getType() != material
										|| tempBlock.getRelative(BlockFace.SOUTH).getType() != material || tempBlock.getRelative(BlockFace.WEST).getType() != material)
									continue label;
								final Location mainLocation = tempLoc.clone();
								BlockVector block = new BlockVector(tempLoc.getX(), tempLoc.getY(), tempLoc.getZ());
								Queue<Block> toChange = new ArrayDeque<>();
								for (double x=-pocketSize; x < pocketSize; x += 0.5)
									for (double y=-pocketSize; y < pocketSize; y += 0.5)
										for (double z=-pocketSize; z < pocketSize; z += 0.5) {
											Vector position = block.clone().add(new Vector(x, y, z));
											if (block.distance(position) > pocketSize)
												continue;
											Block b = world.getBlockAt(position.toLocation(world));
											if (b.getType() == material && b.getRelative(BlockFace.DOWN).getType() == material && b.getRelative(BlockFace.UP).getType() == material
													&& b.getRelative(BlockFace.NORTH).getType() == material && b.getRelative(BlockFace.EAST).getType() == material
													&& b.getRelative(BlockFace.SOUTH).getType() == material && b.getRelative(BlockFace.WEST).getType() == material)
												toChange.add(b);
										}
								Location subLoc = tempLoc.clone().add(new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1).normalize().multiply(pocketSize));
								for (int sub=0; sub < 15; sub++) {
									Block test = tempLoc.clone().add(new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1, (rand.nextDouble()*2)-1).normalize().multiply(pocketSize)).getBlock();
									if (toChange.contains(test)) {
										subLoc = test.getLocation().add(.5,.5,.5);
										break;
									}
								}
								final Location subLocation = subLoc.clone();
								block = new BlockVector(subLoc.getX(), subLoc.getY(), subLoc.getZ());
								double subPocket = (rand.nextDouble()*(pocketSize/2))+(pocketSize/2);
								for (double x=-subPocket; x < subPocket; x += 0.5)
									for (double y=-subPocket; y < subPocket; y += 0.5)
										for (double z=-subPocket; z < subPocket; z += 0.5) {
											Vector position = block.clone().add(new Vector(x, y, z));
											if (block.distance(position) > subPocket)
												continue;
											Block b = world.getBlockAt(position.toLocation(world));
											if (!toChange.contains(b) && b.getType() == material && b.getRelative(BlockFace.DOWN).getType() == material && b.getRelative(BlockFace.UP).getType() == material
													&& b.getRelative(BlockFace.NORTH).getType() == material && b.getRelative(BlockFace.EAST).getType() == material
													&& b.getRelative(BlockFace.SOUTH).getType() == material && b.getRelative(BlockFace.WEST).getType() == material)
												toChange.add(b);
										}
								final boolean allowCustom = (boolean) WorldObject.findWorldObject(world).settings.get("custom_mob_spawning");
								int value = (int) (pocketSize*2);
								List<String> entities = new ArrayList<>();
								if (allowCustom) {
									int tempValue = 0;
									while (tempValue < value) {
										if (rand.nextInt(2) == 0) {
											switch (rand.nextInt(5)) {
											case 0:
												entities.add("infestedskeleton");
												break;
											case 1:
												entities.add("infestedzombie");
												break;
											case 2:
												entities.add("infestedcreeper");
												break;
											case 3:
												entities.add("infestedenderman");
												break;
											case 4:
												entities.add("infestedspirit");
												break;
											}
											tempValue++;
										} else {
											switch (rand.nextInt(4)) {
											case 0:
												for (int amount=0; amount < 4; amount++)
													entities.add("infestedtribesman");
												tempValue += 2;
												break;
											case 1:
												for (int amount=0; amount < 2; amount++)
													entities.add("infesteddevourer");
												tempValue++;
												break;
											case 2:
												entities.add("infestedhowler");
												tempValue++;
												break;
											case 3:
												entities.add("shadowleech");
												tempValue++;
												break;
											}
										}
									}
								}
								plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
									@Override
									public void run() {
										for (Block b : toChange)
											if (blocks.contains(b))
												b.setType(Material.AIR);
										if (!allowCustom) {
											for (int i=0; i < value/3*2; i++)
												((Mob) world.spawnEntity(mainLocation, EntityType.ZOMBIE)).setTarget(p);
											for (int i=0; i < value/3; i++)
												((Mob) world.spawnEntity(subLocation, EntityType.ZOMBIE)).setTarget(p);
										} else {
											for (int i=entities.size()-1; i >= entities.size()/3; i--) {
												String species = entities.get(i);
												Entity tempEntity = world.spawnEntity(mainLocation, speciesMap.get(species));
												CustomEntity.handler.addEntityBySpecies(species, tempEntity);
												if (species.equals("infesteddevourer"))
													devourers.add((InfestedDevourer) CustomEntity.handler.findEntity((LivingEntity) tempEntity));
												else
													targetMap.put(tempEntity.getUniqueId(), p.getUniqueId());
												((Mob) tempEntity).setTarget(p);
												entities.remove(i);
											}
											for (String species : entities) {
												Entity tempEntity = world.spawnEntity(subLocation, speciesMap.get(species));
												CustomEntity.handler.addEntityBySpecies(species, tempEntity);
												if (species.equals("infesteddevourer"))
													devourers.add((InfestedDevourer) CustomEntity.handler.findEntity((LivingEntity) tempEntity));
												else
													targetMap.put(tempEntity.getUniqueId(), p.getUniqueId());
												((Mob) tempEntity).setTarget(p);
											}
										}
										BlockVector door = new BlockVector(opening.getX(), opening.getY(), opening.getZ());
										double[] tick = {0};
										final double maxDoorSize = Math.max(pocketSize, 2);
										new RepeatingTask(plugin, 0, 15) {
											@Override
											public void run() {
												if (tick[0] >= maxDoorSize) {
													cancel();
													return;
												}
												for (double x=-tick[0]; x < tick[0]; x += 0.5)
													for (double y=-tick[0]; y < tick[0]; y += 0.5)
														for (double z=-tick[0]; z < tick[0]; z += 0.5) {
															Vector pos = door.clone().add(new Vector(x, y, z));
															double distance = door.distance(pos);
															if (distance < (tick[0] - 1) || distance > tick[0])
																continue;
															Block b = world.getBlockAt(pos.toLocation(world));
															if (blocks.contains(b))
																b.setType(Material.AIR);
														}
												if (plugin.mcVersion >= 1.19)
													opening.getWorld().playSound(opening, Sound.BLOCK_SCULK_SPREAD, 1, (rand.nextFloat()/2)+0.5F);
												tick[0] += 0.5;
											}
										};
									}
								});
								return;
							} else
								break;;
						tempLoc.add(vec);
					}
				}
			}
		});
	}
	private void spawnWarden(Location location) {
		if (plugin.mcVersion < 1.19)
			return;
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> canShriek = true, 300);
		for (int i=1; i < 30; i++) {
			Location spawn = Utils.findSmartYSpawn(location, location.clone().add((rand.nextInt(i)-(i/2))+1, 0, (rand.nextInt(i)-(i/2))+1), 3, size/2);
			if (spawn == null || spawn.getBlock().getRelative(BlockFace.DOWN).isPassable() || spawn.getBlock().getRelative(BlockFace.DOWN, 2).isPassable() || spawn.getBlock().getRelative(BlockFace.DOWN, 3).isPassable())
				continue;
			warden = (LivingEntity) location.getWorld().spawnEntity(spawn, EntityType.WARDEN);
			warden.setInvisible(true);
			Utils.mergeEntityData(warden, "{Brain:{memories:{\"minecraft:dig_cooldown\":{ttl:"+(time*20)+"L,value:{}},\"minecraft:is_emerging\":{ttl:134L,value:{}}}}}");
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> warden.setInvisible(false), 10);
			break;
		}
	}
	public void spawnWorm(LivingEntity entity) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Location entityLoc = entity.getLocation().add(0,2,0);
				List<BlockFace> blockFaces = new ArrayList<>(Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST));
				for (int i=0; i < 30; i++) {
					Location tempLoc = entityLoc.clone();
					Vector tempVec = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*1.6)-0.8, (rand.nextDouble()*2)-1).normalize();
					for (int c=0; c < 15; c++) {
						tempLoc.add(tempVec);
						Block b = tempLoc.getBlock();
						if (!b.isPassable()) {
							if (c < 7 || !blocks.contains(b))
								break;
							int faceOccupied = 0;
							for (BlockFace bface : blockFaces)
								if (!b.getRelative(bface).isPassable())
									faceOccupied++;
							if (faceOccupied < 3 || faceOccupied > 5)
								break;
							BlockFace faceSpawn = null;
							whileloop:
							for (int faceLoop=0; faceLoop < blockFaces.size(); faceLoop++) {
								BlockFace tempFace = blockFaces.get(rand.nextInt(blockFaces.size()));
								if (b.getRelative(tempFace).isPassable()) {
									for (int air=1; air < 4; air++)
										if (!b.getRelative(tempFace, air).isPassable())
											continue whileloop;
									faceSpawn = tempFace;
									break;
								}
							}
							if (faceSpawn == null)
								break;
							final BlockFace finalFace = faceSpawn;
							plugin.getServer().getScheduler().runTask(plugin, () -> {
								InfestedWorm worm = new InfestedWorm(b, finalFace, plugin, rand);
								CustomEntity.handler.addFalseEntity(worm);
								worms.add(worm);
							});
							return;
						}
					}
				}
			}
		});
	}
	public void closeRoute(LivingEntity entity, int maxDistance) {
		Vector initVec = null;
		for (Entry<UUID, UUID> entry : targetMap.entrySet()) {
			Entity tempEntity = Bukkit.getEntity(entry.getKey());
			if (tempEntity != null && ((Mob) tempEntity).hasLineOfSight(entity)) {
				initVec = Utils.getVectorTowards(Bukkit.getEntity(entry.getKey()).getLocation(), entity.getLocation());
				break;
			}
		}
		if (initVec == null)
			initVec = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*1.6)-0.8, (rand.nextDouble()*2)-1).normalize();
		closeRoute(entity, maxDistance, initVec);
	}
	public void closeRoute(LivingEntity entity, int maxDistance, Vector vec) {
		final Location entityloc = entity.getEyeLocation();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				label:
				for (int i=0; i < 30; i++) {
					Location tempLoc = entityloc.clone();
					Vector tempVec = vec.clone().add(new Vector(rand.nextDouble()*2-1, rand.nextDouble()*2-1, rand.nextDouble()*2-1)).normalize();
					for (int c=0; c < 10; c++) {
						tempLoc.add(tempVec);
						if (!tempLoc.getBlock().isPassable())
							continue label;
					}
					if (!tempLoc.getWorld().equals(loc.getWorld()) || tempLoc.getBlockY() > maxHeight || tempLoc.distanceSquared(loc) > sizeSquared)
						continue;
					@SuppressWarnings("unchecked")
					Set<Block>[] blocksToChange = new Set[maxDistance];
					for (int index=0; index < blocksToChange.length; index++)
						blocksToChange[index] = new HashSet<>();
					Vector right = new Vector(tempVec.getZ(), 0, -tempVec.getX()), up;
					if (tempLoc.getY() > entityloc.getY())
						up = new Vector(-tempVec.getX(), 1, -tempVec.getY());
					else
						up = new Vector(tempVec.getX(), 1, tempVec.getY());
					int[] distance = {0, 0, 0, 0};
					for (int d=1; d < maxDistance; d++) {
						Block tempBlock = tempLoc.clone().add(right.clone().multiply(d)).getBlock();
						if (!tempBlock.isPassable() && blocks.contains(tempBlock)) {
							distance[0] = d;
							break;
						}
					}
					if (distance[0] == 0)
						continue;
					for (int d=1; d < maxDistance; d++) {
						Block tempBlock = tempLoc.clone().add(right.clone().multiply(-d)).getBlock();
						if (!tempBlock.isPassable() && blocks.contains(tempBlock)) {
							distance[1] = d;
							break;
						}
					}
					if (distance[1] == 0)
						continue;
					tempLoc.add(right.clone().multiply(-distance[1])).add(right.clone().multiply((distance[0]+distance[1])/2));
					for (int d=1; d < maxDistance; d++) {
						Block tempBlock = tempLoc.clone().add(up.clone().multiply(d)).getBlock();
						if (!tempBlock.isPassable() && blocks.contains(tempBlock)) {
							distance[2] = d;
							break;
						}
					}
					if (distance[2] == 0)
						continue;
					for (int d=1; d < maxDistance; d++) {
						Block tempBlock = tempLoc.clone().add(up.clone().multiply(-d)).getBlock();
						if (!tempBlock.isPassable() && blocks.contains(tempBlock)) {
							distance[3] = d;
							break;
						}
					}
					if (distance[3] == 0)
						continue;
					tempLoc.add(up.clone().multiply(-distance[3])).add(up.clone().multiply((distance[2]+distance[3])/2));
					for (int x=-maxDistance; x <= maxDistance; x++)
						for (int y=-maxDistance; y <= maxDistance; y++) {
							Block cornerBlock = tempLoc.clone().add(right.clone().multiply(x)).add(up.clone().multiply(y)).getBlock();
							if (blocks.contains(cornerBlock) || Utils.isBlockImmune(cornerBlock.getType()) || Utils.isZoneProtected(cornerBlock.getLocation()))
								continue;
							int index = (int) Math.min(maxDistance-1, cornerBlock.getLocation().distance(tempLoc));
							if (!blocksToChange[index].contains(cornerBlock))
								blocksToChange[index].add(cornerBlock);
						}
					int[] tick = {maxDistance-1};
					final Location centerLoc = tempLoc.clone();
					new RepeatingTask(plugin, 0, 10) {
						@Override
						public void run() {
							if (time <= 0 || tick[0] < 0) {
								cancel();
								return;
							}
							for (Block b : blocksToChange[tick[0]]) {
								map.putIfAbsent(b, new Object[] {b.getBlockData(), b.getState()});
								b.setBlockData(data);
								blocks.add(b);
							}
							if (plugin.mcVersion >= 1.19)
								for (Entity e : centerLoc.getWorld().getNearbyEntities(centerLoc, 15, 15, 15, p -> (p instanceof Player)))
									((Player) e).playSound(e.getLocation(), Sound.BLOCK_SCULK_SPREAD, 2, (rand.nextFloat()/2)+0.5F);
							tick[0]--;
						}
					};
				}
			}
		});
	}
	public void clearEntities() {
		for (UUID uuid : targetMap.keySet()) {
			Entity e = Bukkit.getEntity(uuid);
			if (e != null)
				e.remove();
		}
		for (InfestedDevourer e : devourers)
			if (e.getEntity() != null)
				e.getEntity().remove();
		if (warden != null && !warden.isDead()) {
			for (Entity entities : warden.getNearbyEntities(20, 20, 20))
				((org.bukkit.entity.Warden) warden).setAnger(entities, 0);
			Utils.mergeEntityData(warden, "{Brain:{memories:{\"minecraft:dig_cooldown\":{ttl:0L,value:{}},\"minecraft:sniff_cooldown\":{ttl:200L,value:{}},\"minecraft:vibration_cooldown\":{ttl:200L,value:{}}}}}");
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
				if (warden != null && !warden.isDead())
					warden.remove();
			}, 160);
		}
		for (InfestedWorm worm : worms) {
			worm.clean();
			worm.shouldRemove = true;
		}
	}
	@Override
	public Location findApplicableLocation(Location temp, Player p) {
		if (temp.getBlockY()+15 > configFile.getInt("corruptedcaves.max_height"))
			return null;
		return temp;
	}
	@Override
	public void startAdjustment(Location loc, Player p) {
		start(loc, p);
	}
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if (blocks.contains(e.getBlock())) {
			e.setCancelled(true);
			Block b = e.getBlock();
			b.breakNaturally(new ItemStack(Material.AIR));
			b.getWorld().spawnParticle(VersionUtils.getRedstoneDust(), b.getLocation().add(.5,.5,.5), 30, .3, .3, .3, 0.1, dust);
			if (plugin.mcVersion >= 1.19) {
				Location middleBlock = b.getLocation().add(.5,.5,.5);
				b.getWorld().playSound(middleBlock, Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 2, (rand.nextFloat()/2)+0.5F);
				Vector vec = Utils.getVectorTowards(middleBlock, e.getPlayer().getEyeLocation());
				label:
				for (int i=0; i < 10; i++) {
					Location blockLoc = middleBlock.clone();
					Vector tempVec = vec.clone().add(new Vector(rand.nextDouble()*2-1, rand.nextDouble()*2-1, rand.nextDouble()*2-1)).normalize();
					for (int c=0; c < 10; c++) {
						blockLoc.add(tempVec);
						if (!blockLoc.getBlock().isPassable()) {
							if (c < 2 || !blocks.contains(blockLoc.getBlock()))
								continue label;
							b.getWorld().spawnParticle(Particle.VIBRATION, middleBlock, 1, 0, 0, 0, 1, new org.bukkit.Vibration(middleBlock, new org.bukkit.Vibration.Destination.BlockDestination(blockLoc.getBlock().getLocation().add(.5,.5,.5)), rand.nextInt(10)+5));
							break label;
						}
					}
				}
			}
			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					if (Utils.isBlockImmune(b.getType()))
						blocks.remove(b);
					else if (blocks.contains(b))
						b.setBlockData(data);
					if (plugin.mcVersion >= 1.19)
						b.getWorld().playSound(b.getLocation(), Sound.BLOCK_SCULK_SPREAD, 1, (rand.nextFloat()/2)+0.5F);
				}
			}, 20);
			if (rand.nextInt(6) == 0 && !Utils.isPlayerImmune(e.getPlayer()))
				shriekEvent(e.getPlayer(), true);
				
		}
	}
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if (e.getPlayer().getWorld().equals(loc.getWorld()) && e.getBlock().getLocation().distanceSquared(loc) <= sizeSquared && e.getBlock().getLocation().getBlockY() <= maxHeight && !Utils.isBlockImmune(e.getBlock().getType())) {
			InfestedDevourer.blockTargets.add(e.getBlock().getLocation());
			if (rand.nextInt(7) == 0)
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
					@Override
					public void run() {
						Block block = Utils.rayCastForBlock(e.getPlayer().getLocation().add(0,1,0), 6, 10, 30, null, blocks);
						if (block == null)
							return;
						Vector vec = Utils.getVectorTowards(block.getLocation().add(.5,.5,.5), e.getPlayer().getLocation().add(0,1.5,0));
						plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
							@Override
							public void run() {
								if (time > 0) {
									Mob entity = (Mob) block.getLocation().add(.5,.5,.5).getWorld().spawnEntity(loc, EntityType.ZOMBIE);
									InfestedDevourer devourer = new InfestedDevourer((Zombie) entity, plugin, rand);
									CustomEntity.handler.addEntity(devourer);
									entity.setVelocity(vec);
									devourers.add(devourer);
									devourer.updateTarget();
								}
							}
						});
					}
				});
			for (InfestedDevourer entity : devourers)
				entity.updateTarget();
		}
	}
	@EventHandler
	public void onBlockExplode(EntityExplodeEvent e) {
		Map<Block,Double> tempBlocks = new HashMap<>();
		Location location = e.getEntity().getLocation();
		for (Block b : e.blockList())
			if (blocks.contains(b))
				tempBlocks.put(b, b.getLocation().distanceSquared(location));
		if (tempBlocks.isEmpty())
			return;
		Queue<Block> blocksToChange = new ArrayDeque<>(Utils.reverseMap(Utils.sortByValue(tempBlocks)).keySet());
		new RepeatingTask(plugin, 20, 1) {
			@Override
			public void run() {
				for (int i=0; i < 3; i++) {
					Block b = blocksToChange.iterator().next();
					if (blocks.contains(b) && !Utils.isBlockImmune(b.getType()))
						b.setBlockData(data);
					blocksToChange.remove(b);
					if (rand.nextInt(8) == 0 && plugin.mcVersion >= 1.19)
						b.getWorld().playSound(b.getLocation(), Sound.BLOCK_SCULK_SPREAD, 1, (rand.nextFloat()/2)+0.5F);
					if (blocksToChange.isEmpty()) {
						cancel();
						return;
					}
				}
			}
		};
		for (Entity near : e.getEntity().getNearbyEntities(12, 12, 12))
			if (near instanceof Player && !Utils.isPlayerImmune((Player) near)) {
				shriekEvent((LivingEntity) near, true);
				break;
			}
		if (plugin.mcVersion >= 1.19)
			e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 2, 0.5F);
	}
	public boolean isEarlyBroadcast() {
		return earlyBroadcast;
	}
	public void setEarlyBroadcast(boolean earlyBroadcast) {
		this.earlyBroadcast = earlyBroadcast;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public Material getMaterial() {
		return material;
	}
	public void setMaterial(Material material) {
		this.material = material;
	}
	public double getSizeSquared() {
		return sizeSquared;
	}
	public void setSizeSquared(double sizeSquared) {
		this.sizeSquared = sizeSquared;
	}
}