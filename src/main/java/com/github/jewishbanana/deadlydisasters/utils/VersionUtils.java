package com.github.jewishbanana.deadlydisasters.utils;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("deprecation")
public class VersionUtils {
	
	private static final Integer[] serverVersion;
	
	private static final Enchantment sharpness;
	private static final Enchantment unbreaking;
	
	public static final boolean displaysAllowed;
	public static final boolean usingNewDamageEvent;
	public static final boolean is17OrHigher;
	
	private static final Particle block_dust;
	private static final Particle block_crack;
	private static final Particle redstone_dust;
	private static final Particle item_crack;
	private static final Particle enchant;
	private static final Particle normal_smoke;
	private static final Particle large_smoke;
	private static final Particle drip_water;
	private static final Particle water_bubble;
	private static final Particle water_splash;
	private static final Particle explosion_huge;
	private static final Particle explosion_large;
	private static final Particle snow_shovel;
	
	private static final PotionEffectType jump_boost;
	private static final PotionEffectType slowness;
	private static final PotionEffectType resistance;
	private static final PotionEffectType confusion;
	private static final PotionEffectType slow_dig;
	
	private static final ItemFlag hide_effects;
	
	private static final Material short_grass;
	
	private static final boolean legacyDragonParticles;
	private static final boolean legacyFlashParticles;
	
	private static final Attribute maxHealthAttribute;
	private static final Attribute attackDamageAttribute;
	private static final Attribute movementSpeedAttribute;
	private static final Attribute followRangeAttribute;
	private static final Attribute armorAttribute;
	private static final Attribute armorToughnessAttribute;
	
	static {
		serverVersion = Arrays.stream(Bukkit.getBukkitVersion().split("-")[0].split("\\."))
				.filter(e -> e.matches("\\d+"))
				.map(Integer::parseInt)
				.toArray(Integer[]::new);
		
		if (isMCVersionOrAbove("1.18")) {
			sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
			unbreaking = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
		} else {
			sharpness = Enchantment.getByKey(NamespacedKey.minecraft("sharpness"));
			unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
		}
		
		if (isMCVersionOrAbove("1.20.3")) {
			jump_boost = Registry.EFFECT.get(NamespacedKey.minecraft("jump_boost"));
			slowness = Registry.EFFECT.get(NamespacedKey.minecraft("slowness"));
			resistance = Registry.EFFECT.get(NamespacedKey.minecraft("resistance"));
			confusion = Registry.EFFECT.get(NamespacedKey.minecraft("nausea"));
			slow_dig = Registry.EFFECT.get(NamespacedKey.minecraft("mining_fatigue"));
		} else {
			jump_boost = PotionEffectType.getByName("jump");
			slowness = PotionEffectType.getByName("slow");
			resistance = PotionEffectType.getByName("damage_resistance");
			confusion = PotionEffectType.getByName("confusion");
			slow_dig = PotionEffectType.getByName("slow_digging");
		}
		
		if (isMCVersionOrAbove("1.20.4")) {
			usingNewDamageEvent = true;
			short_grass = Material.SHORT_GRASS;
		} else {
			usingNewDamageEvent = false;
			short_grass = Material.valueOf("GRASS");
		}
		displaysAllowed = isMCVersionOrAbove("1.19.4");
		
		if (isMCVersionOrAbove("1.20.5")) {
			block_dust = Particle.DUST_PILLAR;
			block_crack = Particle.BLOCK;
			redstone_dust = Particle.DUST;
			item_crack = Particle.ITEM;
			enchant = Particle.ENCHANT;
			normal_smoke = Particle.SMOKE;
			large_smoke = Particle.LARGE_SMOKE;
			drip_water = Particle.DRIPPING_WATER;
			water_bubble = Particle.BUBBLE;
			water_splash = Particle.SPLASH;
			explosion_huge = Particle.EXPLOSION_EMITTER;
			explosion_large = Particle.EXPLOSION;
			snow_shovel = Particle.ITEM_SNOWBALL;
			hide_effects = ItemFlag.HIDE_ADDITIONAL_TOOLTIP;
		} else {
			block_dust = Particle.valueOf("BLOCK_DUST");
			block_crack = Particle.valueOf("BLOCK_CRACK");
			redstone_dust = Particle.valueOf("REDSTONE");
			item_crack = Particle.valueOf("ITEM_CRACK");
			enchant = Particle.valueOf("ENCHANTMENT_TABLE");
			normal_smoke = Particle.valueOf("SMOKE_NORMAL");
			large_smoke = Particle.valueOf("SMOKE_LARGE");
			drip_water = Particle.valueOf("DRIP_WATER");
			water_bubble = Particle.valueOf("WATER_BUBBLE");
			water_splash = Particle.valueOf("WATER_SPLASH");
			explosion_huge = Particle.valueOf("EXPLOSION_HUGE");
			explosion_large = Particle.valueOf("EXPLOSION_LARGE");
			snow_shovel = Particle.valueOf("SNOW_SHOVEL");
			hide_effects = ItemFlag.valueOf("HIDE_POTION_EFFECTS");
		}
		
		is17OrHigher = isMCVersionOrAbove("1.17");
		
		legacyDragonParticles = !isMCVersionOrAbove("1.21.9");
		legacyFlashParticles = legacyDragonParticles;
		
		if (isMCVersionOrAbove("1.21.3")) {
			maxHealthAttribute = Attribute.MAX_HEALTH;
		    attackDamageAttribute = Attribute.ATTACK_DAMAGE;
		    movementSpeedAttribute = Attribute.MOVEMENT_SPEED;
		    followRangeAttribute = Attribute.FOLLOW_RANGE;
		    armorAttribute = Attribute.ARMOR;
		    armorToughnessAttribute = Attribute.ARMOR_TOUGHNESS;
		} else {
		    maxHealthAttribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.max_health"));
		    attackDamageAttribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.attack_damage"));
		    movementSpeedAttribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.movement_speed"));
		    followRangeAttribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.follow_range"));
		    armorAttribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.armor"));
		    armorToughnessAttribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.armor_toughness"));
		}
	}
	public static boolean isMCVersionOrAbove(String version) {
		try {
			String[] test = version.split("\\.");
			for (int i = 0; i < test.length; i++) {
	            if (i >= serverVersion.length)
	                return false;
	            int currentSegment = serverVersion[i];
	            int testSegment = Integer.parseInt(test[i]);
	            if (currentSegment > testSegment)
	                return true;
	            else if (currentSegment < testSegment)
	                return false;
	        }
	        return true;
		} catch (NumberFormatException ex) {
			throw new NumberFormatException("The version string you supplied '"+version+"' is not a valid version string! Format must be as follows: '1.2.3' or '1.2' or '1'!");
		}
	}
	public static void spawnDragonBreathParticle(Location location, int count, double offX, double offY, double offZ, double speed, float data) {
		location.getWorld().spawnParticle(Particle.DRAGON_BREATH, location, count, offX, offY, offZ, speed, legacyDragonParticles ? null : data);
	}
	public static void spawnDragonBreathParticle(Player player, Location location, int count, double offX, double offY, double offZ, double speed, float data) {
		player.spawnParticle(Particle.DRAGON_BREATH, location, count, offX, offY, offZ, speed, legacyDragonParticles ? null : data);
	}
	public static void spawnDragonBreathParticle(Player player, double x, double y, double z, int count, double offX, double offY, double offZ, double speed, float data) {
		player.spawnParticle(Particle.DRAGON_BREATH, x, y, z, count, offX, offY, offZ, speed, legacyDragonParticles ? null : data);
	}
	public static void spawnFlashParticle(Location location, int count, double offX, double offY, double offZ, double speed, Color data) {
		location.getWorld().spawnParticle(Particle.FLASH, location, count, offX, offY, offZ, speed, legacyFlashParticles ? null : data);
	}
	public static void spawnFlashParticle(Player player, Location location, int count, double offX, double offY, double offZ, double speed, Color data) {
		player.spawnParticle(Particle.FLASH, location, count, offX, offY, offZ, speed, legacyFlashParticles ? null : data);
	}
	
