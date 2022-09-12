package deadlydisasters.listeners.spawners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.ItemStack;

import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.EntityHandler;
import deadlydisasters.entities.endstormentities.BabyEndTotem;
import deadlydisasters.entities.endstormentities.EndTotem;
import deadlydisasters.entities.endstormentities.EndWorm;
import deadlydisasters.entities.endstormentities.VoidArcher;
import deadlydisasters.entities.endstormentities.VoidGuardian;
import deadlydisasters.entities.endstormentities.VoidStalker;
import deadlydisasters.entities.infestedcavesentities.InfestedCreeper;
import deadlydisasters.entities.infestedcavesentities.InfestedDevourer;
import deadlydisasters.entities.infestedcavesentities.InfestedEnderman;
import deadlydisasters.entities.infestedcavesentities.InfestedHowler;
import deadlydisasters.entities.infestedcavesentities.InfestedSkeleton;
import deadlydisasters.entities.infestedcavesentities.InfestedSpirit;
import deadlydisasters.entities.infestedcavesentities.InfestedTribesman;
import deadlydisasters.entities.infestedcavesentities.InfestedWorm;
import deadlydisasters.entities.infestedcavesentities.InfestedZombie;
import deadlydisasters.entities.monsoonentities.CursedDiver;
import deadlydisasters.entities.purgeentities.DarkMage;
import deadlydisasters.entities.purgeentities.PrimedCreeper;
import deadlydisasters.entities.purgeentities.ShadowLeech;
import deadlydisasters.entities.purgeentities.SkeletonKnight;
import deadlydisasters.entities.purgeentities.SwampBeast;
import deadlydisasters.entities.purgeentities.TunnellerZombie;
import deadlydisasters.entities.purgeentities.ZombieKnight;
import deadlydisasters.entities.sandstormentities.AncientMummy;
import deadlydisasters.entities.sandstormentities.AncientSkeleton;
import deadlydisasters.entities.snowstormentities.Yeti;
import deadlydisasters.entities.solarstormentities.FirePhantom;
import deadlydisasters.entities.soulstormentities.LostSoul;
import deadlydisasters.entities.soulstormentities.SoulReaper;
import deadlydisasters.general.Main;
import deadlydisasters.general.SeasonsHandler;
import deadlydisasters.general.WorldObject;

public class GlobalSpawner implements Listener {
	
	private Main plugin;
	private EntityHandler handler;
	private Random rand;
	
	public static Set<World> noSpawnWorlds = new HashSet<>();
	
