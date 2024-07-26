package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisasterEvent;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.listeners.CoreListener;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.utils.ChannelDataHolder;
import com.github.jewishbanana.deadlydisasters.utils.Metrics;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.mojang.datafixers.util.Pair;

public class LandSlide extends DestructionDisaster implements Listener {
	
	private int radius,depth,maxOffset,maxBlocks;
	private BlockData[] materials;
	private double size,damage;
	public int blocksDestroyed;
	private Random rand;
	private Iterator<Block> it;
	private boolean shouldBeAvalanche;
	private Vector direction;
	
	private Map<UUID, Integer> fallingBlocks = new ConcurrentHashMap<>();
	private Map<UUID, Pair<Location, BlockData>> fallingBlocksLocations = new HashMap<>();
	private Map<UUID, Block> initialSpot = new HashMap<>();
	private Map<UUID, ItemStack[]> invBlocks = new HashMap<>();
	
	private Set<Material> allowedBlocks = new HashSet<>(Arrays.asList(Material.GRASS_BLOCK, Material.DIRT, Material.DIRT_PATH, Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.SNOW_BLOCK, Material.STONE, Material.GRAVEL, Material.PACKED_ICE, Material.ICE, Material.BLUE_ICE));
	
	private Set<ChannelDataHolder> survivingPlayers = new HashSet<>();

