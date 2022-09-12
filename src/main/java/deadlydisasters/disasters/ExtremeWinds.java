package deadlydisasters.disasters;

import java.util.ArrayDeque;
import java.util.ArrayList;
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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.DisasterEvent;
import deadlydisasters.disasters.events.WeatherDisaster;
import deadlydisasters.disasters.events.WeatherDisasterEvent;
import deadlydisasters.general.DifficultyLevel;
import deadlydisasters.general.WorldObject;
import deadlydisasters.listeners.CoreListener;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.Metrics;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class ExtremeWinds extends WeatherDisaster {
	
	public static Set<Material> bannedBlocks = new HashSet<>();
	
	private double tempForce,breakForce;
	private Particle particle;
	private int maxParticles;
	private int blocksDestroyed;
	
	private Queue<Entity> entities = new ArrayDeque<>();
	
	private ExtremeWinds me = this;
	
	public Map<FallingBlock,Block> regenPreState = new HashMap<>();
	public Map<Block,Block> regenStates = new HashMap<>();
	public Map<Block,Material[]> fallenBlocks = new LinkedHashMap<>();
		
	public ExtremeWinds(int level) {
		super(level);
		time = plugin.getConfig().getInt("extremewinds.time.level "+this.level) * 20;
		delay = plugin.getConfig().getInt("extremewinds.start_delay") * 20;
		tempForce = plugin.getConfig().getDouble("extremewinds.force.level "+level);
		particle = Particle.CLOUD;
		maxParticles = (plugin.getConfig().getInt("extremewinds.max_particles")/6) * level;
		breakForce = plugin.getConfig().getDouble("extremewinds.block_break_force");
		volume = plugin.getConfig().getDouble("extremewinds.volume");
		
		this.type = Disaster.EXTREMEWINDS;
	}
	public void start(World world, Player p, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		ongoingDisasters.add(me);
		DeathMessages.extremewinds.add(this);
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast")) {
			String str = Utils.chat(plugin.getConfig().getString("messages.weather.winds.level "+level));
			if (plugin.getConfig().getBoolean("messages.disaster_tips"))
				str += "\n"+type.getTip();
			for (Player all : world.getPlayers())
				all.sendMessage(str);
		}
		startWinds();
	}
	public void startWinds() {
		Random rand = plugin.random;
		double[] force = {0, tempForce/100, -(tempForce/3), tempForce};
		Vector vel = new Vector(rand.nextDouble()-0.5, 0, rand.nextDouble()-0.5).normalize().setY(Math.min(tempForce*4.5, 0.5));
		Vector reverse = vel.clone().multiply(-1).setY(0);
		int fallOff = (8-level)*10;
		int halfFall = fallOff/2;
		int heightDist = plugin.getConfig().getInt("extremewinds.interior_height_distance");
		boolean CP = plugin.CProtect;
		FixedMetadataValue fixdata = new FixedMetadataValue(plugin, "protected");
		
		List<Entity> tempList = new ArrayList<>();
		tempList.addAll(world.getEntities());
		int[] cycle = {tempList.size()-1, tempList.size()/20, 0};
		
		new RepeatingTask(plugin, delay, 1) {
			@Override
			public void run() {
				if (time <= 0) {
					if (!regenPreState.isEmpty()) {
						Iterator<Entry<FallingBlock, Block>> it = regenPreState.entrySet().iterator();
						while (it.hasNext())
							if (it.next().getKey().isDead())
								it.remove();
						return;
					}
					ongoingDisasters.remove(me);
					triggerRegen();
					cancel();
					DeathMessages.extremewinds.remove(me);
					Metrics.incrementValue(Metrics.disasterDestroyedMap, type.getMetricsLabel(), blocksDestroyed);
					return;
				}
				time--;
				force[0] += force[1];
				if ((force[1] > 0 && force[0] > force[3]) || (force[1] < 0 && force[0] < force[2]))
					force[1] *= -1;
				Vector speed = vel.clone().multiply(force[0]);
				Vector offset = speed.clone().multiply(-1).setY(0).multiply(force[0]*700);
				Vector entityWindSpeed = speed.clone().setY(speed.getY()/2).multiply(10);
				if (force[0] > 0) {
					for (Entity e : entities) {
						if (e instanceof LivingEntity)
							e.setVelocity(e.getVelocity().setY(Math.min(e.getVelocity().getY(), 0.35)).add(speed));
						else
							e.setVelocity(entityWindSpeed);
					}
					for (Player p : world.getPlayers())
						if (p.getLocation().getBlockY() > 50) {
							Location pLoc = p.getLocation();
							for (int i=0; i < maxParticles; i++)
								p.spawnParticle(particle, (rand.nextDouble()*fallOff-halfFall)+pLoc.getX()+offset.getX(), (rand.nextDouble()*fallOff-(fallOff/4))+pLoc.getY(), (rand.nextDouble()*fallOff-halfFall)+pLoc.getZ()+offset.getZ(), 0, speed.getX(), 0.001, speed.getZ(), force[0]*150);
							if (cycle[2] == 0) {
								if (entities.contains(p))
									p.playSound(p.getLocation().clone().add(speed), Sound.WEATHER_RAIN, (float) ((force[0]*5)*volume), 0.5F);
								else
									p.playSound(p.getLocation().clone().add(0,2,0), Sound.WEATHER_RAIN_ABOVE, (float) ((force[0])*volume), 0.5F);
								if (force[0] > breakForce) {
									Block b = pLoc.clone().add(0,2,0).getBlock();
									if (!b.getType().isSolid()) b = pLoc.clone().add(rand.nextInt(8)-4, rand.nextInt(5)-1, rand.nextInt(8)-4).getBlock();
									if (b.getType().isSolid() && !Utils.passStrengthTest(b.getType()) && !Utils.isZoneProtected(b.getLocation())) {
										if (CP) Utils.getCoreProtect().logRemoval("Deadly-Disasters", b.getLocation(), b.getType(), b.getBlockData());
										FallingBlock fb = world.spawnFallingBlock(b.getLocation().clone().add(0.5,0.5,0.5), b.getBlockData());
										fb.setDropItem(false);
										fb.setHurtEntities(true);
										fb.setMetadata("dd-fb", fixdata);
										entities.add(fb);
										if (b.getState() instanceof InventoryHolder)
											CoreListener.addBlockInventory(fb, ((InventoryHolder) b.getState()).getInventory().getContents());
										else {
											addBlockToList(b, new Material[] {b.getType(), Material.AIR});
											CoreListener.fallingBlocks.put(fb.getUniqueId(), me);
											regenPreState.put(fb, b);
										}
										b.setType(Material.AIR);
										blocksDestroyed++;
									}
								}
							}
						}
				}
				core:
					for (int i=cycle[0]; i > cycle[0]-cycle[1]; i--) {
						if (i < 0)
							break;
						Entity e = tempList.get(i);
						if (e == null) {
							tempList.remove(i);
							continue;
						}
						Location temp = e.getLocation();
						if (temp.getBlockY() < 57 || Utils.isZoneProtected(temp) || (temp.getBlockY() <= 63 && temp.getBlock().isLiquid())) {
							tempList.remove(i);
							continue;
						}
						if (!(e instanceof LivingEntity))
							continue;
						if (temp.getBlock().getLightFromBlocks() >= 9) {
							Block b = temp.clone().add(0,2,0).getBlock();
							for (int c=0; c < heightDist; c++) {
								if (b.getType().isSolid() && !bannedBlocks.contains(b.getType())) {
									tempList.remove(i);
									continue core;
								}
								b = b.getRelative(BlockFace.UP);
							}
						}
						internal:
							for (int c=0; c < 5; c++) {
								Block b = temp.add(reverse).getBlock();
								if (b.getType().isSolid() && !bannedBlocks.contains(b.getType())) {
									for (int c2=0; c2 < c; c2++) {
										b = b.getRelative(BlockFace.UP);
										if (!b.getType().isSolid() || bannedBlocks.contains(b.getType()))
											continue internal;
									}
									tempList.remove(i);
									continue core;
								}
							}
						if (e instanceof Player && ((Player) e).isFlying())
							tempList.remove(i);
					}
				cycle[0] -= cycle[1];
				if (cycle[0] <= 0) {
					entities.clear();
					entities.addAll(tempList);
					tempList.clear();
					tempList.addAll(world.getEntities());
					cycle[1] = tempList.size()/20;
					cycle[0] = tempList.size()-1;
				}
				if (cycle[2] < 5)
					cycle[2]++;
				else
					cycle[2] = 0;
			}
		};
	}
	@Override
	public void clear() {
		time = 0;
	}
	public void triggerRegen() {
		if (WorldObject.findWorldObject(world).difficulty != DifficultyLevel.CUSTOM || (int) WorldObject.findWorldObject(world).settings.get("regenDelay") < 0)
			return;
		DisasterEvent instance = this;
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				damagedBlocks.putAll(fallenBlocks);
				final double regenRate = type.getRegenTickRate();
				double[] regenTicks = {0};
				RepeatingTask task = new RepeatingTask(plugin, (int) WorldObject.findWorldObject(world).settings.get("regenDelay") * 20, 1) {
					@Override
					public void run() {
						regenTicks[0] += regenRate;
						while (regenTicks[0] >= 1) {
							regenTicks[0] -= 1;
							if (damagedBlocks.isEmpty()) {
								cancel();
								regeneratingTasks.remove(this);
								return;
							}
							Entry<Block, Material[]> entry = damagedBlocks.entrySet().iterator().next();
							Block b = entry.getKey();
							Material material = entry.getValue()[1];
							if (b.getType() == material || ((b.getType() == Material.WATER || b.getType() == Material.LAVA) && ((Levelled) b.getBlockData()).getLevel() > 0)
									|| b.getType() == Material.SNOW || material == Material.FIRE) {
								if (regenStates.containsKey(b)) {
									if (regenStates.get(b).getType() == entry.getValue()[0]) {
										b.setType(entry.getValue()[0]);
										b.setBlockData(blocksData.get(b));
										blocksData.remove(b);
										if (!b.isPassable())
											for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), .5, .5, .5))
												if (e.getVelocity().getY() < 0.4)
													e.setVelocity(e.getVelocity().setY(0.4));
										b = regenStates.get(b);
										if (damagedBlocks.containsKey(b)) {
											b.setType(damagedBlocks.get(b)[0]);
											b.setBlockData(blocksData.get(b));
											blocksData.remove(b);
											if (!b.isPassable())
												for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), .5, .5, .5))
													if (e.getVelocity().getY() < 0.4)
														e.setVelocity(e.getVelocity().setY(0.4));
											damagedBlocks.remove(b);
										}
									}
									damagedBlocks.remove(entry.getKey());
									continue;
								}
								b.setType(entry.getValue()[0]);
								b.setBlockData(blocksData.get(b));
								blocksData.remove(b);
								if (!b.isPassable())
									for (Entity e : b.getWorld().getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), .5, .5, .5))
										if (e.getVelocity().getY() < 0.4)
											e.setVelocity(e.getVelocity().setY(0.4));
							}
							damagedBlocks.remove(entry.getKey());
						}
					}
				};
				Map<DisasterEvent, Map<Block,Material[]>> tempMap = new HashMap<>();
				tempMap.put(instance, damagedBlocks);
				regeneratingTasks.put(task, tempMap);
			}
		});
	}
	public boolean isEntityInvolved(UUID uuid) {
		return (entities.stream().anyMatch(e -> e.getUniqueId().equals(uuid)));
	}
	public double getTempForce() {
		return tempForce;
	}
	public void setTempForce(double tempForce) {
		this.tempForce = tempForce;
	}
	public Particle getParticle() {
		return particle;
	}
	public void setParticle(Particle particle) {
		this.particle = particle;
	}
	public int getMaxParticles() {
		return maxParticles;
	}
	public void setMaxParticles(int maxParticles) {
		this.maxParticles = maxParticles;
	}
	public double getBreakForce() {
		return breakForce;
	}
	public void setBreakForce(double breakForce) {
		this.breakForce = breakForce;
	}
}
