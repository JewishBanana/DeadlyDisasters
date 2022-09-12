package deadlydisasters.listeners;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.Monsoon;
import deadlydisasters.disasters.events.DisasterEvent;
import deadlydisasters.entities.CustomEntityType;
import deadlydisasters.entities.soulstormentities.TamedLostSoul;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class CustomEnchantHandler implements Listener {
	
	private Main plugin;
	private Random rand;
	
	private Map<UUID,Integer> ancientBladeCooldownMap = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> soulRipperCooldownMap = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> poseidonsTridentCooldown = new HashMap<UUID,Integer>();
//	private Map<UUID,Vector> playerMoveMap = new ConcurrentHashMap<UUID,Vector>();
//	private Map<UUID,Location> playerMoveLocationMap = new ConcurrentHashMap<UUID,Location>();
//	private Map<UUID,Integer> playerMoveCooldown = new HashMap<UUID,Integer>();
	
	private int ancientCurseFireTicks;
	private int ancientCurseLifeTicks;
	private int ancientCurseParticleCount;
	private int[] yetisBlessingRange;
	private int[] yetisBlessingChance;
	private int[] poseidonsTridentRange;
	
	public CustomEnchantHandler(Main plugin) {
		this.plugin = plugin;
		this.rand = plugin.random;
		
		reload();
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				Iterator<Entry<UUID, Integer>> it = ancientBladeCooldownMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
				it = soulRipperCooldownMap.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
				it = poseidonsTridentCooldown.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					entry.setValue(entry.getValue() - 1);
					if (entry.getValue() <= 0)
						it.remove();
				}
//				it = playerMoveCooldown.entrySet().iterator();
//				while (it.hasNext()) {
//					Entry<UUID, Integer> entry = it.next();
//					entry.setValue(entry.getValue() - 1);
//					if (entry.getValue() <= 0)
//						it.remove();
//				}
				Iterator<Entry<UUID, Map<DisasterEvent, Integer>>> iterator = DisasterEvent.countdownMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<UUID, Map<DisasterEvent, Integer>> entry = iterator.next();
					Iterator<Entry<DisasterEvent, Integer>> internal = entry.getValue().entrySet().iterator();
					while (internal.hasNext()) {
						Entry<DisasterEvent, Integer> map = internal.next();
						map.setValue(map.getValue() - 1);
						if (map.getValue() <= 0) {
							internal.remove();
							if (entry.getValue().isEmpty())
								iterator.remove();
						}
					}
				}
			}
		}, 0, 20);
	}
	@EventHandler
	public void onAttack(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof LivingEntity) || !(e.getDamager() instanceof LivingEntity))
			return;
		LivingEntity entity = (LivingEntity) e.getEntity();
		LivingEntity damager = (LivingEntity) e.getDamager();
		if (damager.getEquipment().getItemInMainHand().getType() == Material.IRON_HOE) {
			ItemStack item = damager.getEquipment().getItemInMainHand();
			if (CustomEntityType.TAMEDLOSTSOUL.canSpawn() && !soulRipperCooldownMap.containsKey(damager.getUniqueId()) && item.getItemMeta().hasLore() && item.getItemMeta().getLore().get(0).equals(ItemsHandler.soulRipperLore)) {
				if (damager instanceof Player && !Utils.isPlayerImmune((Player) damager))
					soulRipperCooldownMap.put(damager.getUniqueId(), ItemsHandler.soulRipperCooldown);
				else
					soulRipperCooldownMap.put(damager.getUniqueId(), ItemsHandler.soulRipperCooldown);
				spawnSouls(damager.getLocation(), entity);
				if (damager instanceof Player && !Utils.isPlayerImmune((Player) damager)) {
					ItemMeta meta = item.getItemMeta();
					((Damageable) meta).setDamage(((Damageable) meta).getDamage() + 10);
					if (((Damageable) meta).getDamage() >= item.getType().getMaxDurability())
						item.setAmount(0);
					else
						item.setItemMeta(meta);
				}
			}
		}
		if (e.getCause() == DamageCause.ENTITY_ATTACK && entity.getEquipment().getChestplate() != null && Utils.levelOfEnchant(Languages.langFile.getString("misc.yetiBlessing"), entity.getEquipment().getChestplate()) > 0) {
			int level = Utils.levelOfEnchant(Languages.langFile.getString("misc.yetiBlessing"), entity.getEquipment().getChestplate());
			double[] data = {yetisBlessingChance[0], yetisBlessingRange[0]};
			if (level == 2) {
				data[0] = yetisBlessingChance[1];
				data[1] = yetisBlessingRange[1];
			} else if (level == 3) {
				data[0] = yetisBlessingChance[2];
				data[1] = yetisBlessingRange[2];
			}
			if (rand.nextInt(100) < data[0])
				yetiRoar(entity, level, data);
		}
	}
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getItem() == null || !e.getItem().hasItemMeta())
			return;
		Material type = e.getItem().getType();
		if (((plugin.mcVersion >= 1.16 && type == Material.NETHERITE_SWORD && e.getPlayer().getAttackCooldown() == 1) || (plugin.mcVersion < 1.16 && type == Material.DIAMOND_SWORD)) && e.getAction() == Action.LEFT_CLICK_AIR && !ancientBladeCooldownMap.containsKey(e.getPlayer().getUniqueId())
				&& e.getItem().getItemMeta().hasLore() && e.getItem().getItemMeta().getLore().get(0).equals(ItemsHandler.ancientCurseName)) {
			if (e.getPlayer() instanceof Player && !Utils.isPlayerImmune(e.getPlayer()))
				ancientBladeCooldownMap.put(e.getPlayer().getUniqueId(), ItemsHandler.ancientBladeCooldown);
			int[] spellLife = {ancientCurseLifeTicks};
			Vector motion = e.getPlayer().getEyeLocation().getDirection().clone();
			Location spell = e.getPlayer().getEyeLocation().clone().add(motion.clone().multiply(2));
			e.getPlayer().getWorld().playSound(spell, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1f, .6f);
			World tempW = spell.getWorld();
			BlockData bd = Material.SAND.createBlockData();
			new RepeatingTask(plugin, 0, 1) {
				@Override
				public void run() {
					if (spellLife[0] <= 0) {
						cancel();
						return;
					}
					spellLife[0]--;
					spell.add(motion);
					tempW.spawnParticle(Particle.FLAME, spell, ancientCurseParticleCount, 1, 1, 1, .05);
					tempW.spawnParticle(Particle.BLOCK_DUST, spell, ancientCurseParticleCount, 1, 1, 1, .1, bd);
					for (Entity e : spell.getWorld().getNearbyEntities(spell, 1.5, 1.5, 1.5))
						if (e instanceof LivingEntity) {
							e.setFireTicks(ancientCurseFireTicks);
							e.setVelocity(motion.clone().multiply(0.5));
						}
				}
			};
		}
	}
	@EventHandler
	public void onShoot(ProjectileLaunchEvent e) {
		if (e.getEntityType() == EntityType.TRIDENT && e.getEntity().getShooter() instanceof LivingEntity && ((Trident) e.getEntity()).getItem().getItemMeta().getPersistentDataContainer().has(ItemsHandler.poseidonsTridentKey, PersistentDataType.BYTE)
				&& !poseidonsTridentCooldown.containsKey(((LivingEntity) e.getEntity().getShooter()).getUniqueId())) {
			LivingEntity entity = (LivingEntity) e.getEntity().getShooter();
			if (entity instanceof Player && !Utils.isPlayerImmune((Player) entity))
				poseidonsTridentCooldown.put(entity.getUniqueId(), ItemsHandler.poseidonsTridentCooldown);
			else
				soulRipperCooldownMap.put(entity.getUniqueId(), ItemsHandler.soulRipperCooldown);
			castWaveSpell(entity.getLocation(), e.getEntity().getVelocity().normalize(), poseidonsTridentRange[0], rand, entity);
		}
	}
