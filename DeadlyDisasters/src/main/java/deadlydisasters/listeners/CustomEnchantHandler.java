package deadlydisasters.listeners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.events.DisasterEvent;
import deadlydisasters.entities.soulstormentities.TamedLostSoul;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Main;
import deadlydisasters.utils.RepeatingTask;
import deadlydisasters.utils.Utils;

public class CustomEnchantHandler implements Listener {
	
	private Main plugin;
	private Random rand;
	
	private Map<UUID,Integer> ancientBladeCooldownMap = new HashMap<UUID,Integer>();
	private Map<UUID,Integer> soulRipperCooldownMap = new HashMap<UUID,Integer>();
	
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
			if (!soulRipperCooldownMap.containsKey(damager.getUniqueId()) && item.getItemMeta().hasLore() && item.getItemMeta().getLore().get(0).equals(ItemsHandler.soulRipperLore)) {
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
			int[] spellLife = {20};
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
					tempW.spawnParticle(Particle.FLAME, spell, 10, 1, 1, 1, .05);
					tempW.spawnParticle(Particle.BLOCK_DUST, spell, 10, 1, 1, 1, .1, bd);
					for (Entity e : spell.getWorld().getNearbyEntities(spell, 1.5, 1.5, 1.5))
						if (e instanceof LivingEntity) {
							e.setFireTicks(80);
							e.setVelocity(motion.clone().multiply(0.5));
						}
				}
			};
		}
	}
	public void reload() {
		
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
}