	public static Enchantment getSharpness() {
		return sharpness;
	}
	public static Enchantment getUnbreaking() {
		return unbreaking;
	}
	public static Particle getBlockDust() {
		return block_dust;
	}
	public static Particle getBlockCrack() {
		return block_crack;
	}
	public static Particle getRedstoneDust() {
		return redstone_dust;
	}
	public static Particle getItemCrack() {
		return item_crack;
	}
	public static Particle getEnchantParticle() {
		return enchant;
	}
	public static Particle getNormalSmoke() {
		return normal_smoke;
	}
	public static Particle getLargeSmoke() {
		return large_smoke;
	}
	public static Particle getDripWater() {
		return drip_water;
	}
	public static Particle getWaterBubble() {
		return water_bubble;
	}
	public static Particle getWaterSplash() {
		return water_splash;
	}
	public static Particle getHugeExplosion() {
		return explosion_huge;
	}
	public static Particle getLargeExplosion() {
		return explosion_large;
	}
	public static Particle getSnowShovel() {
		return snow_shovel;
	}
	public static PotionEffectType getJumpBoost() {
		return jump_boost;
	}
	public static PotionEffectType getSlowness() {
		return slowness;
	}
	public static PotionEffectType getResistance() {
		return resistance;
	}
	public static PotionEffectType getConfusion() {
		return confusion;
	}
	public static PotionEffectType getSlowDigging() {
		return slow_dig;
	}
	public static ItemFlag getHideEffects() {
		return hide_effects;
	}
	public static Material getShortGrass() {
		return short_grass;
	}
	public static Attribute getMaxHealthAttribute() {
		return maxHealthAttribute;
	}
	public static Attribute getAttackDamageAttribute() {
		return attackDamageAttribute;
	}
	public static Attribute getMovementSpeedAttribute() {
		return movementSpeedAttribute;
	}
	public static Attribute getFollowRangeAttribute() {
		return followRangeAttribute;
	}
	public static Attribute getArmorAttribute() {
		return armorAttribute;
	}
	public static Attribute getArmorToughnessAttribute() {
		return armorToughnessAttribute;
	}
}
