package com.github.jewishbanana.deadlydisasters.entities;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Mob;
import org.bukkit.util.EulerAngle;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.utils.AnimationHandler;
import com.github.jewishbanana.deadlydisasters.utils.AnimationHandler.BodyPart;

public class AnimatedEntity extends CustomEntity {

	protected ArmorStand stand;
	protected Location stepLocation;
	protected AnimationHandler walkAnimation;
	protected AnimationHandler attackAnimation;
	protected float bodyRotation, rotationSpeed = 8.0f;
	protected boolean shouldWalk = true;
	
	public AnimatedEntity(Mob entity, Main plugin) {
		super(entity, plugin);
		this.stepLocation = entity.getLocation();
		
		this.attackAnimation = new AnimationHandler(true, false, false);
		this.attackAnimation.setAnimations(this.attackAnimation.new Animation(
					this.attackAnimation.new AnimationCheckpoint(BodyPart.RIGHT_ARM, 0, 0, 0, -112, 30, 0, 8, true, 0.5, 0.5),
					this.attackAnimation.new AnimationCheckpoint(BodyPart.LEFT_ARM, 0, 0, 0, -112, -30, 0, 8, true, 0.5, 0.5)
				)
			);
	}
	@Override
	public void tick() {
		Location entityLoc = entity.getLocation();
		float rot = entityLoc.getYaw() - bodyRotation;
		if (Math.abs(rot) > 300)
			bodyRotation = entityLoc.getYaw();
		else
			bodyRotation += (rot) / rotationSpeed;
		stand.setHeadPose(new EulerAngle(Math.toRadians(entity.getLocation().getPitch()), Math.toRadians(entityLoc.getYaw() - bodyRotation), 0));
		entityLoc.setYaw(bodyRotation);
		stand.teleport(entityLoc);
		
		if (shouldWalk && stepLocation.distanceSquared(entity.getLocation()) > 0.01 && entity.isOnGround()) {
			stepLocation = entity.getLocation();
			walkAnimation.go();
		} else
			walkAnimation.stop();
		walkAnimation.tick(stand);
		attackAnimation.tick(stand);
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
	}
	@Override
	public void clean() {
		if (stand != null)
			stand.remove();
	}
	@Override
	public void update(FileConfiguration file) {
	}
	public void attack() {
		if (stand == null)
			return;
		attackAnimation.startAnimation(stand);
	}
}