//	@EventHandler
//	public void onPlayerMove(PlayerMoveEvent e) {
//		UUID uuid = e.getPlayer().getUniqueId();
//		if (e.getPlayer().isFlying() || playerMoveCooldown.containsKey(uuid) || e.getFrom().distanceSquared(e.getTo()) <= 0.0225)
//			return;
//		e.getPlayer().sendMessage(""+e.getFrom().distance(e.getTo()));
//		if (playerMoveMap.containsKey(uuid)) {
//			Vector vec = Utils.getVectorTowards(e.getFrom(), e.getTo());
//			Vector history = playerMoveMap.get(uuid);
////			e.getPlayer().sendMessage(Utils.chat("&bTried "+(history.getX()*history.getZ()) / (vec.getX()*vec.getZ())));
//			if ((history.getX()*history.getZ()) / (vec.getX()*vec.getZ()) <= 1.0 && playerMoveLocationMap.get(uuid).distanceSquared(e.getTo()) <= 0.25) {
//				e.getPlayer().setVelocity(vec.multiply(2).setY(0.3));
//				playerMoveCooldown.put(uuid, 2);
//				e.getPlayer().sendMessage(Utils.chat("&aDodged"));
//			}
//			playerMoveMap.remove(uuid);
//			playerMoveLocationMap.remove(uuid);
//		} else {
//			playerMoveMap.put(uuid, Utils.getVectorTowards(e.getFrom(), e.getTo()));
//			playerMoveLocationMap.put(uuid, e.getFrom());
//			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
//				@Override
//				public void run() {
//					playerMoveMap.remove(uuid);
//					playerMoveLocationMap.remove(uuid);
//				}
//			}, 2);
//		}
//	}
	public void reload() {
		ancientCurseFireTicks = plugin.getConfig().getInt("customitems.enchants.ancient_curse.fire_ticks");
		ancientCurseLifeTicks = plugin.getConfig().getInt("customitems.enchants.ancient_curse.spell_life_ticks");
		ancientCurseParticleCount = plugin.getConfig().getInt("customitems.enchants.ancient_curse.particle_count");
		yetisBlessingRange = new int[] {plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 1.range"), plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 2.range"), plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 3.range")};
		yetisBlessingChance = new int[] {plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 1.chance")-1, plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 2.chance")-1, plugin.getConfig().getInt("customitems.enchants.yetis_blessing.level 3.chance")-1};
		poseidonsTridentRange = new int[] {plugin.getConfig().getInt("customitems.enchants.tidal_wave.level 1.range")};
	}
	private void spawnSouls(Location loc, LivingEntity entity)  {
		LivingEntity[] souls = new LivingEntity[ItemsHandler.soulRipperNumberOfSouls];
		for (int i=0; i < ItemsHandler.soulRipperNumberOfSouls; i++) {
			Location temp = loc.clone().add(rand.nextInt(10)-5,0,rand.nextInt(10)-5);
			if (temp.getBlock().isPassable())
				temp = Utils.getBlockBelow(temp).getLocation().clone().add(0.5,0.5,0.5);
			else
				temp = Utils.getBlockAbove(temp).getLocation().clone().add(0.5,0.5,0.5);
			Mob vex = (Mob) loc.getWorld().spawnEntity(temp, EntityType.VEX);
			plugin.handler.addEntity(new TamedLostSoul(vex, plugin, rand, entity));
			souls[i] = vex;
			temp.getWorld().spawnParticle(Particle.SQUID_INK, temp.clone().add(0,0.75,0), 20, .4, .4, .4, 0.0001);
			temp.getWorld().playSound(temp, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.PLAYERS, 1, 0.8f);
			temp.getWorld().playSound(temp, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.3f, 1.5f);
		}
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				for (LivingEntity soul : souls)
					if (soul != null)
						soul.remove();
			}
		}, ItemsHandler.soulRipperSoulLifeTicks);
	}
	private void yetiRoar(LivingEntity entity, int level, double[] data) {
		Location entityHit = entity.getLocation().clone();
		World world = entityHit.getWorld();
		BlockVector block = new BlockVector(entityHit.getX(), entityHit.getY(), entityHit.getZ());
		BlockData bd = Material.PACKED_ICE.createBlockData();
		if (plugin.mcVersion >= 1.16)
			world.playSound(entityHit, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, (1f/3f)*((float) level), 0.6f);
		world.playSound(entityHit, Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, (2f/3f)*((float) level), .5f);
		int[] time = {1};
		new RepeatingTask(plugin, 0, 5) {
		@Override
		public void run() {
			time[0]++;
				for (int x = -time[0]; x < time[0]; x++)
					for (int z = -time[0]; z < time[0]; z++) {
						Vector position = block.clone().add(new Vector(x, 0, z));
						Block b = world.getBlockAt(position.toLocation(world));
						if (block.distance(position) >= (time[0] - 1) && block.distance(position) <= time[0]) {
							if (b.isPassable()) {
								for (int i = 0; i < 3; i++) {
									b = b.getRelative(BlockFace.DOWN);
									if (!b.isPassable())
										break;
								}
								if (b.isPassable())
									continue;
								else
									b = b.getRelative(BlockFace.UP);
							} else {
								for (int i = 0; i < 3; i++) {
									b = b.getRelative(BlockFace.UP);
									if (b.isPassable())
										break;
								}
								if (!b.isPassable())
									continue;
							}
							world.spawnParticle(Particle.SNOW_SHOVEL, b.getLocation().clone().add(0.5, 0.5, 0.5), 20, .3, .5, .3, 0.001);
							world.spawnParticle(Particle.BLOCK_CRACK, b.getLocation().clone().add(0.5, 0.5, 0.5), 3, .3, .5, .3, 0.001, bd);
							if (b.getType() == Material.FIRE) {
								b.setType(Material.AIR);
								world.playSound(b.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
								world.spawnParticle(Particle.SMOKE_LARGE, b.getLocation().clone().add(0.5, 0.2, 0.5), 5, .3, .5, .3, 0.001, bd);
							}
							if (plugin.mcVersion >= 1.17)
								world.playSound(b.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 0.5f, 0.5f);
							else
								world.playSound(b.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 0.5f, 1f);
							for (Entity e : world.getNearbyEntities(b.getLocation().clone().add(0.5, 0.5, 0.5), 0.5, 1, 0.5)) {
								if (e.equals(entity))
									continue;
								e.setVelocity(new Vector(e.getLocation().getX() - entityHit.getX(), 0, e.getLocation().getZ() - entityHit.getZ()).normalize().multiply(0.2).setY(0.8));
								if (e instanceof LivingEntity && !(e instanceof Player && Utils.isPlayerImmune((Player) e))) {
									((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 7, true, false));
									if (plugin.mcVersion >= 1.17)
										e.setFreezeTicks(500);
								}
							}
						}
					}
				if (time[0] >= data[1])
					cancel();
			}
		};
	}
	private void castWaveSpell(Location location, Vector dir, int distance, Random rand, LivingEntity shooter) {
		World world = location.getWorld();
		int[] timer = {distance};
		Vector angle = new Vector(dir.getZ(), 0, -dir.getX());
		Location spot = location.clone().add(dir.clone().multiply(2)).add(angle.clone().multiply(-2));
		Block[] water = new Block[15];
		Queue<Block> allWaters = new ArrayDeque<>();
		Queue<Block> puddles = new ArrayDeque<>();
		new RepeatingTask(plugin, 0, 2) {
			@Override
			public void run() {
				for (Block b : water)
					if (b != null && b.getType() == Material.WATER) {
						world.spawnParticle(Particle.FALLING_WATER, b.getLocation().add(.5,.5,.5), 30, .5, .5, .5, 0.0001);
						if (rand.nextInt(4) == 0 && b.getRelative(BlockFace.DOWN).getType().isSolid()) {
							puddles.add(b);
							allWaters.remove(b);
							Levelled data = ((Levelled) b.getBlockData());
							data.setLevel(7);
							b.setBlockData(data);
							Monsoon.globalPuddles.add(b);
						} else
							b.setType(Material.AIR);
					}
				if (timer[0] <= 0) {
					for (Block b : allWaters)
						if (b != null)
							b.setType(Material.AIR);
					if (timer[0] <= -10) {
						cancel();
						new RepeatingTask(plugin, 60, 1) {
							@Override
							public void run() {
								if (puddles.isEmpty())
									cancel();
								Block b = puddles.poll();
								if (b != null && b.getType() == Material.WATER)
									b.setType(Material.AIR);
								Monsoon.globalPuddles.remove(b);
							}
						};
					}
					timer[0]--;
					return;
				}
				timer[0]--;
				for (int cycle=0; cycle < 3; cycle++) {
					Location line = spot.clone().add(dir.clone().multiply(cycle)).add(0,cycle,0);
					for (int i=0; i < 5; i++) {
						Block b = line.clone().add(angle.clone().multiply(i)).getBlock();
						if (b.getType() != Material.AIR || Utils.isZoneProtected(b.getLocation()))
							continue;
						allWaters.add(b);
						world.spawnParticle(Particle.BUBBLE_POP, b.getLocation().add(.5,.5,.5), 10, .5, .5, .5, 0.0001);
						b.setType(Material.WATER);
						water[i+(cycle*5)] = b;
						for (Entity e : world.getNearbyEntities(b.getLocation(), 0.5, 0.5, 0.5)) {
							if (e.equals(shooter))
								continue;
							e.setVelocity(dir);
							if (e instanceof LivingEntity && !(e instanceof Player && Utils.isPlayerImmune((Player) e)))
								((LivingEntity) e).damage(4);
						}
					}
				}
				spot.add(dir);
				world.playSound(spot, Sound.WEATHER_RAIN, SoundCategory.HOSTILE, 1, 0.75f);
			}
		};
	}
}