	public LandSlide(int level) {
		super(level);
		this.rand = plugin.random;
		switch (level) {
		default:
		case 1:
			radius = 9;
			depth = 1;
			maxOffset = 6;
			break;
		case 2:
			radius = 13;
			depth = 2;
			maxOffset = 8;
			break;
		case 3:
			radius = 18;
			depth = 3;
			maxOffset = 10;
			break;
		case 4:
			radius = 23;
			depth = 4;
			maxOffset = 12;
			break;
		case 5:
			radius = 30;
			depth = 5;
			maxOffset = 15;
			break;
		case 6:
			radius = 50;
			depth = 6;
			maxOffset = 25;
			break;
		}
		size = configFile.getDouble("landslide.size");
		radius *= size;
		damage = configFile.getDouble("landslide.damage");
		maxBlocks = configFile.getInt("landslide.max_falling_blocks");
		volume = configFile.getDouble("landslide.volume");
		
		if (plugin.mcVersion >= 1.17)
			allowedBlocks.add(Material.POWDER_SNOW);
		
		this.type = Disaster.LANDSLIDE;
	}
	@Override
	public void start(Location loc, Player p) {
		start(loc, loc.clone().add(Utils.randomVector().multiply(15.0)), p);
	}
	public void start(Location location, Location towards, Player p) {
		DestructionDisasterEvent event = new DestructionDisasterEvent(this, loc, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		ongoingDisasters.add(this);
		this.loc = location;
		radius *= size;
		world = loc.getWorld();
		DeathMessages.landslides.add(this);
		addPlayersToSurvivalChannel(loc.clone().subtract(0,radius/2,0), radius+5, survivingPlayers);
		Map<Block,Double> blocks = new HashMap<>();
		BlockVector block = new BlockVector(loc.getX(), loc.getY(), loc.getZ());
		for (int x = -radius; x < radius; x++)
			for (int z = -radius; z < radius; z++) {
				Vector position = block.clone().add(new Vector(x, 0, z));
				if (block.distance(position) > radius) continue;
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
		Set<Block> spots = new LinkedHashSet<>(Utils.sortByValue(blocks).keySet());
		Set<Block> next = new LinkedHashSet<>();
		final int ticks = Math.min(maxBlocks, spots.size() / 20);
		int[] current = {0, 0, 0, 0};
		it = spots.iterator();
		direction = Utils.getVectorTowards(loc, towards);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		LandSlide instance = this;
		double soundDistance = (level*20) * (level*20);
		final double vol = 1.0 / soundDistance;
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				if (current[3]++ % 5 == 0)
					for (Player p : world.getPlayers()) {
						Location pLoc = p.getEyeLocation();
						double dist = pLoc.distanceSquared(loc);
						if (dist <= soundDistance) {
							if (!shouldBeAvalanche) {
								if (rand.nextInt(2) == 0)
									world.playSound(pLoc.add(Utils.getVectorTowards(pLoc, loc).multiply(4.0)), Sound.BLOCK_GRAVEL_BREAK, (float) ((vol*(soundDistance-dist))*volume), 0.5f);
								else
									world.playSound(pLoc.add(Utils.getVectorTowards(pLoc, loc).multiply(4.0)), Sound.BLOCK_GRASS_BREAK, (float) ((vol*(soundDistance-dist))*volume), 0.5f);
							} else {
								if (rand.nextInt(2) == 0)
									world.playSound(pLoc.add(Utils.getVectorTowards(pLoc, loc).multiply(4.0)), Sound.BLOCK_SNOW_BREAK, (float) ((vol*(soundDistance-dist))*volume), 0.5f);
								else
									world.playSound(pLoc.add(Utils.getVectorTowards(pLoc, loc).multiply(4.0)), Sound.BLOCK_SAND_BREAK, (float) ((vol*(soundDistance-dist))*volume), 0.5f);
							}
						}
					}
				Iterator<Entry<UUID, Integer>> blocksIterator = fallingBlocks.entrySet().iterator();
				while (blocksIterator.hasNext()) {
					Entry<UUID, Integer> entry = blocksIterator.next();
					UUID uuid = entry.getKey();
					Entity entity = Bukkit.getEntity(uuid);
					if (entity == null || entity.isDead()) {
						FallingBlock fb = loc.getWorld().spawnFallingBlock(fallingBlocksLocations.get(uuid).getFirst(), fallingBlocksLocations.get(uuid).getSecond());
						fb.setHurtEntities(false);
						fb.setDropItem(false);
						fb.setMetadata("dd-fb", new FixedMetadataValue(plugin, "protected"));
						fb.setVelocity(direction.clone().add(new Vector(rand.nextDouble()/4-.125,0.0,rand.nextDouble()/4-.125)).multiply(rand.nextDouble()/4+.4).setY(0.3));
						if (!shouldBeAvalanche) {
							if (rand.nextInt(2) == 0)
								fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_GRAVEL_BREAK, (float) (1f*volume), 0.5f);
							else
								fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_GRASS_BREAK, (float) (1f*volume), 0.5f);
						} else {
							if (rand.nextInt(2) == 0)
								fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_SNOW_BREAK, (float) (1f*volume), 0.5f);
							else
								fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_SAND_BREAK, (float) (1f*volume), 0.5f);
						}
						int bounces = entry.getValue();
						if (bounces > 0) {
							fallingBlocks.put(fb.getUniqueId(), bounces - 1);
							fallingBlocksLocations.put(fb.getUniqueId(), Pair.of(fb.getLocation(), fb.getBlockData()));
							initialSpot.put(fb.getUniqueId(), initialSpot.remove(uuid));
							if (invBlocks.containsKey(uuid))
								invBlocks.put(fb.getUniqueId(), invBlocks.remove(uuid));
						} else {
							UUIDToFalling.put(fb.getUniqueId(), initialSpot.remove(uuid));
							if (invBlocks.containsKey(uuid))
								CoreListener.addBlockInventory(fb, invBlocks.remove(uuid));
						}
						fallingBlocksLocations.remove(uuid);
						blocksIterator.remove();
						continue;
					} else {
						fallingBlocksLocations.replace(entity.getUniqueId(), Pair.of(entity.getLocation(), ((FallingBlock) entity).getBlockData()));
						Vector vel = entity.getVelocity().multiply(0.05).setY(0);
						for (Entity e : entity.getNearbyEntities(1.8, 1.8, 1.8)) {
							if (e instanceof FallingBlock || isEntityTypeProtected(e))
								continue;
							if (e.getVelocity().distanceSquared(vel) < 0.28)
								e.setVelocity(e.getVelocity().add(vel));
							if (e instanceof LivingEntity && ((LivingEntity) e).getNoDamageTicks() == 0 && (!(e instanceof Player) || !Utils.isPlayerImmune((Player) e)))
								Utils.damageEntity((LivingEntity) e, damage, "dd-" + (shouldBeAvalanche ? "avalanche" : "landslide"), false, DamageCause.FALLING_BLOCK);
						}
					}
				}
				if (current[2] > 0) {
					if (current[2]++ > 200 || fallingBlocks.isEmpty()) {
						cancel();
						fallingBlocks.forEach((k, v) -> {
							if (Bukkit.getEntity(k) != null)
								Bukkit.getEntity(k).remove();
						});
						HandlerList.unregisterAll(instance);
						ongoingDisasters.remove(instance);
						DeathMessages.landslides.remove(instance);
						triggerRegen(true);
						Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
					}
					return;
				}
				for (int i=current[0]; i < current[0]+ticks; i++) {
					if (fallingBlocks.size() >= maxBlocks) {
						current[0] += i-current[0];
						return;
					}
					if (!it.hasNext()) {
						current[0] = 0;
						current[1]++;
						spots.clear();
						spots.addAll(next);
						next.clear();
						it = spots.iterator();
						if (current[1] >= depth)
							current[2] = 1;
						break;
					}
					Block b = it.next();
					for (int c=0; c < (rand.nextInt(4) == 0 ? 2 : 1); c++) {
						if (isPassableOrSnow(b) || Utils.passStrengthTest(b.getType()) || Utils.isZoneProtected(b.getLocation()))
							break;
						BlockData mat = b.getBlockData();
						if (materials != null)
							mat = materials[rand.nextInt(materials.length)];
						FallingBlock fb = b.getWorld().spawnFallingBlock(b.getLocation().add(0.5,0.5,0.5), mat);
						if (plugin.CProtect)
							Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
						BlockState state = b.getState();
						if (state instanceof InventoryHolder)
							invBlocks.put(fb.getUniqueId(), ((InventoryHolder) b.getState()).getInventory().getContents());
						addBlockWithTopToList(b, state);
						b.setType(Material.AIR);
						blocksDestroyed++;
						fb.setHurtEntities(false);
						fb.setDropItem(false);
						fb.setVelocity(direction.clone().add(new Vector(rand.nextDouble()/4-.125,0.0,rand.nextDouble()/4-.125)).multiply(rand.nextDouble()/4+.25).setY(0.1));
						fb.setMetadata("dd-fb", new FixedMetadataValue(plugin, "protected"));
						fallingBlocks.put(fb.getUniqueId(), rand.nextInt(5));
						fallingBlocksLocations.put(fb.getUniqueId(), Pair.of(fb.getLocation(), fb.getBlockData()));
						initialSpot.put(fb.getUniqueId(), b);
						b = b.getRelative(BlockFace.DOWN);
					}
					next.add(b);
				}
				current[0] += ticks;
			}
		};
	}
	@EventHandler(priority=EventPriority.LOW)
	public void onBlockLand(EntityChangeBlockEvent e) {
		if (!fallingBlocks.containsKey(e.getEntity().getUniqueId()))
			return;
		UUID uuid = e.getEntity().getUniqueId();
		e.setCancelled(true);
		BlockData mat = ((FallingBlock) e.getEntity()).getBlockData();
		if (materials != null)
			mat = materials[rand.nextInt(materials.length)];
		FallingBlock fb = e.getEntity().getWorld().spawnFallingBlock(e.getEntity().getLocation(), mat);
		fb.setHurtEntities(false);
		fb.setDropItem(false);
		fb.setMetadata("dd-fb", new FixedMetadataValue(plugin, "protected"));
		fb.setVelocity(direction.clone().add(new Vector(rand.nextDouble()/4-.125,0.0,rand.nextDouble()/4-.125)).multiply(rand.nextDouble()/4+.4).setY(0.3));
		int bounces = fallingBlocks.remove(uuid);
		if (bounces > 0) {
			fallingBlocks.put(fb.getUniqueId(), bounces - 1);
			fallingBlocksLocations.put(fb.getUniqueId(), Pair.of(fb.getLocation(), fb.getBlockData()));
			initialSpot.put(fb.getUniqueId(), initialSpot.remove(uuid));
			if (invBlocks.containsKey(uuid))
				invBlocks.put(fb.getUniqueId(), invBlocks.remove(uuid));
		} else {
			UUIDToFalling.put(fb.getUniqueId(), initialSpot.remove(uuid));
			if (invBlocks.containsKey(uuid))
				CoreListener.addBlockInventory(fb, invBlocks.remove(uuid));
		}
		fallingBlocksLocations.remove(e.getEntity().getUniqueId());
		e.getEntity().remove();
		if (!shouldBeAvalanche) {
			if (rand.nextInt(2) == 0)
				fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_GRAVEL_BREAK, (float) (1f*volume), 0.5f);
			else
				fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_GRASS_BREAK, (float) (1f*volume), 0.5f);
		} else {
			if (rand.nextInt(2) == 0)
				fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_SNOW_BREAK, (float) (1f*volume), 0.5f);
			else
				fb.getWorld().playSound(fb.getLocation(), Sound.BLOCK_SAND_BREAK, (float) (1f*volume), 0.5f);
		}
	}
	@Override
	public Location findApplicableLocation(Location temp, Player p) {
		for (int t=0; t < 5; t++) {
			Location newTemp = temp.clone().add(0, rand.nextInt(level*4)+10, 0);
			if (newTemp.getBlock().getType() != Material.AIR || newTemp.getY() < type.getMinHeight())
				continue;
			for (int i=0; i < 10; i++) {
				Vector vec = new Vector(rand.nextDouble()-.5, (rand.nextDouble()-.8)/2, rand.nextDouble()-.5).normalize().multiply(0.8);
				Location cast = newTemp.clone().add(vec);
				for (int d=0; d < 30; d++) {
					if (!isPassableOrSnow(cast.getBlock()) && !Tag.LEAVES.isTagged(cast.getBlock().getType())) {
						if (cast.getBlockY() <= temp.getBlockY()+5 || !allowedBlocks.contains(cast.getBlock().getType()))
							break;
						int count = 0, snowCount = 0;
						label:
						for (int c=0; c < 30; c++) {
							Vector angle = new Vector(vec.getX()+((rand.nextDouble()-.5)), vec.getY()+((rand.nextDouble()-.5)/2), vec.getZ()+((rand.nextDouble()-.5))).normalize().multiply(0.8);
							Location check = newTemp.clone().add(angle);
							for (int a=0; a < d+6; a++) {
								if (!isPassableOrSnow(check.getBlock()) && !Tag.LEAVES.isTagged(check.getBlock().getType())) {
									if (!allowedBlocks.contains(check.getBlock().getType()))
										break;
									if (a >= d-6) {
										count++;
										if (check.getBlock().getType() == Material.SNOW_BLOCK || (plugin.mcVersion >= 1.17 && check.getBlock().getType() == Material.POWDER_SNOW))
											snowCount++;
										if (count >= 20) {
											if (snowCount >= 10 && check.getBlock().getTemperature() < 0.15)
												shouldBeAvalanche = true;
											else
												shouldBeAvalanche = false;
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
	public Location findApplicableLocationDebug(Location temp, Player p) {
		Queue<Location> locs = new ArrayDeque<>();
		Queue<Location> current = new ArrayDeque<>();
		Queue<Location> locs2 = new ArrayDeque<>();
		Location[] b = new Location[] {null};
		new RepeatingTask(plugin, 0, 1) {
			@Override
			public void run() {
				locs.forEach(e -> e.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, e, 1, 0, 0, 0, 0.00001, null));
				locs2.forEach(e -> e.getWorld().spawnParticle(Particle.FLAME, e, 1, 0, 0, 0, 0.00001, null));
				current.forEach(e -> e.getWorld().spawnParticle(Particle.CRIT, e, 1, 0, 0, 0, 0.00001, null));
				if (b[0] != null && b[0].getBlock().isPassable())
					cancel();
			}
		};
		for (int t=0; t < 5; t++) {
			Location newTemp = temp.clone().add(0, rand.nextInt(level*4)+10, 0);
			if (newTemp.getBlock().getType() != Material.AIR || newTemp.getY() < type.getMinHeight())
				continue;
			newTemp.getBlock().setType(Material.GOLD_BLOCK);
			b[0] = newTemp;
			for (int i=0; i < 10; i++) {
				Vector vec = new Vector(rand.nextDouble()-.5, (rand.nextDouble()-.8)/2, rand.nextDouble()-.5).normalize().multiply(0.8);
				Location cast = newTemp.clone().add(vec);
				for (int d=0; d < 30; d++) {
					current.add(cast.clone());
					if (!isPassableOrSnow(cast.getBlock()) && !Tag.LEAVES.isTagged(cast.getBlock().getType())) {
						if (cast.getBlockY() <= temp.getBlockY()+5 || !allowedBlocks.contains(cast.getBlock().getType())) {
							locs.addAll(current);
							current.clear();
							break;
						}
						int count = 0, snowCount = 0;
						label:
						for (int c=0; c < 30; c++) {
							Vector angle = new Vector(vec.getX()+((rand.nextDouble()-.5)), vec.getY()+((rand.nextDouble()-.5)/2), vec.getZ()+((rand.nextDouble()-.5))).normalize().multiply(0.8);
							Location check = newTemp.clone().add(angle);
							for (int a=0; a < d+6; a++) {
								locs2.add(check.clone());
								if (check.getBlock().getType() == Material.GOLD_BLOCK) {
									check.add(angle);
									continue;
								}
								if (!isPassableOrSnow(check.getBlock()) && !Tag.LEAVES.isTagged(check.getBlock().getType())) {
									if (!allowedBlocks.contains(check.getBlock().getType()))
										break;
									if (a >= d-6) {
										count++;
										if (check.getBlock().getType() == Material.SNOW_BLOCK || (plugin.mcVersion >= 1.17 && check.getBlock().getType() == Material.POWDER_SNOW))
											snowCount++;
										if (count >= 20) {
											if (snowCount >= 10 && check.getBlock().getTemperature() < 0.15)
												shouldBeAvalanche = true;
											else
												shouldBeAvalanche = false;
											break label;
										}
									}
									break;
								}
								check.add(angle);
							}
						}
						Bukkit.broadcastMessage(newTemp.getBlockY()+" "+count);
						if (count >= 20) {
							cast.getBlock().setType(Material.DIAMOND_BLOCK);
							return cast;
						}
						locs.addAll(current);
						current.clear();
						break;
					}
					cast.add(vec);
				}
			}
		}
		return null;
	}
	public void broadcastMessage(Location temp, Player p) {
		if ((boolean) WorldObject.findWorldObject(temp.getWorld()).settings.get("event_broadcast")) {
			String str = configFile.getString("messages.destructive.level "+level);
			str = str.replace("%location%", temp.getBlockX()+" "+temp.getBlockY()+" "+temp.getBlockZ());
			if (p != null) str = str.replace("%player%", p.getName());
			else str = str.replace("%player%", "");
			if (!shouldBeAvalanche)
				str = str.replace("%disaster%", type.getLabel().substring(0, type.getLabel().indexOf('/')));
			else
				str = str.replace("%disaster%", type.getLabel().substring(type.getLabel().indexOf('/')+1));
			str = ChatColor.translateAlternateColorCodes('&', str);
			if (configFile.getBoolean("messages.disaster_tips"))
				str += "\n"+type.getTip();
			for (Player players : temp.getWorld().getPlayers())
				players.sendMessage(str);
			Main.consoleSender.sendMessage(Languages.prefix+str+ChatColor.GREEN+" ("+temp.getWorld().getName()+")");
		}
	}
	public void startAdjustment(Location loc, Player p) {
		start(loc, p);
	}
	private boolean isPassableOrSnow(Block b) {
		return (b.isPassable() && !(plugin.mcVersion >= 1.17 && b.getType() == Material.POWDER_SNOW));
	}
	public BlockData[] getMaterials() {
		return materials;
	}
	public void setMaterials(Material[] temp) {
		materials = new BlockData[temp.length];
		for (int i=0; i < temp.length; i++)
			materials[i] = temp[i].createBlockData();
	}
}
