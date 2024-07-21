package com.github.jewishbanana.deadlydisasters.entities.easterentities;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.handlers.ItemsHandler;
import com.github.jewishbanana.deadlydisasters.handlers.Languages;
import com.github.jewishbanana.deadlydisasters.handlers.specialevents.EasterEventHandler;
import com.github.jewishbanana.deadlydisasters.utils.Utils;

public class RampagingGoat extends CustomEntity {
	
	private Random rand;
	private int cooldown;
	
	public RampagingGoat(Goat entity, Main plugin) {
		super(entity, plugin);
		this.rand = plugin.random;
		this.entityType = CustomEntityType.RAMPAGINGGOAT;
		this.species = entityType.species;
		entity.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		
		entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(60.0);
		entity.setHealth(60.0);
		entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(20.0);
		entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
		entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
		
		entity.setMetadata("dd-rampaginggoat", plugin.fixedData);
		if (entity.getCustomName() == null)
			entity.setCustomName(Languages.langFile.getString("easter.rampagingGoat"));
		entity.setRemoveWhenFarAway(true);
	}
	@Override
	public void tick() {
		if (entity == null)
			return;
		for (int i=0; i < 8; i++) {
			DustTransition dust = new DustTransition(Color.fromRGB(rand.nextInt(125)+25, 255, rand.nextInt(55)+25), Color.fromRGB(25, rand.nextInt(155)+100, 255), rand.nextFloat());
			if (rand.nextInt(2) == 0)
				dust = new DustTransition(Color.fromRGB(rand.nextInt(105)+150, 25, 255), Color.fromRGB(25, rand.nextInt(155)+100, 255), rand.nextFloat()/2f);
			entity.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, entity.getLocation().add(rand.nextDouble()*1.5-.75,0.7+(rand.nextDouble()*1.5-.75),rand.nextDouble()*1.5-.75), 1, 0, 0, 0, 0.001, dust);
		}
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		entity = (Mob) plugin.getServer().getEntity(entityUUID);
		if (entity == null || entity.isDead()) {
			if (entity != null && entity.getKiller() != null && plugin.eventHandler.isEnabled && plugin.eventHandler instanceof EasterEventHandler && !((EasterEventHandler) plugin.eventHandler).eggs[2].hasAchieved(entity.getKiller().getUniqueId())) {
				Item item = entity.getWorld().dropItemNaturally(entity.getLocation(), ItemsHandler.redEasterEgg);
				item.setInvulnerable(true);
				((EasterEventHandler) plugin.eventHandler).eggs[2].addProgress(entity.getKiller().getUniqueId(), 1);
			}
			it.remove();
			return;
		}
		if (cooldown-- > 0)
			return;
		Utils.mergeEntityData(entity, "{Brain:{memories:{\"minecraft:ram_cooldown_ticks\":{value:0},\"minecraft:long_jump_cooling_down\":{value:0}}}}");
		cooldown = 5;
	}
	@Override
	public void clean() {
	}
	@Override
	public void update(FileConfiguration file) {
	}
}
