package com.github.jewishbanana.deadlydisasters.listeners.spawners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedCreeper;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedDevourer;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedEnderman;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedHowler;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedSkeleton;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedSpirit;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedTribesman;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedWorm;
import com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities.InfestedZombie;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class InfestedSpawner implements Listener {
	
	private Main plugin;
	private Random rand;
	private EntityHandler handler;
	private InfestedSpawner instance;
	
	private Set<Chunk> handledChunks = new HashSet<>();
	private Set<UUID> nonNormalWorlds = new HashSet<>();
	
	public InfestedSpawner(Main plugin) {
		this.plugin = plugin;
		this.handler = CustomEntity.handler;
		this.rand = plugin.random;
		this.instance = this;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		try {
			if (nonNormalWorlds.contains(e.getWorld().getUID()) || !Utils.isEnvironment(e.getWorld(), Environment.NORMAL) || rand.nextInt(4) != 0 || GlobalSpawner.noSpawnWorlds.contains(e.getWorld()) || handledChunks.contains(e.getChunk()) || e.getChunk().getBlock(0, -32, 0).getBiome() != Biome.DEEP_DARK)
				return;
		} catch (Exception exception) {
			nonNormalWorlds.add(e.getWorld().getUID());
			return;
		}
		handledChunks.add(e.getChunk());
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Location loc = null;
				try {
					loc = Utils.findSmartYSpawn(e.getChunk().getBlock(0, -32, 0).getLocation(), e.getChunk().getBlock(0, -32, 0).getLocation(), 3, 28);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					Main.consoleSender.sendMessage(Utils.chat("&e[DeadlyDisasters]: You are most likely using a bugged version of paper that does not support getting chunks below Y=0! The plugin will disable natural spawning of infested mobs."));
					HandlerList.unregisterAll(instance);
					return;
				}
				if (loc == null)
					return;
				CustomEntityType[] customType = {null};
				List<Integer> order = new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9));
				Collections.shuffle(order);
				label:
				for (int i : order) {
					switch (i) {
					case 1:
						if (rand.nextDouble()*100 < CustomEntityType.INFESTEDCREEPER.getSpawnRate()) {
							customType[0] = CustomEntityType.INFESTEDCREEPER;
							break label;
						}
						break;
					case 2:
						if (rand.nextDouble()*100 < CustomEntityType.INFESTEDDEVOURER.getSpawnRate()) {
							customType[0] = CustomEntityType.INFESTEDDEVOURER;
							break label;
						}
						break;
					case 3:
						if (rand.nextDouble()*100 < CustomEntityType.INFESTEDENDERMAN.getSpawnRate()) {
							customType[0] = CustomEntityType.INFESTEDENDERMAN;
							break label;
						}
						break;
					case 4:
						if (rand.nextDouble()*100 < CustomEntityType.INFESTEDHOWLER.getSpawnRate()) {
							customType[0] = CustomEntityType.INFESTEDHOWLER;
							break label;
						}
						break;
					case 5:
						if (rand.nextDouble()*100 < CustomEntityType.INFESTEDSKELETON.getSpawnRate()) {
							customType[0] = CustomEntityType.INFESTEDSKELETON;
							break label;
						}
						break;
					case 6:
						if (rand.nextDouble()*100 < CustomEntityType.INFESTEDSPIRIT.getSpawnRate()) {
							customType[0] = CustomEntityType.INFESTEDSPIRIT;
							break label;
						}
						break;
					case 7:
						if (rand.nextDouble()*100 < CustomEntityType.INFESTEDTRIBESMAN.getSpawnRate()) {
							customType[0] = CustomEntityType.INFESTEDTRIBESMAN;
							break label;
						}
						break;
					case 8:
						if (rand.nextDouble()*100 < CustomEntityType.INFESTEDWORM.getSpawnRate()) {
							customType[0] = CustomEntityType.INFESTEDWORM;
							break label;
						}
						break;
					case 9:
						if (rand.nextDouble()*100 < CustomEntityType.INFESTEDZOMBIE.getSpawnRate()) {
							customType[0] = CustomEntityType.INFESTEDZOMBIE;
							break label;
						}
						break;
					}
				}
				if (customType[0] != null) {
					Location spawn = loc;
					plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
						@Override
						public void run() {
							Mob entity;
							switch (customType[0]) {
							case INFESTEDCREEPER:
								entity = (Mob) spawn.getWorld().spawnEntity(spawn, EntityType.CREEPER);
								handler.addEntity(new InfestedCreeper((Creeper) entity, plugin));
								return;
							case INFESTEDDEVOURER:
								entity = (Mob) spawn.getWorld().spawnEntity(spawn, EntityType.ZOMBIE);
								handler.addEntity(new InfestedDevourer((Zombie) entity, plugin, rand));
								return;
							case INFESTEDENDERMAN:
								entity = (Mob) spawn.getWorld().spawnEntity(spawn, EntityType.ENDERMAN);
								handler.addEntity(new InfestedEnderman(entity, plugin));
								return;
							case INFESTEDHOWLER:
								entity = (Mob) spawn.getWorld().spawnEntity(spawn, EntityType.ZOMBIE);
								handler.addEntity(new InfestedHowler((Zombie) entity, plugin, rand));
								return;
							case INFESTEDSKELETON:
								entity = (Mob) spawn.getWorld().spawnEntity(spawn, EntityType.WITHER_SKELETON);
								handler.addEntity(new InfestedSkeleton(entity, plugin));
								return;
							case INFESTEDSPIRIT:
								entity = (Mob) spawn.getWorld().spawnEntity(spawn, EntityType.VEX);
								handler.addEntity(new InfestedSpirit(entity, plugin, rand));
								return;
							case INFESTEDTRIBESMAN:
								for (int i=0; i < 4; i++) {
									entity = (Mob) spawn.getWorld().spawnEntity(spawn, EntityType.ZOMBIE);
									handler.addEntity(new InfestedTribesman((Zombie) entity, plugin, rand));
								}
								return;
							case INFESTEDWORM:
								List<BlockFace> faceList = new ArrayList<>(Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST));
								Collections.shuffle(faceList);
								for (BlockFace face : faceList)
									if (!spawn.getBlock().getRelative(face).isPassable()) {
										BlockFace oppositeFace = null;
										switch (face) {
										default:
										case UP:
											oppositeFace = BlockFace.DOWN;
											break;
										case DOWN:
											oppositeFace = BlockFace.UP;
											break;
										case NORTH:
											oppositeFace = BlockFace.SOUTH;
											break;
										case EAST:
											oppositeFace = BlockFace.WEST;
											break;
										case SOUTH:
											oppositeFace = BlockFace.NORTH;
											break;
										case WEST:
											oppositeFace = BlockFace.EAST;
											break;
										}
										handler.addFalseEntity(new InfestedWorm(spawn.getBlock().getRelative(face), oppositeFace, plugin, rand));
										return;
									}
								return;
							case INFESTEDZOMBIE:
								entity = (Mob) spawn.getWorld().spawnEntity(spawn, EntityType.ZOMBIE);
								handler.addEntity(new InfestedZombie(entity, plugin));
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
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent e) {
		if (handledChunks.contains(e.getChunk()))
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> handledChunks.remove(e.getChunk()), 200);
	}
}
