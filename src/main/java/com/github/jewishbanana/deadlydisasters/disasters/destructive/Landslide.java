package com.github.jewishbanana.deadlydisasters.disasters.destructive;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.listeners.BlockRegenHandler;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.mojang.datafixers.util.Pair;

public class Landslide extends Disaster implements Listener {
	
	private static final Set<Material> allowedBlocks = new HashSet<>(Arrays.asList(Material.GRASS_BLOCK, Material.DIRT, Material.DIRT_PATH, Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.SNOW_BLOCK, Material.STONE, Material.GRAVEL, Material.PACKED_ICE, Material.ICE, Material.BLUE_ICE, Material.POWDER_SNOW));
	
	private int minHeight;
	private double size;
	private double damage;
	private int maxBlocks;
	
	private int depth;
	private int maxOffset;
	private boolean isAvalanche;
	private Vector direction;
//	private BlockData[] materials;
	private final Map<UUID, Integer> fallingBlocks = new ConcurrentHashMap<>();
	private final Map<UUID, Pair<Location, BlockData>> fallingBlocksLocations = new HashMap<>();
	private final Map<UUID, Block> initialSpot = new HashMap<>();
	private final Map<UUID, ItemStack[]> invBlocks = new HashMap<>();
	/**
	 * Ensure regeneration tracking stays in sync when a falling block entity is replaced
	 * (for example, when we bounce or respawn it). This prevents the regen system from
	 * thinking the original UUID has finished and restoring the source location while
	 * the block is still displaced elsewhere.
	 */
	private void transferFallingBlockTracking(UUID oldId, FallingBlock newEntity) {
		BlockRegenHandler.replaceFallingBlockWithNew(oldId, newEntity.getUniqueId());
		fallingBlocksLocations.remove(oldId);
		fallingBlocksLocations.put(newEntity.getUniqueId(), Pair.of(newEntity.getLocation(), newEntity.getBlockData()));
		Block origin = initialSpot.remove(oldId);
		if (origin != null)
			initialSpot.put(newEntity.getUniqueId(), origin);
		ItemStack[] contents = invBlocks.remove(oldId);
		if (contents != null)
			invBlocks.put(newEntity.getUniqueId(), contents);
	}
	
