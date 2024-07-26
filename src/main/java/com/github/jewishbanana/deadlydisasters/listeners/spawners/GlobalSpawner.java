package com.github.jewishbanana.deadlydisasters.listeners.spawners;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Goat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Elf;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Frosty;
import com.github.jewishbanana.deadlydisasters.entities.christmasentities.Grinch;
import com.github.jewishbanana.deadlydisasters.entities.easterentities.RampagingGoat;
import com.github.jewishbanana.deadlydisasters.handlers.SeasonsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.WorldObject;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class GlobalSpawner implements Listener {
	
	private Main plugin;
	private Random rand;
	private boolean spawnChristmas;
	
	public static Set<World> noSpawnWorlds = new HashSet<>();
	
	public GlobalSpawner(Main plugin) {
		this.plugin = plugin;
		this.rand = new Random();
		reload(plugin);
		
		LocalDate date = LocalDate.now();
		if (date.getMonth() == Month.DECEMBER)
			spawnChristmas = true;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority=EventPriority.MONITOR)
	public void onSpawn(CreatureSpawnEvent e) {
		if (e.isCancelled() || e.getSpawnReason() != SpawnReason.NATURAL || noSpawnWorlds.contains(e.getLocation().getWorld()) || !e.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid())
			return;
		LivingEntity spawnedEntity = e.getEntity();
		Location loc = e.getLocation();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				CustomEntityType[] customType = {null};
				if (Utils.isEnvironment(loc.getWorld(), Environment.NORMAL)) {
					if (!(e.getEntity() instanceof Monster))
						return;
					if (customType[0] == null && spawnChristmas && (plugin.seasonsHandler.isActive ? SeasonsHandler.getSeasonsAPI().getSeason(loc.getWorld()) == me.casperge.realisticseasons.season.Season.WINTER : loc.getBlock().getTemperature() <= 0.15)) {
						List<Integer> order = new ArrayList<>(Arrays.asList(1,2,3));
						Collections.shuffle(order);
						label:
						for (int i : order) {
							switch (i) {
							case 1:
								if (rand.nextDouble()*100 < 4.0) {
									customType[0] = CustomEntityType.CHRISTMASELF;
									break label;
								}
								break;
							case 2:
								if (rand.nextDouble()*100 < 2.0) {
									customType[0] = CustomEntityType.FROSTY;
									Bukkit.broadcastMessage("1");
									break label;
								}
								break;
							case 3:
								if (rand.nextDouble()*100 < 3.0) {
									customType[0] = CustomEntityType.GRINCH;
									break label;
								}
								break;
							}
						}
					}
				}
				if (customType[0] != null) {
					plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
						@Override
						public void run() {
							Mob entity;
							if (spawnedEntity == null)
								return;
							spawnedEntity.remove();
							switch (customType[0]) {
							case CHRISTMASELF:
								for (int i=0; i < 3; i++) {
									entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
									CustomEntity.handler.addEntity(new Elf((Zombie) entity, plugin, rand));
								}
								return;
							case FROSTY:
								entity = (Mob) loc.getWorld().spawn(loc, Snowman.class);
								CustomEntity.handler.addEntity(new Frosty((Snowman) entity, plugin, rand));
								return;
							case GRINCH:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								CustomEntity.handler.addEntity(new Grinch(entity, plugin, rand));
								return;
							case RAMPAGINGGOAT:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.GOAT);
								CustomEntity.handler.addEntity(new RampagingGoat((Goat) entity, plugin));
								return;
							default:
								return;
							}
						}
					});
				}
			}
		});
	}
	public static void reload(Main plugin) {
		noSpawnWorlds.clear();
		for (WorldObject obj : WorldObject.worlds)
			if (!((boolean) obj.settings.get("custom_mob_spawning")))
				noSpawnWorlds.add(obj.getWorld());
	}
}
