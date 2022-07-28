package deadlydisasters.disasters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import deadlydisasters.disasters.events.WeatherDisaster;
import deadlydisasters.disasters.events.WeatherDisasterEvent;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.general.WorldObject;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class BlackPlague extends WeatherDisaster {
	
	private static BukkitTask id;
	private Random rand;
	private List<EntityType> priorities = new ArrayList<>();
	private int range = 50;
	private Set<EntityType> blacklisted = new HashSet<>();
	
	public static Map<UUID,Integer> time = new HashMap<>();
	public static int maxInfectedMobs;
	private static Set<UUID> infectedPlayers = new HashSet<>();
	
	public BlackPlague(int level) {
		super(level);
		if (maxInfectedMobs == 0)
			maxInfectedMobs = plugin.getConfig().getInt("plague.max_infected_mobs");
		this.rand = plugin.random;
		
		Set<String> typeList = new HashSet<>();
		for (EntityType type : EntityType.values())
			typeList.add(type.toString());
		for (String s : plugin.getConfig().getStringList("plague.blacklisted_mobs"))
			if (typeList.contains(s.toUpperCase()))
				blacklisted.add(EntityType.valueOf(s.toUpperCase()));
			else if (plugin.debug)
				Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: Unreconized entity &d'"+s+"' &ein config under \nplague:\n    blacklisted_mobs:\n        "+s));
		priorities.addAll(Arrays.asList(EntityType.VILLAGER, EntityType.ZOMBIE));
		if (plugin.mcVersion >= 1.14)
			priorities.add(EntityType.WANDERING_TRADER);
		this.type = Disaster.PLAGUE;
	}
	public void start(World world, Player p, boolean broadcastAllowed) {
		WeatherDisasterEvent event = new WeatherDisasterEvent(this, world, level, p);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.world = world;
		updateWeatherSettings();
		if (broadcastAllowed && (boolean) WorldObject.findWorldObject(world).settings.get("event_broadcast")) {
			String str = Utils.chat(plugin.getConfig().getString("messages.misc.plague.started"));
			if (plugin.getConfig().getBoolean("messages.disaster_tips"))
				str += "\n"+type.getTip();
			for (Player players : world.getPlayers())
				players.sendMessage(str);
			Main.consoleSender.sendMessage(Languages.prefix+str+ChatColor.GREEN+" ("+world.getName()+")");
		}
		infectRandomMobs(1, world);
		if (id == null) createTask();
		if (p != null) {
			infectNearTarget(p, 50, 15, 50, priorities.get(rand.nextInt(priorities.size())));
			new RepeatingTask(plugin, 200, rand.nextInt(800)+200) {
				@Override
				public void run() {
					if (p.getWorld() == world)
						infectNearTarget(p, range, 15, range, priorities.get(rand.nextInt(priorities.size())));
					else
						infectRandomMobs(1, world);
					range -= 10;
					level--;
					if (level <= 0) cancel();
				}
			};
		} else infectRandomMobs(level-1, world);
	}
	private void createTask() {
		final DustOptions dust = new DustOptions(Color.fromRGB(0, 0, 0), 0.5F);
		id = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
			@Override
			public void run() {
				if (time.isEmpty()) {
					id.cancel();
					id = null;
					return;
				}
				Map<UUID,Integer> temp = new HashMap<>();
				Iterator<Entry<UUID,Integer>> iterator = time.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<UUID,Integer> entry = iterator.next();
					LivingEntity e = (LivingEntity) Bukkit.getEntity(entry.getKey());
					if (e == null || e.isDead()) {
						if (e == null && infectedPlayers.contains(entry.getKey()))
							continue;
						if (e != null && e.hasMetadata("dd-plague"))
							e.removeMetadata("dd-plague", plugin);
						iterator.remove();
						continue;
					}
					int t = entry.getValue();
					if (t <= 0) {
						e.damage(0.001);
						((LivingEntity) e).setHealth(0);
						e.setMetadata("dd-plaguedeath", new FixedMetadataValue(plugin, "protected"));
						infectedPlayers.remove(e.getUniqueId());
						iterator.remove();
						continue;
					}
					if (WorldObject.findWorldObject(e.getWorld()).curePlagueInRegions && Utils.isZoneProtected(e.getLocation())) {
						e.removeMetadata("dd-plague", plugin);
						infectedPlayers.remove(e.getUniqueId());
						iterator.remove();
						continue;
					}
					e.getWorld().spawnParticle(Particle.REDSTONE, e.getLocation().clone().add(0,e.getHeight()/2,0), 8, 0.4, e.getHeight()/4, 0.4, 1, dust);
					if (t <= 250) e.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 1, true, false));
					if (t <= 200) e.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 3, true, false));
					if (t <= 150) e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40, 2, true, false));
					if (t <= 100) e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2, true));
					if (t <= 60) e.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 100, 1, true));
					if (t <= 20) e.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 2, true));
					if (t <= 150 && e instanceof Player && rand.nextInt(4) == 0) ((Player) e).playSound(e.getLocation(), Sound.ENTITY_HORSE_BREATHE, 0.3F, 0.5F);
					if (time.size() < maxInfectedMobs)
						for (Entity near : e.getNearbyEntities(1, 1, 1)) {
							if (near instanceof LivingEntity && !near.isDead() && !blacklisted.contains(near.getType()) && !near.hasMetadata("dd-plague")) {
								if (near instanceof Player) {
									if (Utils.isPlayerImmune((Player) near))
										continue;
									((Player) near).sendMessage(Utils.chat("&c"+Languages.langFile.getString("misc.plagueCatch")));
									infectedPlayers.add(near.getUniqueId());
								}
								temp.put(near.getUniqueId(), 300);
								near.setMetadata("dd-plague", new FixedMetadataValue(plugin, "protected"));
							}
						}
					time.replace(e.getUniqueId(), t - 1);
				}
				time.putAll(temp);
			}
		}, 0, 20);
	}
	public boolean isMobAvailable(World world) {
		for (LivingEntity e : world.getLivingEntities()) {
			if (blacklisted.contains(e.getType())) continue;
			if (e.hasMetadata("dd-plague")) continue;
			if (e instanceof Player) continue;
			if (e.isInvulnerable()) continue;
			if (plugin.RegionProtection && Utils.isZoneProtected(e.getLocation())) continue;
			return true;
		}
		return false;
	}
	public void infectNearTarget(LivingEntity target, double x, double y, double z, EntityType priority) {
		List<LivingEntity> list = new ArrayList<>();
		for (Entity e : target.getNearbyEntities(x, y, z))
			if (e instanceof LivingEntity && !(e instanceof Player) && !blacklisted.contains(e.getType()) && !e.hasMetadata("dd-plague")) list.add((LivingEntity) e);
		for (LivingEntity e : list)
			if (e.getType() == priority) {
				time.put(e.getUniqueId(), 300);
				e.setMetadata("dd-plague", new FixedMetadataValue(plugin, "protected"));
				return;
			}
		if (list.isEmpty()) {
			infectRandomMobs(1, world);
			return;
		}
		LivingEntity e = list.get(rand.nextInt(list.size()));
		time.put(e.getUniqueId(), 300);
		e.setMetadata("dd-plague", new FixedMetadataValue(plugin, "protected"));
	}
	public void infectRandomMobs(int amount, World w) {
		for (LivingEntity e : w.getLivingEntities()) {
			if (blacklisted.contains(e.getType())) continue;
			if (e.hasMetadata("dd-plague")) continue;
			if (e instanceof Player) continue;
			if (e.isInvulnerable()) continue;
			if (plugin.RegionProtection && Utils.isZoneProtected(e.getLocation())) continue;
			time.put(e.getUniqueId(), 300);
			e.setMetadata("dd-plague", new FixedMetadataValue(plugin, "protected"));
			e.setRemoveWhenFarAway(false);
			amount--;
			if (amount <= 0) return;
		}
	}
	public void infect(LivingEntity e) {
		time.put(e.getUniqueId(), 300);
		e.setMetadata("dd-plague", new FixedMetadataValue(plugin, "protected"));
		e.setRemoveWhenFarAway(false);
		if (e instanceof Player)
			infectedPlayers.add(e.getUniqueId());
	}
	public static void cureEntity(LivingEntity e, Main plugin) {
		time.remove(e.getUniqueId());
		e.removeMetadata("dd-plague", plugin);
		if (e instanceof Player)
			infectedPlayers.remove(e.getUniqueId());
	}
	@Override
	public void clear() {
		for (Entry<UUID, Integer> entries : time.entrySet())
			if (Bukkit.getEntity(entries.getKey()) != null)
				Bukkit.getEntity(entries.getKey()).removeMetadata("dd-plague", plugin);
		time.clear();
		infectedPlayers.clear();
	}
	public void setPriorities(List<EntityType> priorities) {
		this.priorities = priorities;
	}
}