	public Landslide(@NotNull Location location, Player player, int level) {
		super(location, player, level);
		
		this.minHeight = getConfigInt("minimum_height");
	}
	public void init() {
		super.init();
		this.size = getConfigDouble("size");
		this.damage = getConfigDouble("damage");
		this.maxBlocks = getConfigInt("max_falling_blocks");
				
		switch (level) {
		default:
		case 1:
			disasterRange = 9;
			depth = 1;
			maxOffset = 6;
			break;
		case 2:
			disasterRange = 13;
			depth = 2;
			maxOffset = 8;
			break;
		case 3:
			disasterRange = 18;
			depth = 3;
			maxOffset = 10;
			break;
		case 4:
			disasterRange = 23;
			depth = 4;
			maxOffset = 12;
			break;
		case 5:
			disasterRange = 30;
			depth = 5;
			maxOffset = 15;
			break;
		case 6:
			disasterRange = 50;
			depth = 6;
			maxOffset = 25;
			break;
		}
		disasterRange *= size;
	}
	public Location findPossiblePosition(Location initial) {
		for (int t=0; t < 5; t++) {
			Location newTemp = initial.clone().add(0, random.nextInt(level * 4) + 10, 0);
			if (newTemp.getBlock().getType() != Material.AIR || newTemp.getY() < minHeight)
				continue;
			for (int i=0; i < 10; i++) {
				Vector vec = new Vector(random.nextFloat(-.5f, .5f), random.nextFloat(-.4f, .1f), random.nextFloat(-.5f, .5f)).normalize().multiply(0.8);
				Location cast = newTemp.clone().add(vec);
				for (int d=0; d < 30; d++) {
					if (!isPassableOrSnow(cast.getBlock()) && !Tag.LEAVES.isTagged(cast.getBlock().getType())) {
						if (cast.getBlockY() <= initial.getBlockY() + 5 || !allowedBlocks.contains(cast.getBlock().getType()))
							break;
						int count = 0, snowCount = 0;
						label:
						for (int c=0; c < 30; c++) {
							Vector angle = new Vector(vec.getX() + random.nextFloat(-.5f, .5f), vec.getY() + random.nextFloat(-.25f, .25f), vec.getZ() + random.nextFloat(-.5f, .5f)).normalize().multiply(0.8);
							Location check = newTemp.clone().add(angle);
							for (int a=0; a < d+6; a++) {
								if (!isPassableOrSnow(check.getBlock()) && !Tag.LEAVES.isTagged(check.getBlock().getType())) {
									if (!allowedBlocks.contains(check.getBlock().getType()))
										break;
									if (a >= d-6) {
										count++;
										if (check.getBlock().getType() == Material.SNOW_BLOCK || check.getBlock().getType() == Material.POWDER_SNOW)
											snowCount++;
										if (count >= 20) {
											if (snowCount >= 10 && check.getBlock().getTemperature() < 0.15)
												isAvalanche = true;
											break label;
										}
									}
									break;
								}
								check.add(angle);
							}
						}
						if (count >= 20) {
							return cast;
						}
						break;
					}
					cast.add(vec);
				}
			}
		}
		return null;
	}
	public boolean canStart() {
		if (getLocation().getBlockY() < minHeight)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		final World world = location.getWorld();
		final Location towards = location.clone().add(Utils.getRandomizedVector().multiply(15.0));
		final Map<Block, Double> blocks = new HashMap<>();
		final BlockVector block = new BlockVector(location.getX(), location.getY(), location.getZ());
		for (double x = -disasterRange; x < disasterRange; x++)
			for (double z = -disasterRange; z < disasterRange; z++) {
				Vector position = block.clone().add(new Vector(x, 0, z));
				if (block.distance(position) > disasterRange)
					continue;
				Block b = world.getBlockAt(position.toLocation(world));
				if (isPassableOrSnow(b))
					for (int i=0; i < maxOffset; i++) {
						b = b.getRelative(BlockFace.DOWN);
						if (!isPassableOrSnow(b))
							break;
					}
				else
					for (int i=0; i < maxOffset; i++) {
						b = b.getRelative(BlockFace.UP);
						if (isPassableOrSnow(b)) {
							b = b.getRelative(BlockFace.DOWN);
							break;
						}
					}
				if (isPassableOrSnow(b))
					continue;
				blocks.put(b, b.getLocation().distanceSquared(towards));
			}
		direction = Utils.getVectorTowards(location, towards);
		scheduleTask(new BukkitRunnable() {
			private final Set<Block> spots = new LinkedHashSet<>(Utils.sortByValue(blocks).keySet());
			private final Set<Block> next = new LinkedHashSet<>();
			private final int tickBatch = Math.min(maxBlocks, spots.size() / 20);
			private Iterator<Block> it = spots.iterator();
			private final double soundDistance = (level*20) * (level*20);
			private final double vol = 1.0 / soundDistance;
			private int soundTick;
			private int tick;
			private int currentDepth;
			private int wrapUpTicks;
			
			@Override
			public void run() {
				if (soundTick++ == 5) {
					soundTick = 0;
					for (Player p : world.getPlayers()) {
						Location pLoc = p.getEyeLocation();
						double dist = pLoc.distanceSquared(location);
						if (dist <= soundDistance) {
							if (!isAvalanche)
								playSound(p, pLoc.add(Utils.getVectorTowards(pLoc, location).multiply(4.0)), random.nextInt(2) == 0 ? Sound.BLOCK_GRAVEL_BREAK : Sound.BLOCK_GRASS_BREAK, SoundCategory.AMBIENT, (float) (vol * (soundDistance - dist)), .5f);
							else
								playSound(p, pLoc.add(Utils.getVectorTowards(pLoc, location).multiply(4.0)), random.nextInt(2) == 0 ? Sound.BLOCK_SNOW_BREAK : Sound.BLOCK_SAND_BREAK, SoundCategory.AMBIENT, (float) (vol * (soundDistance - dist)), .5f);
						}
					}
				}
				Iterator<Entry<UUID, Integer>> blocksIterator = fallingBlocks.entrySet().iterator();
				while (blocksIterator.hasNext()) {
					Entry<UUID, Integer> entry = blocksIterator.next();
					UUID uuid = entry.getKey();
					Entity entity = Bukkit.getEntity(uuid);
					if (entity == null || entity.isDead()) {
						FallingBlock fb = location.getWorld().spawnFallingBlock(fallingBlocksLocations.get(uuid).getFirst(), fallingBlocksLocations.get(uuid).getSecond());
						fb.setHurtEntities(false);
						fb.setDropItem(false);
						EntityUtils.markFallingBlock(fb);
						fb.setVelocity(direction.clone().add(new Vector(random.nextFloat(-.125f, .125f), 0, random.nextFloat(-.125f, .125f))).multiply(random.nextFloat(.4f, .65f)).setY(0.3));
						if (!isAvalanche)
							playSound(fb.getLocation(), random.nextInt(2) == 0 ? Sound.BLOCK_GRAVEL_BREAK : Sound.BLOCK_GRASS_BREAK, SoundCategory.AMBIENT, 1f, .5f);
						else
							playSound(fb.getLocation(), random.nextInt(2) == 0 ? Sound.BLOCK_SNOW_BREAK : Sound.BLOCK_SAND_BREAK, SoundCategory.AMBIENT, 1f, .5f);
						int bounces = entry.getValue();
						transferFallingBlockTracking(uuid, fb);
						if (bounces > 0)
							fallingBlocks.put(fb.getUniqueId(), bounces - 1);
						blocksIterator.remove();
						continue;
					} else {
						fallingBlocksLocations.replace(entity.getUniqueId(), Pair.of(entity.getLocation(), ((FallingBlock) entity).getBlockData()));
						Vector vel = entity.getVelocity().multiply(0.05).setY(0);
						for (Entity e : entity.getNearbyEntities(1.8, 1.8, 1.8)) {
							if (e instanceof FallingBlock || isEntityProtected(e))
								continue;
							if (e.getVelocity().distanceSquared(vel) < 0.28)
								e.setVelocity(e.getVelocity().add(vel));
							if (e instanceof LivingEntity alive && alive.getNoDamageTicks() == 0 && !EntityUtils.isEntityImmunePlayer(alive))
								EntityUtils.damageEntity(alive, damage, "deaths." + (isAvalanche ? "avalanche" : "landslide"), DamageCause.FALLING_BLOCK);
						}
					}
				}
				if (wrapUpTicks > 0) {
					if (wrapUpTicks++ > 200 || fallingBlocks.isEmpty()) {
						cancel();
						fallingBlocks.forEach((k, v) -> {
							if (Bukkit.getEntity(k) != null)
								Bukkit.getEntity(k).remove();
						});
						stop();
//						Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
					}
					return;
				}
				for (int i=tick; i < tick+tickBatch; i++) {
					if (fallingBlocks.size() >= maxBlocks) {
						tick += i-tick;
						return;
					}
					if (!it.hasNext()) {
						tick = 0;
						currentDepth++;
						spots.clear();
						spots.addAll(next);
						next.clear();
						it = spots.iterator();
						if (currentDepth >= depth)
							wrapUpTicks = 1;
						break;
					}
					Block b = it.next();
					for (int c=0; c < (random.nextInt(4) == 0 ? 2 : 1); c++) {
						if (isPassableOrSnow(b))
							break;
//						BlockData mat = b.getBlockData();
//						if (materials != null)
//							mat = materials[random.nextInt(materials.length)];
						FallingBlock fb = convertBlockIntoFallingBlock(b);
						if (fb == null)
							break;
						fb.setHurtEntities(false);
						fb.setDropItem(false);
						fb.setVelocity(direction.clone().add(new Vector(random.nextFloat(-.125f, .125f), 0, random.nextFloat(-.125f, .125f))).multiply(random.nextFloat(.25f, .5f)).setY(0.1));
						fallingBlocks.put(fb.getUniqueId(), random.nextInt(5));
						fallingBlocksLocations.put(fb.getUniqueId(), Pair.of(fb.getLocation(), fb.getBlockData()));
						initialSpot.put(fb.getUniqueId(), b);
						b = b.getRelative(BlockFace.DOWN);
					}
					next.add(b);
				}
				tick += tickBatch;
			}
		}.runTaskTimer(plugin, 0, 1));
	}
	public void clean() {
		super.clean();
		HandlerList.unregisterAll(this);
	}
	private boolean isPassableOrSnow(Block block) {
		return block.isPassable() && block.getType() != Material.POWDER_SNOW;
	}
	protected String getConfigPath() {
		return "disasters.destructive.landslide";
	}
	public String getDisplayName() {
		return Utils.convertString(DataUtils.getLanguageString(isAvalanche ? "disasters.destructive.avalanche" : getConfigPath()));
	}
	public Set<Environment> getBannedEnvironments() {
		return EnumSet.of(Environment.NETHER, Environment.THE_END);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onBlockLand(EntityChangeBlockEvent event) {
		if (!fallingBlocks.containsKey(event.getEntity().getUniqueId()))
			return;
		Entity current = event.getEntity();
		UUID uuid = current.getUniqueId();
		event.setCancelled(true);
		BlockData mat = ((FallingBlock) current).getBlockData();
//		if (materials != null)
//			mat = materials[random.nextInt(materials.length)];
		FallingBlock fb = current.getWorld().spawnFallingBlock(current.getLocation(), mat);
		fb.setHurtEntities(false);
		fb.setDropItem(false);
		EntityUtils.markFallingBlock(fb);
		fb.setVelocity(direction.clone().add(new Vector(random.nextFloat(-.125f, .125f), 0, random.nextFloat(-.125f, .125f))).multiply(random.nextFloat(.4f, .65f)).setY(0.3));
		int bounces = fallingBlocks.remove(uuid);
		transferFallingBlockTracking(uuid, fb);
		if (bounces > 0)
			fallingBlocks.put(fb.getUniqueId(), bounces - 1);
		current.remove();
		if (!isAvalanche)
			playSound(fb.getLocation(), random.nextInt(2) == 0 ? Sound.BLOCK_GRAVEL_BREAK : Sound.BLOCK_GRASS_BREAK, SoundCategory.AMBIENT, 1f, .5f);
		else
			playSound(fb.getLocation(), random.nextInt(2) == 0 ? Sound.BLOCK_SNOW_BREAK : Sound.BLOCK_SAND_BREAK, SoundCategory.AMBIENT, 1f, .5f);
	}
}
