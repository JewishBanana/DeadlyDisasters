package com.github.jewishbanana.deadlydisasters.events.disasters;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Ghoul;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Psyco;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Scarecrow;
import com.github.jewishbanana.deadlydisasters.entities.halloweenentities.Vampire;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.DarkMage;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.ShadowLeech;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.SkeletonKnight;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.SwampBeast;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.TunnellerZombie;
import com.github.jewishbanana.deadlydisasters.entities.purgeentities.ZombieKnight;
import com.github.jewishbanana.deadlydisasters.events.DestructionDisaster;
import com.github.jewishbanana.deadlydisasters.events.Disaster;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.utils.RepeatingTask;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class DeathParade extends DestructionDisaster {
	
	private boolean custom,running = true;
	private int spawnDistance,despawnSpeed,spawnSpeed;
	private Random rand;
	private String endMessage;
	private int[] mobProbabilities;
	private int sum;
	
	public UUID kingUUID;
	
	private Queue<UUID> entities = new ArrayDeque<>();
	private Map<UUID,UUID> targetMap = new HashMap<>();

	public DeathParade(int level, World world) {
		super(level, world);
		this.rand = plugin.random;
		spawnDistance = 25;
		despawnSpeed = 40;
		volume = configFile.getDouble("purge.volume");
		spawnSpeed = 120 - (8 * (level - 1));
		
		mobProbabilities = new int[] {
			2, //chain zombie
			2, //skeleton
			5, //tuneller
			3, //skeleton knight
			2, //dark mage
			3, //shadow leech
			3, //swamp beast
			3, //zombie knight
			5, //ghoul
			4, //psyco
			4, //scarecrow
			4 //vampire
		};
		for (int i = 0; i < mobProbabilities.length; i++)
			sum += mobProbabilities[i];
		
		this.type = Disaster.PURGE;
	}
	@Override
	public void start(Location loc, Player p) {
		custom = (boolean) WorldObject.findWorldObject(loc.getWorld()).settings.get("custom_mob_spawning");
		this.world = loc.getWorld();
		new RepeatingTask(plugin, 0, spawnSpeed) {
			public void run() {
				Entity king = Bukkit.getEntity(kingUUID);
				Iterator<UUID> it = entities.iterator();
				while (it.hasNext()) {
					LivingEntity e = (LivingEntity) Bukkit.getEntity(it.next());
					if (e == null)
						it.remove();
					else if (e.isDead()) {
						it.remove();
					}
				}
				if (king == null || king.isDead()) {
					new RepeatingTask(plugin, 0, despawnSpeed) {
						public void run() {
							UUID uuid = entities.poll();
							if (uuid == null)
								return;
							LivingEntity temp = (LivingEntity) Bukkit.getEntity(uuid);
							if (temp != null)
								temp.remove();
							if (entities.isEmpty())
								cancel();
						}
					};
					cancel();
					running = false;
					return;
				}
				
				for (Player player : king.getWorld().getPlayers()) {
					if (Utils.isPlayerImmune(player) || player.getLocation().distanceSquared(king.getLocation()) > 1600)
						continue;
					
					Location temp = Utils.findSmartYSpawn(player.getLocation(), Utils.getSpotInSquareRadius(player.getLocation(), spawnDistance), 2, 25);
					if (temp == null || temp.getBlock().getRelative(BlockFace.DOWN).isPassable())
						return;
					
					int r = rand.nextInt(sum);
					int cumalitive = 0;
					for (int i=0; i < mobProbabilities.length; i++) {
						cumalitive += mobProbabilities[i];
						if (r <= cumalitive) {
							r = i;
							break;
						}
					}
					Mob entity = null;
					switch (r) {
					default:
					case 0:
						entity = (Mob) temp.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
						entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);
						entity.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
						break;
					case 1:
						entity = (Mob) temp.getWorld().spawnEntity(temp, EntityType.SKELETON);
						entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.35);
						entity.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
						break;
					case 2:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
						CustomEntity.handler.addEntity(new TunnellerZombie((Zombie) entity, player, plugin));
						break;
					case 3:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.SKELETON);
						CustomEntity.handler.addEntity(new SkeletonKnight((Skeleton) entity, plugin));
						break;
					case 4:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
						CustomEntity.handler.addEntity(new DarkMage(entity, plugin));
						break;
					case 5:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
						CustomEntity.handler.addEntity(new ShadowLeech((Zombie) entity, plugin, rand));
						break;
					case 6:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
						CustomEntity.handler.addEntity(new SwampBeast(entity, plugin));
						break;
					case 7:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
						CustomEntity.handler.addEntity(new ZombieKnight(entity, plugin));
						break;
					case 8:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
						CustomEntity.handler.addEntity(new Ghoul((Zombie) entity, temp.getBlock().getRelative(BlockFace.DOWN), plugin, true));
						break;
					case 9:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.SKELETON);
						CustomEntity.handler.addEntity(new Psyco(entity, plugin, rand));
						break;
					case 10:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.ZOMBIE);
						CustomEntity.handler.addEntity(new Scarecrow((Zombie) entity, plugin, rand));
						break;
					case 11:
						if (!custom)
							break;
						entity = (Mob) player.getWorld().spawnEntity(temp, EntityType.EVOKER);
						CustomEntity.handler.addEntity(new Vampire(entity, plugin));
						break;
					}
					if (entity == null)
						continue;
					entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
					entity.setMetadata("dd-halloweenmobs", plugin.fixedData);
					entity.setTarget(player);
					entities.add(entity.getUniqueId());
					targetMap.put(entity.getUniqueId(), player.getUniqueId());
				}
			}
		};
		new RepeatingTask(plugin, 0, 5) {
			public void run() {
				if (!running) {
					cancel();
					return;
				}
				Iterator<Entry<UUID, UUID>> it = targetMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, UUID> entry = it.next();
					Mob entity = (Mob) Bukkit.getEntity(entry.getKey());
					if (entity == null || entity.isDead() || entity.getTarget() != null)
						continue;
					entity.setTarget(Bukkit.getPlayer(entry.getValue()));
				}
			}
		};
	}
	public void clearEntities() {
		for (UUID e : entities)
			if (Bukkit.getEntity(e) != null)
				Bukkit.getEntity(e).remove();
	}
	public Location findApplicableLocation(Location temp, Player p) {
		if (temp.getBlockY() < type.getMinHeight())
			return null;
		if ((boolean) WorldObject.findWorldObject(temp.getWorld()).settings.get("event_broadcast"))
			broadcastMessage(p.getLocation(), p);
		return temp;
	}
	public void startAdjustment(Location loc, Player p) {
		start(loc, p);
	}
	public void broadcastMessage(Location location, Player p) {
		if ((boolean) WorldObject.findWorldObject(location.getWorld()).settings.get("event_broadcast")) {
			String str = configFile.getString("messages.misc.purge.started");
			if (level == 1)
				str = "&a"+str;
			else if (level == 2)
				str = "&2"+str;
			else if (level == 3)
				str = "&b"+str;
			else if (level == 4)
				str = "&e"+str;
			else if (level == 5)
				str = "&c"+str;
			else if (level == 6)
				str = "&4"+str;
			str = Utils.convertString(str.replace("%level%", level+"").replace("%player%", p.getName()));
			if (configFile.getBoolean("messages.disaster_tips"))
				str += "\n"+type.getTip();
			for (Player all : location.getWorld().getPlayers())
				all.sendMessage(str);
			Main.consoleSender.sendMessage(Languages.prefix+str+ChatColor.GREEN+" ("+location.getWorld().getName()+")");
		}
	}
	public int getSpawnDistance() {
		return spawnDistance;
	}
	public void setSpawnDistance(int spawnDistance) {
		this.spawnDistance = spawnDistance;
	}
	public int getDespawnSpeed() {
		return despawnSpeed;
	}
	public void setDespawnSpeed(int despawnSpeed) {
		this.despawnSpeed = despawnSpeed;
	}
	public String getEndMessage() {
		return endMessage;
	}
	public void setEndMessage(String endMessage) {
		this.endMessage = endMessage;
	}
	public int getSpawnSpeed() {
		return spawnSpeed;
	}
	public void setSpawnSpeed(int spawnSpeed) {
		this.spawnSpeed = spawnSpeed;
	}
}
