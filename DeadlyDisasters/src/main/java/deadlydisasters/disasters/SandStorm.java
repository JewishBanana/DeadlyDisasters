package deadlydisasters.disasters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import deadlydisasters.disasters.events.WeatherDisaster;
import deadlydisasters.disasters.events.WeatherDisasterEvent;
import deadlydisasters.entities.sandstormentities.AncientMummy;
import deadlydisasters.entities.sandstormentities.AncientSkeleton;
import deadlydisasters.general.WorldObject;
import deadlydisasters.listeners.DeathMessages;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class SandStorm extends WeatherDisaster {
	
	private boolean skulls,wither,custom;
	private double version,spawnRate,particleMultiplier;
	private int particleRange,particleYRange;
	
	private Set<UUID> mobs = new HashSet<>();
	
	public static Set<Biome> sandStormBiomes = new HashSet<>();
	private Set<Biome> badlands = new HashSet<>();
	private Map<UUID,UUID> targets = new HashMap<>();
	
	public SandStorm(int level) {
		super(level);
		skulls = plugin.getConfig().getBoolean("sandstorm.mobs_drop_skulls");
		wither = plugin.getConfig().getBoolean("sandstorm.wither_effect");
		custom = plugin.getConfig().getBoolean("customentities.allow_custom_mobs");
		time = plugin.getConfig().getInt("sandstorm.time.level "+this.level) * 20;
		delay = plugin.getConfig().getInt("sandstorm.start_delay") * 20;
		particleRange = plugin.getConfig().getInt("sandstorm.particle_max_distance");
		particleYRange = plugin.getConfig().getInt("sandstorm.particle_Y_range");
		particleMultiplier = 0.25 * plugin.getConfig().getInt("sandstorm.particle_multiplier");
		spawnRate = plugin.getConfig().getDouble("sandstorm.mob_spawn_rate");
		volume = plugin.getConfig().getDouble("sandstorm.volume");
		version = plugin.mcVersion;
		
		badlands.addAll(Arrays.asList(Biome.BADLANDS, Biome.ERODED_BADLANDS));
		if (plugin.mcVersion < 1.18)
			badlands.add(Biome.valueOf("MODIFIED_WOODED_BADLANDS_PLATEAU"));
		this.type = Disaster.SANDSTORM;
	}
	public void start(World world, Player player, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, player);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast"))
			Utils.broadcastEvent(level, "weather", this.type, world);
		DeathMessages.sandstorms.add(this);
		Random rand = new Random();
		SandStorm obj = this;
		BukkitTask[] task = new BukkitTask[1];
		int[] current = {0};
		task[0] = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				if (time <= 0) {
					task[0].cancel();
					if (version >= 1.16)
						for (Player p : world.getPlayers()) {
							p.stopSound(Sound.AMBIENT_BASALT_DELTAS_ADDITIONS);
							p.stopSound(Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP);
						}
					return;
				}
				for (Player p : world.getPlayers()) {
					Location closest = null;
					Location location = p.getLocation();
					p.playSound(location.clone().add(0,3,0), Sound.WEATHER_RAIN_ABOVE, (float) (0.017*volume), 0.5F);
					BlockData bd = Material.SAND.createBlockData();
					DustOptions dust = new DustOptions(Color.fromRGB(255, 254, 196), 1);
					if (badlands.contains(location.getBlock().getBiome())) {
						bd = Material.RED_SAND.createBlockData();
						dust = new DustOptions(Color.fromRGB(255, 136, 77), 1);
					}
					for (int x=-particleRange; x <= particleRange; x++)
						for (int z=-particleRange; z <= particleRange; z++) {
							if (rand.nextDouble() >= particleMultiplier)
								continue;
							Location temp = p.getLocation().add(x,0,z);
							Location b = world.getHighestBlockAt(temp).getLocation();
							if (!sandStormBiomes.contains(b.getBlock().getBiome()) || Utils.isWeatherDisabled(b, obj))
								continue;
							int diff = b.getBlockY() - temp.getBlockY();
							if (diff > particleYRange)
								continue;
							if (diff < 0)
								b.setY(b.getY()+(diff*-1));
							if (closest == null || b.distanceSquared(p.getLocation()) < closest.distanceSquared(p.getLocation()))
								closest = b;
							if (diff > 0)
								p.spawnParticle(Particle.FALLING_DUST, b.clone().add(0,3,0), 1, .5, 1, .5, 1, bd);
							else {
								p.spawnParticle(Particle.FALLING_DUST, b.clone().add(0,3,0), 1, .5, 2, .5, 1, bd);
								p.spawnParticle(Particle.REDSTONE, b.clone().add(0,3,0), 1, .5, 2, .5, 1, dust);
							}
						}
					if (current[0] >= 60 && plugin.mcVersion >= 1.16 && closest != null) {
						p.playSound(closest, Sound.AMBIENT_BASALT_DELTAS_ADDITIONS, SoundCategory.AMBIENT, (float) (1*volume), 0.75F);
						p.playSound(closest, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, SoundCategory.AMBIENT, (float) (1*volume), 0.5F);
						if (rand.nextInt(10) == 0)
							p.playSound(closest, Sound.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.AMBIENT, (float) (1*volume), 0.5F);
					}
					if (sandStormBiomes.contains(p.getLocation().getBlock().getBiome()) && p.getWorld().getHighestBlockYAt(location) <= location.getBlockY()+1)
						for (int i=0; i < 10; i++)
							p.spawnParticle(Particle.SMOKE_NORMAL, location.getX()+((rand.nextDouble()*4)-2), location.getY()+((rand.nextDouble()*2.5)-0.5), location.getZ()+((rand.nextDouble()*4)-2), 0, (rand.nextDouble()*10)-5, (rand.nextDouble()*10)-5, (rand.nextDouble()*10)-5, 1);
				}
				if (current[0] > 60)
					current[0] = 0;
				current[0]++;
			}
		}, delay, 1);
		int[] spawnTick = {0};
		new RepeatingTask(plugin, delay, 5) {
			@Override
			public void run() {
				if (time <= 0) {
					clear();
					for (UUID uuid : mobs) {
						Mob mob = (Mob) Bukkit.getEntity(uuid);
						if (mob == null)
							continue;
						Location temp = mob.getLocation().clone();
						mob.remove();
						if (skulls && rand.nextInt(9) == 0 && temp.getBlock().getType() == Material.AIR && temp.clone().subtract(0,1,0).getBlock().getType().isSolid()) {
							if (Utils.isZoneProtected(temp)) continue;
							temp.getBlock().setType(Material.SKELETON_SKULL);
							temp.getBlock().setBlockData(Bukkit.createBlockData("minecraft:skeleton_skull[rotation="+rand.nextInt(13)+"]"));
							if (plugin.CProtect) Utils.getCoreProtect().logPlacement("Deadly-Disasters", temp, Material.SKELETON_SKULL, temp.getBlock().getBlockData());
						}
					}
					cancel();
					world.setStorm(false);
					return;
				}
				if (!world.hasStorm())
					world.setStorm(true);
				for (LivingEntity all : world.getLivingEntities()) {
					if (mobs.contains(all.getUniqueId()) && ((Mob) all).getTarget() == null && Bukkit.getEntity(targets.get(all.getUniqueId())) != null)
						((Mob) all).setTarget((LivingEntity) Bukkit.getEntity(targets.get(all.getUniqueId())));
					if (all.getLocation().getY() < 50 || !sandStormBiomes.contains(all.getLocation().getBlock().getBiome()) || Utils.isWeatherDisabled(all.getLocation(), obj)
							|| all instanceof Husk || all instanceof Stray || all instanceof Skeleton || all instanceof Zombie) continue;
					if (wither && spawnTick[0] >= 60 && rand.nextInt(4) == 0 && world.getHighestBlockYAt(all.getLocation()) <= all.getLocation().getBlockY()+1)
						Utils.pureDamageEntity(all, 1D, "dd-sandstormdeath", false);
					if (all instanceof Player && !Utils.isPlayerImmune((Player) all)) {
						if (world.getHighestBlockYAt(all.getLocation()) <= all.getLocation().getBlockY()+1) {
							if (wither) {
								all.removePotionEffect(PotionEffectType.WITHER);
								all.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20, 0, true, false));
							}
							all.removePotionEffect(PotionEffectType.BLINDNESS);
							all.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, true, false));
						}
						if (spawnTick[0] >= 60 && rand.nextDouble()*100 < spawnRate) {
							Location loc = Utils.getSpotInSquareRadius(all.getLocation(), 10);
							int r = rand.nextInt(8);
							if (!custom && r <= 1)
								r = 3;
							if (r == 0) {
								Skeleton skel = (Skeleton) world.spawnEntity(loc, EntityType.SKELETON);
								plugin.handler.addEntity(new AncientSkeleton(skel, plugin, rand));
								mobs.add(skel.getUniqueId());
								skel.setTarget(all);
								targets.put(skel.getUniqueId(), all.getUniqueId());
								skel.setMetadata("dd-sandstormmob", plugin.fixedData);
							} else if (r == 1) {
								Husk husk = (Husk) world.spawnEntity(loc, EntityType.HUSK);
								plugin.handler.addEntity(new AncientMummy(husk, plugin, rand));
								mobs.add(husk.getUniqueId());
								husk.setTarget(all);
								targets.put(husk.getUniqueId(), all.getUniqueId());
								husk.setMetadata("dd-sandstormmob", plugin.fixedData);
							} else if (r <= 3) {
								Husk husk = (Husk) world.spawnEntity(loc, EntityType.HUSK);
								husk.setTarget(all);
								mobs.add(husk.getUniqueId());
								targets.put(husk.getUniqueId(), all.getUniqueId());
								husk.setMetadata("dd-sandstormmob", plugin.fixedData);
							} else {
								Skeleton skell = (Skeleton) world.spawnEntity(loc, EntityType.SKELETON);
								skell.setTarget(all);
								targets.put(skell.getUniqueId(), all.getUniqueId());
								mobs.add(skell.getUniqueId());
								skell.setMetadata("dd-sandstormmob", plugin.fixedData);
								plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
									@Override
									public void run() {
										skell.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
										skell.setMetadata("dd-unburnable", new FixedMetadataValue(plugin, "protected"));
										skell.setMetadata("dd-ancientminion", new FixedMetadataValue(plugin, "protected"));
										skell.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, true, false));
									}
								}, 1L);
							}
						}
					}
				}
				if (spawnTick[0] > 60)
					spawnTick[0] = 0;
				spawnTick[0] += 5;
				time -= 5;
			}
		};
	}
	@Override
	public void clear() {
		time = 0;
		clearEntities();
		DeathMessages.sandstorms.remove(this);
	}
	public void clearEntities() {
		for (UUID e : mobs)
			if (Bukkit.getEntity(e) != null)
				Bukkit.getEntity(e).remove();
	}
	public boolean isWitherActive() {
		return wither;
	}
	public void setWitherActive(boolean value) {
		this.wither = value;
	}
	public boolean canMobsDropSkulls() {
		return skulls;
	}
	public void setMobsDropSkulls(boolean value) {
		this.skulls = value;
	}
	public Set<UUID> getMobs() {
		return mobs;
	}
	public boolean isCustom() {
		return custom;
	}
	public void setCustom(boolean custom) {
		this.custom = custom;
	}
}