	public GlobalSpawner(Main plugin, EntityHandler handler) {
		this.plugin = plugin;
		this.handler = handler;
		this.rand = plugin.random;
		reload(plugin);
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority=EventPriority.MONITOR)
	public void onSpawn(CreatureSpawnEvent e) {
		if (e.isCancelled() || e.getSpawnReason() != SpawnReason.NATURAL || noSpawnWorlds.contains(e.getLocation().getWorld()) || !(e.getEntity() instanceof Monster) || !e.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid())
			return;
		LivingEntity spawnedEntity = e.getEntity();
		EntityType type = spawnedEntity.getType();
		Location loc = e.getLocation();
		Biome biome = loc.getBlock().getBiome();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				CustomEntityType[] customType = {null};
				if (type == EntityType.ENDERMAN && loc.getWorld().getEnvironment() == Environment.THE_END) {
					List<Integer> order = new ArrayList<>(Arrays.asList(1,2,3,4,5,6));
					Collections.shuffle(order);
					label:
					for (int i : order) {
						switch (i) {
						case 1:
							if (rand.nextDouble() * 100 < CustomEntityType.ENDTOTEM.getSpawnRate()) {
								customType[0] = CustomEntityType.ENDTOTEM;
								break label;
							}
							break;
						case 2:
							if (rand.nextDouble() * 100 < CustomEntityType.ENDWORM.getSpawnRate()) {
								customType[0] = CustomEntityType.ENDWORM;
								break label;
							}
							break;
						case 3:
							if (rand.nextDouble() * 100 < CustomEntityType.VOIDARCHER.getSpawnRate()) {
								customType[0] = CustomEntityType.VOIDARCHER;
								break label;
							}
							break;
						case 4:
							if (rand.nextDouble() * 100 < CustomEntityType.VOIDGUARDIAN.getSpawnRate()) {
								customType[0] = CustomEntityType.VOIDGUARDIAN;
								break label;
							}
							break;
						case 5:
							if (rand.nextDouble() * 100 < CustomEntityType.VOIDSTALKER.getSpawnRate()) {
								customType[0] = CustomEntityType.VOIDSTALKER;
								break label;
							}
							break;
						case 6:
							if (rand.nextDouble() * 100 < CustomEntityType.BABYENDTOTEM.getSpawnRate()) {
								customType[0] = CustomEntityType.BABYENDTOTEM;
								break label;
							}
							break;
						}
					}
				} else if (plugin.mcVersion >= 1.16 && biome == Biome.SOUL_SAND_VALLEY) {
					List<Integer> order = new ArrayList<>(Arrays.asList(1,2));
					Collections.shuffle(order);
					label:
					for (int i : order) {
						switch (i) {
						case 1:
							if (rand.nextDouble()*100 < CustomEntityType.LOSTSOUL.getSpawnRate()) {
								customType[0] = CustomEntityType.LOSTSOUL;
								break label;
							}
							break;
						case 2:
							if (rand.nextDouble()*100 < CustomEntityType.SOULREAPER.getSpawnRate()) {
								customType[0] = CustomEntityType.SOULREAPER;
								break label;
							}
							break;
						}
					}
				} else if (biome == Biome.DESERT || biome == Biome.BADLANDS || biome == Biome.ERODED_BADLANDS || (plugin.mcVersion >= 1.18 && biome == Biome.WOODED_BADLANDS)) {
					List<Integer> order = new ArrayList<>(Arrays.asList(1,2));
					Collections.shuffle(order);
					label:
					for (int i : order) {
						switch (i) {
						case 1:
							if (rand.nextDouble()*100 < CustomEntityType.ANCIENTMUMMY.getSpawnRate()) {
								customType[0] = CustomEntityType.ANCIENTMUMMY;
								break label;
							}
							break;
						case 2:
							if (rand.nextDouble()*100 < CustomEntityType.ANCIENTSKELETON.getSpawnRate() && loc.getBlock().getRelative(BlockFace.UP, 2).isPassable()) {
								customType[0] = CustomEntityType.ANCIENTSKELETON;
								break label;
							}
							break;
						}
					}
				} else if (loc.getWorld().getEnvironment() == Environment.NORMAL) {
					List<Integer> order = new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9,10));
					Collections.shuffle(order);
					label:
					for (int i : order) {
						switch (i) {
						case 1:
							if (rand.nextDouble()*100 < CustomEntityType.YETI.getSpawnRate() && loc.getBlock().getRelative(BlockFace.UP, 2).isPassable()
									&& ((!plugin.seasonsHandler.isActive && loc.getBlock().getTemperature() <= 0.15) || (plugin.seasonsHandler.isActive && SeasonsHandler.getSeasonsAPI().getSeason(loc.getWorld()) == me.casperge.realisticseasons.season.Season.WINTER))) {
								customType[0] = CustomEntityType.YETI;
								break label;
							}
							break;
						case 2:
							if (rand.nextDouble()*100 < CustomEntityType.PRIMEDCREEPER.getSpawnRate()) {
								customType[0] = CustomEntityType.PRIMEDCREEPER;
								break label;
							}
							break;
						case 3:
							if (rand.nextDouble()*100 < CustomEntityType.SKELETONKNIGHT.getSpawnRate() && loc.getBlock().getRelative(BlockFace.UP, 2).isPassable()) {
								customType[0] = CustomEntityType.SKELETONKNIGHT;
								break label;
							}
							break;
						case 4:
							if (rand.nextDouble()*100 < CustomEntityType.DARKMAGE.getSpawnRate()) {
								customType[0] = CustomEntityType.DARKMAGE;
								break label;
							}
							break;
						case 5:
							if (loc.getBlockY() < 50 && rand.nextDouble()*100 < CustomEntityType.TUNNELLER.getSpawnRate()) {
								customType[0] = CustomEntityType.TUNNELLER;
								break label;
							}
							break;
						case 6:
							if (type == EntityType.DROWNED && rand.nextDouble()*100 < CustomEntityType.CURSEDDIVER.getSpawnRate()) {
								customType[0] = CustomEntityType.CURSEDDIVER;
								break label;
							}
							break;
						case 7:
							if (rand.nextDouble()*100 < CustomEntityType.FIREPHANTOM.getSpawnRate() && loc.getY() > 65 && loc.getBlock().getRelative(BlockFace.UP, 10).isPassable()
									&& loc.getBlock().getTemperature() > 0.8 && (!plugin.seasonsHandler.isActive || SeasonsHandler.getSeasonsAPI().getSeason(loc.getWorld()) == me.casperge.realisticseasons.season.Season.SUMMER)) {
								customType[0] = CustomEntityType.FIREPHANTOM;
								break label;
							}
							break;
						case 8:
							if (loc.getBlock().getLightFromBlocks() <= (byte) 0 && rand.nextDouble()*100 < CustomEntityType.SHADOWLEECH.getSpawnRate()) {
								customType[0] = CustomEntityType.SHADOWLEECH;
								break label;
							}
							break;
						case 9:
							if (rand.nextDouble()*100 < CustomEntityType.ZOMBIEKNIGHT.getSpawnRate() && loc.getBlock().getRelative(BlockFace.UP, 2).isPassable()) {
								customType[0] = CustomEntityType.ZOMBIEKNIGHT;
								break label;
							}
							break;
						case 10:
							if (rand.nextDouble()*100 < CustomEntityType.SWAMPBEAST.getSpawnRate() && (biome == Biome.SWAMP || (plugin.mcVersion >= 1.19 && biome == Biome.MANGROVE_SWAMP))) {
								customType[0] = CustomEntityType.SWAMPBEAST;
								break label;
							}
							break;
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
							case ENDTOTEM:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
								entity.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
								handler.addEntity(new EndTotem(entity, plugin, rand));
								return;
							case BABYENDTOTEM:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WOLF);
								handler.addEntity(new BabyEndTotem(entity, plugin.dataFile, plugin, rand));
								return;
							case ENDWORM:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								handler.addEntity(new EndWorm(entity, plugin, rand));
								return;
							case VOIDARCHER:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
								handler.addEntity(new VoidArcher(entity, plugin, rand));
								return;
							case VOIDGUARDIAN:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								handler.addEntity(new VoidGuardian(entity, plugin, rand));
								return;
							case VOIDSTALKER:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.PHANTOM);
								handler.addEntity(new VoidStalker(entity, plugin, rand));
								return;
							case LOSTSOUL:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.VEX);
								handler.addEntity(new LostSoul(entity, plugin, rand));
								return;
							case SOULREAPER:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
								handler.addEntity(new SoulReaper(entity, plugin, rand));
								return;
							case ANCIENTMUMMY:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.HUSK);
								handler.addEntity(new AncientMummy(entity, plugin, rand));
								return;
							case ANCIENTSKELETON:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
								handler.addEntity(new AncientSkeleton(entity, plugin, rand));
								return;
							case PRIMEDCREEPER:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.CREEPER);
								handler.addEntity(new PrimedCreeper(entity, plugin));
								return;
							case SKELETONKNIGHT:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
								handler.addEntity(new SkeletonKnight((Skeleton) entity, plugin));
								return;
							case DARKMAGE:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								entity.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
								handler.addEntity(new DarkMage(entity, plugin));
							case TUNNELLER:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								handler.addEntity(new TunnellerZombie((Zombie) entity, null, plugin));
								return;
							case YETI:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.IRON_GOLEM);
								handler.addEntity(new Yeti(entity, plugin, rand));
								return;
							case CURSEDDIVER:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.DROWNED);
								handler.addEntity(new CursedDiver(entity, plugin, rand));
								return;
							case INFESTEDCREEPER:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.CREEPER);
								handler.addEntity(new InfestedCreeper((Creeper) entity, plugin));
								return;
							case INFESTEDDEVOURER:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								handler.addEntity(new InfestedDevourer((Zombie) entity, plugin, rand));
								return;
							case INFESTEDENDERMAN:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ENDERMAN);
								handler.addEntity(new InfestedEnderman(entity, plugin));
								return;
							case INFESTEDHOWLER:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								handler.addEntity(new InfestedHowler((Zombie) entity, plugin, rand));
								return;
							case INFESTEDSKELETON:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
								handler.addEntity(new InfestedSkeleton(entity, plugin));
								return;
							case INFESTEDSPIRIT:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.VEX);
								handler.addEntity(new InfestedSpirit(entity, plugin, rand));
								return;
							case INFESTEDTRIBESMAN:
								for (int i=0; i < 4; i++) {
									entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
									handler.addEntity(new InfestedTribesman((Zombie) entity, plugin, rand));
								}
								return;
							case INFESTEDWORM:
								List<BlockFace> faceList = new ArrayList<>(Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST));
								Collections.shuffle(faceList);
								for (BlockFace face : faceList)
									if (!loc.getBlock().getRelative(face).isPassable()) {
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
										handler.addFalseEntity(new InfestedWorm(loc.getBlock().getRelative(face), oppositeFace, plugin, rand));
										return;
									}
								return;
							case INFESTEDZOMBIE:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								handler.addEntity(new InfestedZombie(entity, plugin));
								return;
							case FIREPHANTOM:
								entity = (Mob) loc.getWorld().spawnEntity(loc.clone().add(0,10,0), EntityType.PHANTOM);
								handler.addEntity(new FirePhantom(entity, plugin, rand));
								return;
							case SHADOWLEECH:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								handler.addEntity(new ShadowLeech((Zombie) entity, plugin, rand));
								return;
							case ZOMBIEKNIGHT:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								handler.addEntity(new ZombieKnight((Zombie) entity, plugin));
								return;
							case SWAMPBEAST:
								entity = (Mob) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
								handler.addEntity(new SwampBeast(entity, plugin));
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
