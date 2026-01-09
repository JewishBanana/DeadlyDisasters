package com.github.jewishbanana.deadlydisasters.disasters.mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;
import com.github.jewishbanana.deadlydisasters.disasters.MobDisaster;
import com.github.jewishbanana.deadlydisasters.events.DisasterStartEvent.DisasterStartReason;
import com.github.jewishbanana.deadlydisasters.utils.DataUtils;
import com.github.jewishbanana.deadlydisasters.utils.DependencyUtils;
import com.github.jewishbanana.deadlydisasters.utils.EntityUtils;
import com.github.jewishbanana.deadlydisasters.utils.SpawnUtils;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class Purge extends Disaster implements MobDisaster {
	
	private static final Set<UUID> targetedPlayers;
	private static final String storedPlayersKey;
	public static final String purgeMobMetadata;
	static {
		targetedPlayers = new HashSet<>();
		storedPlayersKey = "purge.stored_players";
		purgeMobMetadata = "dd:pm";
	}
	
	private float entitySpawnDistance;
	private int maxHordeSize;
	private int hordeVanquishThreshold;
	
	private UUID targetUUID;
	private float entitySpawnRate;
	private BossBar bar;
	private List<EntityContainer> entityContainers;

	public Purge(@NotNull Location location, Player player, int level) {
		super(location, player, level);
	}
	public void init() {
		super.init();
		this.entitySpawnDistance = (float) getConfigDouble("entity_spawn_distance");
		this.maxHordeSize = getConfigInt("max_horde_size.level_"+level);
		this.hordeVanquishThreshold = getConfigInt("horde_vanquish_threshold.level_"+level);
		
		this.entitySpawnRate = (float) (0.05 * getConfigDouble("entity_spawn_rate") * level);
		if (getConfigBoolean("display_boss_bar"))
			this.bar = Bukkit.createBossBar(Utils.convertString(DataUtils.getLanguageString("disasters.features.purge_boss_bar_title")), BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);
		this.entityContainers = new ArrayList<>();
		List<Map<?, ?>> containers = getConfigMapList("entity_spawns");
		if (containers != null)
			containers.forEach(m -> {
				EntityContainer container = EntityContainer.createContainer(m, this);
				if (container == null)
					return;
				entityContainers.add(container);
			});
	}
	public boolean canStart() {
		if (player == null || targetedPlayers.contains(player.getUniqueId()))
			return false;
		int height = getLocation().getBlockY();
		if (height < -32 || height > 100)
			return false;
		return super.canStart();
	}
	public void start() {
		super.start();
		if (player == null || entityContainers.isEmpty()) {
			stop();
			return;
		}
		this.targetUUID = player.getUniqueId();
		targetedPlayers.add(targetUUID);
		playSound(location, Sound.EVENT_RAID_HORN, 100, .1);
		if (bar != null)
			bar.addPlayer(player);
		scheduleTask(new BukkitRunnable() {
			private int playerUpdateTick;
			private final double decrement = 1.0 / hordeVanquishThreshold;
			
			@Override
			public void run() {
				final Player player = Bukkit.getPlayer(targetUUID);
				if (player == null || !player.isOnline() || EntityUtils.isPlayerImmune(player) || !player.getWorld().equals(location.getWorld())) {
					if (player == null || !player.isOnline()) {
						Map<String, Integer> map = DataUtils.computeSection(DataUtils.getDataFile(), storedPlayersKey, new HashMap<String, Integer>());
						Integer value = map.get(targetUUID.toString());
						if (value == null || value < getLevel()) {
							map.put(targetUUID.toString(), getLevel());
							DataUtils.writeToDataFile(file -> file.set(storedPlayersKey, map));
						}
					}
					stop();
					return;
				}
				Set<UUID> set = getAllEntities();
				if (set != null) {
					Iterator<UUID> it = set.iterator();
					while (it.hasNext()) {
						Entity entity = Bukkit.getEntity(it.next());
						if (entity == null)
							it.remove();
						else if (entity.isDead()) {
							if (bar != null)
								bar.setProgress(Math.max(bar.getProgress() - decrement, 0));
							it.remove();
							if (--hordeVanquishThreshold == 0) {
								stop();
								return;
							}
						}
					}
				}
				final Location playerLoc = player.getLocation();
				if (playerUpdateTick-- == 0) {
					playerUpdateTick = 4;
					if (bar != null)
						player.getWorld().getPlayers().forEach(p -> {
							if (p.equals(player))
								return;
							if (p.getLocation().distanceSquared(playerLoc) > 900) {
								bar.removePlayer(p);
								return;
							}
							bar.addPlayer(p);
						});
					updateEntityTargets();
				}
				if ((set == null || set.size() < maxHordeSize) && random.nextFloat() < entitySpawnRate) {
					for (int i=0; i < 10; i++) {
						Location spawn = SpawnUtils.findMonsterSpawnLocation(playerLoc, 2, entitySpawnDistance, entitySpawnDistance + 5f);
						if (spawn == null)
							continue;
						if (spawn.getWorld().getNearbyEntities(spawn, Math.min(entitySpawnDistance-1, 15.0), Math.min(entitySpawnDistance-1, 10.0), Math.min(entitySpawnDistance-1, 15.0), e -> e instanceof Player p && !EntityUtils.isPlayerImmune(p)).stream().count() != 0)
							continue;
						EntityContainer container = rollEntity();
						if (container == null)
							continue;
						Entity entity = container.spawnEntity(spawn);
						if (!Utils.isAreaClear(spawn, (float) entity.getWidth(), (float) (entity.getHeight() - 0.2))) {
							entity.remove();
							continue;
						}
						addEntityToDisasterList(entity, player);
						entity.setMetadata(purgeMobMetadata, plugin.getFixedMetadata());
						if (entity instanceof Mob mob)
							mob.setTarget(player);
						break;
					}
				}
			}
		}.runTaskTimer(plugin, 0, 5));
	}
	public void clean() {
		super.clean();
		if (targetUUID != null)
			targetedPlayers.remove(targetUUID);
		if (bar != null)
			bar.removeAll();
		if (getWorldLink().getConfigBoolean("world.broadcast_disasters")) {
			Player player = Bukkit.getPlayer(targetUUID);
			if (player != null) {
				String endMessage = Utils.convertString(DataUtils.getLanguageString("messages.disaster_broadcasts.purge.ended"));
				player.sendMessage(endMessage);
				player.getNearbyEntities(30.0, 30.0, 30.0).forEach(e -> {
					if (e instanceof Player)
						e.sendMessage(endMessage);
				});
			}
		}
	}
	private EntityContainer rollEntity() {
		final float roll = random.nextFloat((float) entityContainers.stream().mapToDouble(t -> t.chance).sum());
		float cumulative = 0;
		for (EntityContainer container : entityContainers) {
			cumulative += container.chance;
			if (roll < cumulative)
				return container;
		}
		return null;
	}
	protected String getConfigPath() {
		return "disasters.mob.purge";
	}
	public String getBroadcastMessageConfigPath() {
		return "messages.disaster_broadcasts.purge.started.level_"+level;
	}
	public static void checkForPlayerInMap(Player player) {
		if (player == null)
			return;
		Map<String, Integer> map = DataUtils.computeSection(DataUtils.getDataFile(), storedPlayersKey, new HashMap<String, Integer>());
		Integer value = map.remove(player.getUniqueId().toString());
		if (value == null)
			return;
		DataUtils.writeToDataFile(file -> file.set(storedPlayersKey, map));
		Purge purge = new Purge(player.getLocation(), player, value);
		if (!purge.canStart(DisasterStartReason.CUSTOM))
			return;
		purge.init();
		purge.start();
	}
	
	private class EntityContainer {
		
		private Class<?> entityClass;
		private float chance = 5f;
		private double health;
		private double damage;
		private double speed;
		private boolean randomize = true;
		private ItemStack[] armor;
		private ItemStack mainHand;
		private ItemStack offHand;
		
		private boolean isUCType;
		private boolean chargedCreeper;
		
		@SuppressWarnings("unchecked")
		private Entity spawnEntity(Location location) {
			Entity entity = null;
			if (isUCType) {
				entity = com.github.jewishbanana.uiframework.entities.UIEntityManager.spawnEntity(location, (Class<? extends com.github.jewishbanana.uiframework.entities.CustomEntity<?>>) entityClass).getEntity();
				if (entity instanceof LivingEntity living) {
					if (health != 0) {
						living.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
						living.setHealth(health);
					}
					if (damage != 0)
						living.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
					if (speed != 0)
						living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
					if (armor != null)
						living.getEquipment().setArmorContents(armor);
					if (mainHand != null)
						plugin.getServer().getScheduler().runTaskLater(plugin, () -> living.getEquipment().setItemInMainHand(mainHand), 1);
					if (offHand != null)
						plugin.getServer().getScheduler().runTaskLater(plugin, () -> living.getEquipment().setItemInOffHand(offHand), 1);
					if (living instanceof Creeper creeper)
						creeper.setPowered(chargedCreeper);
					
					living.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(50.0);
				}
			} else
				entity = location.getWorld().spawn(location, (Class<? extends Entity>) entityClass, randomize, temp -> {
					if (temp instanceof LivingEntity living) {
						if (health != 0) {
							living.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
							living.setHealth(health);
						}
						if (damage != 0)
							living.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
						if (speed != 0)
							living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
						if (armor != null)
							living.getEquipment().setArmorContents(armor);
						if (mainHand != null)
							plugin.getServer().getScheduler().runTaskLater(plugin, () -> living.getEquipment().setItemInMainHand(mainHand), 1);
						if (offHand != null)
							plugin.getServer().getScheduler().runTaskLater(plugin, () -> living.getEquipment().setItemInOffHand(offHand), 1);
						if (temp instanceof Creeper creeper)
							creeper.setPowered(chargedCreeper);
						if (temp instanceof Ravager)
							temp.addPassenger(temp.getWorld().spawn(location, Pillager.class));
						
						living.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(50.0);
					}
				});
			return entity;
		}
		private static EntityContainer createContainer(Map<?, ?> map, Purge disaster) {
			Object typeName = map.get("type");
			if (typeName == null || !(typeName instanceof String typeString)) {
				Utils.sendConsoleMessage("&eWARNING there is an entity entry with no specified entity type for the Purge disaster in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"!");
				return null;
			}
			EntityContainer container = disaster.new EntityContainer();
			try {
				EntityType vanillaType = EntityType.valueOf(typeString.toUpperCase());
				container.entityClass = vanillaType.getEntityClass();
			} catch (IllegalArgumentException ex) {
				if (typeString.toLowerCase().startsWith("uc:")) {
					if (DependencyUtils.isUltimateContentEnabled()) {
						com.github.jewishbanana.uiframework.entities.UIEntityManager manager = com.github.jewishbanana.uiframework.entities.UIEntityManager.getEntityType(typeString.toLowerCase());
						if (manager == null) {
							Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry does not exist in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e!");
							Utils.sendConsoleMessage("&eWARNING a Purge entity entry was improperly entered and must be fixed in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e!");
							return null;
						}
						container.entityClass = manager.getEntityClass();
						container.isUCType = true;
					} else
						return null;
				} else {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry does not exist in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e!");
					Utils.sendConsoleMessage("&eWARNING a Purge entity entry was improperly entered and must be fixed in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e!");
					return null;
				}
			}
			if (map.containsKey("chance"))
				try {
					container.chance = (float) ((double) map.get("chance"));
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'chance' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("health"))
				try {
					container.health = (double) map.get("health");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'health' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("damage"))
				try {
					container.damage = (double) map.get("damage");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'damage' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("speed"))
				try {
					container.speed = (double) map.get("speed");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'speed' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("randomize"))
				try {
					container.randomize = (Boolean) map.get("randomize");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'randomize' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("feet"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("feet"));
					if (item != null) {
						if (container.armor == null)
							container.armor = new ItemStack[4];
						container.armor[0] = item;
					} else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'feet' &evalue, no such item &b'"+String.valueOf(map.get("feet"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'feet' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("legs"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("legs"));
					if (item != null) {
						if (container.armor == null)
							container.armor = new ItemStack[4];
						container.armor[1] = item;
					} else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'legs' &evalue, no such item &b'"+String.valueOf(map.get("legs"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'legs' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("chest"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("chest"));
					if (item != null) {
						if (container.armor == null)
							container.armor = new ItemStack[4];
						container.armor[2] = item;
					} else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'chest' &evalue, no such item &b'"+String.valueOf(map.get("chest"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'chest' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("head"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("head"));
					if (item != null) {
						if (container.armor == null)
							container.armor = new ItemStack[4];
						container.armor[3] = item;
					} else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'head' &evalue, no such item &b'"+String.valueOf(map.get("head"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'head' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("main_hand"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("main_hand"));
					if (item != null)
						container.mainHand = item;
					else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'main_hand' &evalue, no such item &b'"+String.valueOf(map.get("main_hand"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'main_hand' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("off_hand"))
				try {
					ItemStack item = createEquipmentItemStack((String) map.get("off_hand"));
					if (item != null)
						container.offHand = item;
					else
						Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'off_hand' &evalue, no such item &b'"+String.valueOf(map.get("off_hand"))+"' &eexists in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'off_hand' &evalue, only text values are acceptable! In the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			if (map.containsKey("charged"))
				try {
					container.chargedCreeper = (Boolean) map.get("charged");
				} catch (Exception e) {
					Utils.sendConsoleMessage("&eWARNING the entity type &d'"+typeString+"' &efor a Purge entity entry has an invalid &a'charged' &evalue in the world disaster config &b'"+disaster.getWorldLink().getConfigName()+"' &eat the section &cdisasters.mob.purge.entity_spawns&e. Something may not work properly!");
				}
			return container;
		}
		private static ItemStack createEquipmentItemStack(String item) {
			if (DependencyUtils.isUltimateContentEnabled()) {
				com.github.jewishbanana.uiframework.items.UIItemType itemType = com.github.jewishbanana.uiframework.items.UIItemType.getItemType(item);
				if (itemType != null)
					return itemType.getItem();
			}
			Material material = Material.getMaterial(item.toUpperCase());
			return material == null ? null : new ItemStack(material);
		}
	}
}
