package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.github.jewishbanana.deadlydisasters.Main;

public class EntityUtils {
	
	private static final Main plugin;
	private static final FixedMetadataValue pluginMetadata;
	private static final boolean isVersion192OrAbove;
	static {
		plugin = Main.getInstance();
		pluginMetadata = plugin.getFixedMetadata();
		isVersion192OrAbove = VersionUtils.isMCVersionOrAbove("1.19.2");
	}
	
	@SuppressWarnings("removal")
	public static boolean pureDamageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause, Entity source, boolean ignoreTotem, boolean silent, Sound playerHurtSound) {
		if (entity == null || entity.isDead())
			return false;
		EntityDamageEvent event = source == null ? new EntityDamageEvent(entity, cause, damage) : new EntityDamageByEntityEvent(source, entity, cause, damage);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return false;
		double finalDamage = event.getFinalDamage();
		entity.setLastDamageCause(event);
		if (entity.getHealth()-finalDamage <= 0) {
			if (!ignoreTotem && (entity.getEquipment().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING || entity.getEquipment().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING)) {
				if (event instanceof EntityDamageByEntityEvent damageEntityEvent)
					entity.damage(1, damageEntityEvent.getDamager());
				else
					entity.damage(1);
				return true;
			}
			if (meta != null)
				entity.setMetadata(meta, pluginMetadata);
			entity.setHealth(0);
			playDamageEffect(entity);
			if (!silent && !entity.isSilent())
				playEntityHarmSound(entity, playerHurtSound);
			if (meta != null)
				entity.removeMetadata(meta, plugin);
			return true;
		}
		entity.setHealth(Math.min(Math.max(entity.getHealth()-finalDamage, 0), entity.getHealth()));
		playDamageEffect(entity);
		if (!silent && !entity.isSilent())
			playEntityHarmSound(entity, playerHurtSound);
		if (event instanceof EntityDamageByEntityEvent damageEntityEvent && entity instanceof Mob mob && damageEntityEvent.getDamager() instanceof LivingEntity livingDamager)
			mob.setTarget(livingDamager);
		return true;
	}
	public static boolean pureDamageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause, Entity source, boolean ignoreTotem, boolean silent) {
		return pureDamageEntity(entity, damage, meta, cause, source, ignoreTotem, silent, Sound.ENTITY_PLAYER_HURT);
	}
	public static boolean pureDamageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause, Entity source, boolean ignoreTotem) {
		return pureDamageEntity(entity, damage, meta, cause, source, ignoreTotem, false);
	}
	public static boolean pureDamageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause, Entity source) {
		return pureDamageEntity(entity, damage, meta, cause, source, false);
	}
	public static boolean pureDamageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause) {
		return pureDamageEntity(entity, damage, meta, cause, null);
	}
	public static void damageArmor(LivingEntity entity, double damage) {
		int dmg = Math.max((int) (damage + 4 / 4), 1);
		for (ItemStack armor : entity.getEquipment().getArmorContents()) {
			if (armor == null || armor.getItemMeta() == null)
				continue;
			ItemMeta meta = armor.getItemMeta();
			if (((Damageable) meta).getDamage() >= armor.getType().getMaxDurability()) armor.setAmount(0);
			else ((Damageable) meta).setDamage(((Damageable) meta).getDamage()+dmg);
			armor.setItemMeta(meta);
		}
	}
	public static boolean damageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause, Entity source, boolean ignoreTotem, boolean silent, Sound playerHurtSound) {
		final double armor = entity.getAttribute(VersionUtils.getArmorAttribute()).getValue();
		final double toughness = entity.getAttribute(VersionUtils.getArmorToughnessAttribute()).getValue();
		final double actualDamage = damage * (1 - Math.min(20, Math.max(armor / 5, armor - damage / (2 + toughness / 4))) / 25);
		if (pureDamageEntity(entity, actualDamage, meta, cause, source, ignoreTotem, silent, playerHurtSound)) {
			damageArmor(entity, actualDamage);
			return true;
		}
		return false;
	}
	public static boolean damageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause, Entity source, boolean ignoreTotem, boolean silent) {
		return damageEntity(entity, damage, meta, cause, source, ignoreTotem, silent, Sound.ENTITY_PLAYER_HURT);
	}
	public static boolean damageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause, Entity source, boolean ignoreTotem) {
		return damageEntity(entity, damage, meta, cause, source, ignoreTotem, false);
	}
	public static boolean damageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause, Entity source) {
		return damageEntity(entity, damage, meta, cause, source, false);
	}
	public static boolean damageEntity(LivingEntity entity, double damage, String meta, @NotNull DamageCause cause) {
		return damageEntity(entity, damage, meta, cause, null);
	}
	@SuppressWarnings("deprecation")
	public static void playDamageEffect(LivingEntity entity) {
		if (VersionUtils.usingNewDamageEvent)
			entity.playHurtAnimation(0);
		else
			entity.playEffect(EntityEffect.HURT);
	}
	public static void markFallingBlock(FallingBlock block) {
		block.setMetadata("dd-fb", pluginMetadata);
	}
	public static boolean rayTraceEntityConeForSolid(Entity entity, Location initial) {
		double height = entity.getHeight(), width = entity.getWidth();
		Location target = entity.getLocation().add(0,height/2.0,0);
		if (BlockUtils.rayTraceForSolid(initial, target))
			return true;
		if (BlockUtils.rayTraceForSolid(initial, target.add(0,height/2.0,0)))
			return true;
		if (BlockUtils.rayTraceForSolid(initial, target.clone().subtract(0,height/2.0,0)))
			return true;
		Vector angle = Utils.getVectorTowards(initial, target);
		try {
			angle.checkFinite();
		} catch (IllegalArgumentException err) {
			return false;
		}
		if (BlockUtils.rayTraceForSolid(initial, target.clone().add(new Vector(angle.getZ(), 0, -angle.getX()).normalize().multiply(width/2.0))))
			return true;
		if (BlockUtils.rayTraceForSolid(initial, target.clone().add(new Vector(-angle.getZ(), 0, angle.getX()).normalize().multiply(width/2.0))))
			return true;
		return false;
	}
	public static void makeEntityFaceLocation(Entity entity, Location to) {
		Vector dirBetweenLocations = to.toVector().subtract(entity.getLocation().toVector());
		entity.teleport(entity.getLocation().setDirection(dirBetweenLocations));
    }
	public static EquipmentSlot getEquipmentSlot(EntityEquipment inventory, ItemStack item) {
		for (EquipmentSlot slot : EquipmentSlot.values())
			if (inventory.getItem(slot).equals(item))
				return slot;
		return null;
	}
	public static boolean isPlayerImmune(Player player) {
		GameMode mode = player.getGameMode();
		return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
	}
	public static boolean isEntityImmunePlayer(Entity entity) {
		if (!(entity instanceof Player player))
			return false;
		GameMode mode = player.getGameMode();
		return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
	}
	public static boolean isEntityUnderHealth(LivingEntity entity, double value) {
		if (entity == null)
			return false;
		return entity.getHealth() < entity.getAttribute(VersionUtils.getMaxHealthAttribute()).getValue() * value;
	}
	public static enum EntitySound {
		HURT_SOUND,
		DEATH_SOUND;
	}
	public static void playEntitySound(LivingEntity entity, Location location, EntitySound sound, float volume, float pitch) {
		if (!isVersion192OrAbove)
			return;
		switch (sound) {
		case HURT_SOUND -> location.getWorld().playSound(location, entity.getHurtSound(), volume, pitch);
		case DEATH_SOUND -> location.getWorld().playSound(location, entity.getDeathSound(), volume, pitch);
		}
	}
	public static void playEntitySound(LivingEntity entity, EntitySound sound, float volume, float pitch) {
		playEntitySound(entity, entity.getLocation(), sound, volume, pitch);
	}
	public static void playEntityHarmSound(LivingEntity entity, Location location, float volume, float pitch, Sound playerHarmSound) {
		if (entity instanceof Player) {
			if (entity.isDead())
				location.getWorld().playSound(location, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, volume, pitch);
			else
				location.getWorld().playSound(location, playerHarmSound, SoundCategory.PLAYERS, volume, pitch);
		}
		if (!isVersion192OrAbove)
			return;
		if (entity.isDead())
			location.getWorld().playSound(location, entity.getDeathSound(), entity instanceof Monster ? SoundCategory.HOSTILE : SoundCategory.NEUTRAL, volume, pitch);
		else
			location.getWorld().playSound(location, entity.getHurtSound(), entity instanceof Monster ? SoundCategory.HOSTILE : SoundCategory.NEUTRAL, volume, pitch);
	}
	public static void playEntityHarmSound(LivingEntity entity, float volume, float pitch) {
		playEntityHarmSound(entity, entity.getLocation(), volume, pitch, Sound.ENTITY_PLAYER_HURT);
	}
	public static void playEntityHarmSound(LivingEntity entity, Location location, Sound playerHarmSound) {
		playEntityHarmSound(entity, location, 1, Utils.getRandomGenerator().nextFloat(0.8f, 1.2f), playerHarmSound);
	}
	public static void playEntityHarmSound(LivingEntity entity, Location location) {
		playEntityHarmSound(entity, location, 1, Utils.getRandomGenerator().nextFloat(0.8f, 1.2f), Sound.ENTITY_PLAYER_HURT);
	}
	public static void playEntityHarmSound(LivingEntity entity, Sound playerHarmSound) {
		playEntityHarmSound(entity, entity.getLocation(), 1, Utils.getRandomGenerator().nextFloat(0.8f, 1.2f), playerHarmSound);
	}
	public static void playEntityHarmSound(LivingEntity entity) {
		playEntityHarmSound(entity, entity.getLocation(), 1, Utils.getRandomGenerator().nextFloat(0.8f, 1.2f), Sound.ENTITY_PLAYER_HURT);
	}
}
