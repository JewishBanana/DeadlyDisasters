package com.github.jewishbanana.deadlydisasters.entities.infestedcavesentities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.github.jewishbanana.deadlydisasters.Main;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntity;
import com.github.jewishbanana.deadlydisasters.entities.CustomEntityType;
import com.github.jewishbanana.deadlydisasters.entities.CustomHead;
import com.github.jewishbanana.deadlydisasters.entities.EntityHandler;
import com.github.jewishbanana.deadlydisasters.events.disasters.InfestedCaves;
import com.github.jewishbanana.deadlydisasters.listeners.DeathMessages;
import com.github.jewishbanana.deadlydisasters.utils.Utils;
import com.github.jewishbanana.deadlydisasters.utils.VersionUtils;

public class InfestedWorm extends CustomEntity {
	
	private Location loc,staring,particleLoc,tpOffset;
	private Block block, blockBelow;
	private BlockFace face;
	private ArmorStand[] stands = new ArmorStand[5];
	private boolean isAnimation, swingDirection, noUpdate;
	public boolean shouldRemove;
	private Vector vec,peekVec,staringTowards;
	private float[] rotation = new float[3];
	private int peekingTicks = 80, frame, swingTimes;
	private Material blockMaterial,headMaterial;
	private Random rand;
	private BoundingBox box;
	private double dX = .2, dY = .2, dZ = .2, swing, lastSwing = 0.8, swingVel, playerLockOffset;
	private Player player;
	private ArmorStand entityStand;
	
	public static Set<Block> immuneBlocks = new HashSet<>();

	public InfestedWorm() {
	}
	public InfestedWorm(Block block, BlockFace direction, Main plugin, Random rand) {
		super(null, plugin);
		this.rand = rand;
		this.entityType = CustomEntityType.INFESTEDWORM;
		this.species = entityType.species;
		entityStand = spawnStand(block.getLocation().add(.5,.5,.5), new ItemStack(Material.AIR), 0);
		this.entityUUID = entityStand.getUniqueId();
		entityStand.setSmall(true);
		entityStand.getPersistentDataContainer().set(entityType.nameKey, PersistentDataType.BYTE, (byte) 0);
		entityStand.getPersistentDataContainer().set(CustomEntity.handler.globalKey, PersistentDataType.BYTE, (byte) 0);
		entityStand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		
		updateLocation(block, direction);
		Location tempLoc = loc.clone().add(vec.clone().multiply(4));
		box = new BoundingBox(tempLoc.getBlockX()-4, tempLoc.getBlockY()-4, tempLoc.getBlockZ()-4, tempLoc.getBlockX()+4, tempLoc.getBlockY()+4, tempLoc.getBlockZ()+4);
		if (plugin.mcVersion >= 1.19)
			headMaterial = Material.SCULK_SHRIEKER;
		else
			headMaterial = Material.STICKY_PISTON;
		blockMaterial = block.getType();
		
		stands[0] = spawnStand(loc.clone().add(100,100,0), new ItemStack(headMaterial), 0);
		stands[0].setHeadPose(new EulerAngle(rotation[1], 0, 0));
		stands[1] = spawnStand(loc.clone().add(100,100,0), new ItemStack(Material.AIR), 1);
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			if (stands[1] != null && !stands[1].isDead())
				stands[1].getEquipment().setHelmet(CustomHead.INFESTEDWORMBODY.getHead());
			}, 8);
		stands[1].setHeadPose(new EulerAngle(rotation[1], 0, 0));
		stands[1].setSmall(true);
		if (face == BlockFace.UP || face == BlockFace.DOWN)
			peekVec = new Vector(0,0,0);
		else
			peekVec = vec.clone().multiply(-0.1);
		staring = loc.clone().add(vec.clone().multiply(5));
		staringTowards = staring.toVector().subtract(loc.toVector());
		block.getWorld().playSound(block.getLocation().add(vec), Sound.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.HOSTILE, 1f, .75f);
	}
	@Override
	public void tick() {
		if (shouldRemove)
			return;
		if (player != null && player.isDead()) {
			for (Player p : Bukkit.getOnlinePlayers())
				p.showPlayer(plugin, player);
			if (blockBelow != null)
				player.sendBlockChange(blockBelow.getLocation(), blockBelow.getBlockData());
			player = null;
		}
		if (noUpdate) {
			particleLoc.getWorld().spawnParticle(VersionUtils.getBlockCrack(), particleLoc, 3, dX, dY, dZ, 1, blockMaterial.createBlockData());
			if (frame >= 0) {
				if (!swingDirection) {
					if (frame < 9) {
						if (frame % 3 == 0) {
							int standIndex = frame / 3;
							if (frame == 0) {
								stands[standIndex] = spawnStand(loc.clone().add(100,100,0), new ItemStack(headMaterial), standIndex);
								stands[standIndex].teleport(loc.clone().add(vec.clone().multiply(-0.5)).subtract(0,1.8,0).setDirection(staringTowards));
								if (plugin.mcVersion >= 1.19)
									 block.getWorld().playSound(stands[0].getLocation(), Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 0.2F, 2);
							} else {
								stands[standIndex] = spawnStand(loc.clone().add(100,100,0), CustomHead.INFESTEDWORMBODY.getHead(), standIndex);
								stands[standIndex].setSmall(true);
								switch (face) {
								 case UP:
									 stands[standIndex].teleport(tpOffset.clone().add(peekVec).add(vec.clone().multiply(-0.6)).setDirection(staringTowards));
									 break;
								 case DOWN:
									 stands[standIndex].teleport(tpOffset.clone().add(peekVec).add(vec.clone().multiply(-0.6)).setDirection(staringTowards));
									 break;
								 default:
									stands[standIndex].teleport(tpOffset.clone().add(peekVec).add(vec.clone().multiply(-0.45)).setDirection(staringTowards));
									break;
								 }
							}
							stands[standIndex].setHeadPose(new EulerAngle(rotation[1], 0, 0));
						}
						Vector charge = vec.clone().multiply(0.2);
						for (ArmorStand stand : stands)
							if (stand != null)
								stand.teleport(stand.getLocation().add(charge));
					} else if (frame >= 25) {
						frame = 7;
						swingDirection = true;
						player = null;
					}
					frame++;
				} else {
					if (frame % 3 == 0)
						stands[frame / 3].remove();
					frame--;
					Vector charge = vec.clone().multiply(-0.2);
					for (ArmorStand stand : stands)
						if (stand != null && !stand.isDead())
							stand.teleport(stand.getLocation().add(charge));
					if (frame < 0) {
						clean();
						shouldRemove = true;
						return;
					}
				}
			}
			if (player != null) {
				if (stands[0] == null || stands[0].isDead())
					player.teleport(block.getLocation().add(.5,-1,.5));
				else {
					Location tpTo = stands[0].getLocation();
					tpTo.setPitch(rotation[2]);
					tpTo.add(tpTo.getDirection().multiply(1.2));
					tpTo.setDirection(stands[0].getLocation().toVector().subtract(player.getLocation().toVector()));
					player.teleport(tpTo);
					if (blockBelow != null) {
						player.sendBlockChange(blockBelow.getLocation(), blockBelow.getBlockData());
						blockBelow = null;
					}
				}
			}
			return;
		}
		if (!isAnimation) {
			 if (peekingTicks > -100) {
				 peekingTicks--;
				 if (stands[0] != null && stands[1] != null && !stands[0].isDead() && !stands[1].isDead()) {
					 stands[0].teleport(loc.clone().add(peekVec).subtract(0,1.8,0).setDirection(staringTowards));
					 switch (face) {
					 case UP:
						 stands[1].teleport(loc.clone().add(peekVec).subtract(0,0.95,0).add(vec.clone().multiply(-0.6)));
						 break;
					 case DOWN:
						 stands[1].teleport(loc.clone().add(peekVec).subtract(0,1.2,0).add(vec.clone().multiply(-0.6)));
						 break;
					 default:
						stands[1].teleport(loc.clone().add(peekVec).subtract(0,1.1,0).add(vec.clone().multiply(-0.45)));
						break;
					 }
				 }
				 if (peekingTicks > 60) {
					 peekVec.add(vec.clone().multiply(0.055));
					 particleLoc.getWorld().spawnParticle(VersionUtils.getBlockCrack(), particleLoc, 3, dX, dY, dZ, 1, blockMaterial.createBlockData());
				 }
				 else if (peekingTicks > 0) {
					 if (stands[0] != null && !stands[0].isDead())
						 for (Entity e : loc.getWorld().getNearbyEntities(box, p -> (p instanceof Player))) {
							 staring = e.getLocation().add(0,0.5,0);
							 staringTowards = staring.toVector().subtract(stands[0].getLocation().toVector());
							 Location targetLoc = stands[0].getLocation().setDirection(staringTowards);
							 stands[0].teleport(stands[0].getLocation().add(staringTowards.clone().multiply(0.1)));
							 stands[0].setRotation(targetLoc.getYaw(), 0);
							 stands[0].setHeadPose(new EulerAngle(Math.toRadians(targetLoc.getPitch()+90), 0, 0));
							 break;
						 }
					 if (peekingTicks < 20) {
						 peekVec.add(vec.clone().multiply(-0.058));
						 particleLoc.getWorld().spawnParticle(VersionUtils.getBlockCrack(), particleLoc, 3, dX, dY, dZ, 1, blockMaterial.createBlockData());
					 } else if (peekingTicks == 20)
						 block.getWorld().playSound(block.getLocation().add(vec), Sound.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.HOSTILE, 1f, .75f);
				 } else if (peekingTicks == 0) {
					 if (stands[0] != null)
						 stands[0].remove();
					 if (stands[1] != null)
						 stands[1].remove();
				 }
			 }
			 if (block.getType() == blockMaterial && block.getRelative(face).getType() == Material.AIR)
				 for (int i=1; i < 5; i++)
					 if (block.getWorld().getNearbyEntities(loc.clone().add(vec.clone().multiply(i)), .5, .5, .5, p -> (p instanceof Player && !p.isDead()) && !Utils.isPlayerImmune((Player) p)).size() > 0) {
						 clean();
						 isAnimation = true;
						 if (face == BlockFace.UP || face == BlockFace.DOWN)
								peekVec = new Vector(0,0,0);
							else
								peekVec = vec.clone().multiply(-0.1);
						 staring = loc.clone().add(vec.clone().multiply(5));
						 staringTowards = staring.toVector().subtract(loc.toVector());
						 immuneBlocks.add(block);
						 if (plugin.mcVersion >= 1.19) {
							 block.getWorld().playSound(block.getLocation().add(vec), Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 0.2F, 2);
							 block.getWorld().playSound(block.getLocation().add(vec), Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1, 2);
						 }
						 return;
					 }
		} else {
			particleLoc.getWorld().spawnParticle(VersionUtils.getBlockCrack(), particleLoc, 3, dX, dY, dZ, 1, blockMaterial.createBlockData());
			if (frame < 15 && swingTimes == 0) {
				if (frame % 3 == 0) {
					int standIndex = frame / 3;
					if (frame == 0) {
						stands[standIndex] = spawnStand(loc.clone().add(100,100,0), new ItemStack(headMaterial), standIndex);
						stands[standIndex].teleport(loc.clone().add(vec.clone().multiply(-0.5)).subtract(0,1.8,0).setDirection(staringTowards));
					} else {
						stands[standIndex] = spawnStand(loc.clone().add(100,100,0), CustomHead.INFESTEDWORMBODY.getHead(), standIndex);
						stands[standIndex].setSmall(true);
						switch (face) {
						 case UP:
							 stands[standIndex].teleport(tpOffset.clone().add(peekVec).add(vec.clone().multiply(-0.6)).setDirection(staringTowards));
							 break;
						 case DOWN:
							 stands[standIndex].teleport(tpOffset.clone().add(peekVec).add(vec.clone().multiply(-0.6)).setDirection(staringTowards));
							 break;
						 default:
							stands[standIndex].teleport(tpOffset.clone().add(peekVec).add(vec.clone().multiply(-0.45)).setDirection(staringTowards));
							break;
						 }
					}
					stands[standIndex].setHeadPose(new EulerAngle(rotation[1], 0, 0));
				}
				Vector charge = vec.clone().multiply(0.15);
				for (ArmorStand stand : stands)
					if (stand != null)
						stand.teleport(stand.getLocation().add(charge));
				frame++;
			} else {
				if (swingTimes < 5) {
					if (!swingDirection) {
						swing += swingVel;
						if (swing > lastSwing) {
							swingDirection = true;
							swingTimes++;
							swingVel = -0.2;
							if (swingTimes == 3 && plugin.mcVersion >= 1.19)
								block.getWorld().playSound(block.getLocation().add(vec), Sound.ENTITY_WARDEN_DIG, SoundCategory.HOSTILE, 0.6f, 1.2f);
							if (swingTimes >= 5) {
								switch (face) {
								 case UP:
									 playerLockOffset = 1.5;
									 break;
								 case DOWN:
									 playerLockOffset = 1.8;
									 break;
								 default:
									 playerLockOffset = 1.5;
									break;
								 }
								if (plugin.mcVersion >= 1.19)
									block.getWorld().playSound(block.getLocation().add(vec), Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1, 2);
							} else if (player != null) {
								Utils.damageEntity(player, entityType.getDamage(), "dd-infestedwormdeath", false);
								if (plugin.mcVersion >= 1.19)
									block.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 0.5F, 1.2F);
							}
						}
					} else {
						swing -= swingVel;
						if (swing < -0.8) {
							swingDirection = false;
							swingTimes++;
							if (swingTimes >= 4) {
								lastSwing = 0;
								frame = 14;
								swingVel = -0.2;
							}
							if (player != null) {
								Utils.damageEntity(player, entityType.getDamage(), "dd-infestedwormdeath", false);
								if (plugin.mcVersion >= 1.19)
									block.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 0.5F, 1.2F);
							}
						}
					}
					if (swingVel < 0.25)
						swingVel += 0.05;
					Vector tempVec = vec.clone().multiply(0.4);
					if (face == BlockFace.UP || face == BlockFace.DOWN) {
						for (int i=1; i < stands.length; i++) {
							if (i < stands.length-1)
								stands[i].teleport(tpOffset.clone().add(tempVec.clone().multiply(stands.length-i-2)).add(vec.clone().rotateAroundX((swing/5)*(stands.length-i))).setDirection(stands[i].getLocation().toVector().subtract(stands[i+1].getLocation().toVector())));
							else
								stands[i].teleport(tpOffset.clone().add(tempVec.clone().multiply(stands.length-i-2)).add(vec.clone().rotateAroundX((swing/5)*(stands.length-i))).setDirection(stands[i].getLocation().toVector().subtract(loc.toVector())));
						}
						stands[0].teleport(loc.clone().subtract(0,1.8,0).add(tempVec.clone().multiply(stands.length-1.5)).add(vec.clone().rotateAroundX((swing/5)*(stands.length))).setDirection(stands[0].getLocation().toVector().subtract(loc.toVector())));
					} else {
						for (int i=1; i < stands.length; i++) {
							if (i < stands.length-1)
								stands[i].teleport(tpOffset.clone().add(tempVec.clone().multiply(stands.length-i-2)).add(vec.clone().rotateAroundY((swing/5)*(stands.length-i))).setDirection(stands[i].getLocation().toVector().subtract(stands[i+1].getLocation().toVector())));
							else
								stands[i].teleport(tpOffset.clone().add(tempVec.clone().multiply(stands.length-i-2)).add(vec.clone().rotateAroundY((swing/5)*(stands.length-i))).setDirection(stands[i].getLocation().toVector().subtract(loc.toVector())));
						}
						stands[0].teleport(loc.clone().subtract(0,1.8,0).add(tempVec.clone().multiply(stands.length-1.5)).add(vec.clone().rotateAroundY(swing)).setDirection(stands[0].getLocation().toVector().subtract(loc.toVector())));
					}
				} else {
					if (frame % 3 == 0)
						stands[frame / 3].remove();
					frame--;
					Vector charge = vec.clone().multiply(-0.2);
					for (ArmorStand stand : stands)
						if (stand != null && !stand.isDead())
							stand.teleport(stand.getLocation().add(charge));
					if (frame < 0) {
						clean();
						noUpdate = true;
						swingDirection = false;
						if (player != null)
							for (InfestedCaves cave : DeathMessages.infestedcaves)
								if (cave.warden != null && cave.getLocation().getWorld().equals(loc.getWorld()) && cave.getLocation().distanceSquared(loc) <= cave.getSizeSquared()) {
									Location tempWarden = cave.warden.getLocation().add(0,2,0);
									for (int x=-1; x <= 1; x++)
										for (int z=-1; z <= 1; z++) {
											Chunk chunk = tempWarden.getWorld().getChunkAt(tempWarden.getChunk().getX()+x, tempWarden.getChunk().getZ()+z);
											if (!chunk.isLoaded())
												chunk.load(true);
										}
									plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
										@Override
										public void run() {
											Location wardenLoc = cave.warden.getLocation().add(0,2,0);
											List<BlockFace> blockFaces = new ArrayList<>(Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST));
											for (int i=0; i < 50; i++) {
												Location tempLoc = wardenLoc.clone();
												Vector tempVec = new Vector((rand.nextDouble()*2)-1, (rand.nextDouble()*1.6)-0.8, (rand.nextDouble()*2)-1).normalize();
												for (int c=0; c < 11; c++) {
													tempLoc.add(tempVec);
													Block b = tempLoc.getBlock();
													if (!b.isPassable()) {
														if (c < 7 || b.getType() != cave.material)
															break;
														int faceOccupied = 0;
														for (BlockFace bface : blockFaces)
															if (!b.getRelative(bface).isPassable())
																faceOccupied++;
														if (faceOccupied < 3 || faceOccupied > 5)
															break;
														BlockFace faceSpawn = null;
														whileloop:
														for (int faceLoop=0; faceLoop < blockFaces.size(); faceLoop++) {
															BlockFace tempFace = blockFaces.get(rand.nextInt(blockFaces.size()));
															if (b.getRelative(tempFace).isPassable()) {
																for (int air=1; air < 4; air++)
																	if (!b.getRelative(tempFace, air).isPassable() || (air == 3 && tempFace != BlockFace.UP && tempFace != BlockFace.DOWN && !b.getRelative(tempFace, air).getRelative(BlockFace.DOWN).isPassable()))
																		continue whileloop;
																faceSpawn = tempFace;
																break;
															}
														}
														if (faceSpawn == null)
															break;
														updateLocation(b, faceSpawn);
														frame = 0;
														return;
													}
												}
											}
											shouldRemove = true;
										}
									}, 20L);
									for (Player p : loc.getWorld().getPlayers())
										if (!p.equals(player))
											p.hidePlayer(plugin, player);
									immuneBlocks.remove(block);
									frame = -1;
									if (block.getRelative(BlockFace.DOWN).isPassable()) {
										blockBelow = block.getRelative(BlockFace.DOWN);
										player.sendBlockChange(blockBelow.getLocation(), blockMaterial.createBlockData());
									}
									((org.bukkit.entity.Warden) cave.warden).increaseAnger(player, 50);
									return;
								}
						immuneBlocks.remove(block);
						shouldRemove = true;
					}
				}
			}
			if (stands[0] != null) {
				if (player == null)
					for (Entity e : block.getWorld().getNearbyEntities(stands[0].getEyeLocation(), .75, 1, .75, p -> (p instanceof Player && !p.isDead() && !Utils.isPlayerImmune((Player) p))))
						player = (Player) e;
				else {
					if (player.isSwimming())
						player.setSwimming(false);
					Location tpTo = stands[0].getLocation();
					tpTo.setPitch(rotation[2]);
					tpTo.add(tpTo.getDirection().multiply(playerLockOffset));
					tpTo.setDirection(stands[0].getLocation().toVector().subtract(player.getLocation().toVector()));
					player.teleport(tpTo);
				}
			}
		}
	}
	@Override
	public void function(Iterator<CustomEntity> it) {
		if (shouldRemove || !loc.getChunk().isLoaded()) {
			it.remove();
			clean();
			return;
		}
		refreshReferences(stands);
		if (!isAnimation) {
			if (block.getType() != blockMaterial || block.getRelative(face).getType() != Material.AIR) {
				it.remove();
				clean();
				return;
			}
			if (peekingTicks <= -100 && rand.nextInt(3) == 0) {
				peekingTicks = 80;
				stands[0] = spawnStand(loc.clone().add(100,100,0), new ItemStack(headMaterial), 0);
				stands[0].setHeadPose(new EulerAngle(rotation[1], 0, 0));
				stands[1] = spawnStand(loc.clone().add(100,100,0), new ItemStack(Material.AIR), 1);
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> stands[1].getEquipment().setHelmet(CustomHead.INFESTEDWORMBODY.getHead()), 8);
				stands[1].setHeadPose(new EulerAngle(rotation[1], 0, 0));
				stands[1].setSmall(true);
				if (face == BlockFace.UP || face == BlockFace.DOWN)
					peekVec = new Vector(0,0,0);
				else
					peekVec = vec.clone().multiply(-0.1);
				staring = loc.clone().add(vec.clone().multiply(5));
				staringTowards = staring.toVector().subtract(loc.toVector());
				block.getWorld().playSound(block.getLocation().add(vec), Sound.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.HOSTILE, 1f, .75f);
			}
		}
	}
	@Override
	public void clean() {
		if (entityStand != null)
			entityStand.remove();
		for (ArmorStand stand : stands)
			if (stand != null)
				stand.remove();
		if (player != null)
			for (Player p : Bukkit.getOnlinePlayers())
				p.showPlayer(plugin, player);
	}
	@Override
	public void update(FileConfiguration file) {
	}
	private ArmorStand spawnStand(Location location, ItemStack head, int index) {
		if (stands[index] != null)
			stands[index].remove();
		ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location.clone().add(100,100,0), EntityType.ARMOR_STAND);
		if (plugin.mcVersion >= 1.16)
			stand.setInvisible(true);
		else
			stand.setVisible(false);
		stand.setMarker(true);
		stand.setGravity(false);
		if (plugin.mcVersion >= 1.16) {
			stand.addEquipmentLock(EquipmentSlot.CHEST, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.FEET, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.HEAD, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.LEGS, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
			stand.addEquipmentLock(EquipmentSlot.OFF_HAND, org.bukkit.entity.ArmorStand.LockType.ADDING_OR_CHANGING);
		}
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> stand.getEquipment().setHelmet(head), 4);
		stand.teleport(location);
		stand.getPersistentDataContainer().set(EntityHandler.removalKey, PersistentDataType.BYTE, (byte) 0);
		return stand;
	}
	private void updateLocation(Block block, BlockFace direction) {
		this.block = block;
		this.face = direction;
		
		particleLoc = block.getRelative(face).getLocation();
		switch (direction) {
		default:
		case UP:
			vec = new Vector(0,1,0);
			rotation[0] = 0;
			rotation[1] = (float) Math.toRadians(0);
			rotation[2] = -90;
			this.loc = block.getLocation().add(.5,.7,.5);
			dY = .05;
			particleLoc.add(.5,0,.5);
			tpOffset = loc.clone().subtract(0,0.95,0);
			playerLockOffset = 1.0;
			peekVec = new Vector(0,0,0);
			break;
		case DOWN:
			vec = new Vector(0,-1,0);
			rotation[0] = 0;
			rotation[1] = (float) Math.toRadians(180);
			rotation[2] = 90;
			this.loc = block.getLocation().add(.5,1,.5);
			dY = .05;
			particleLoc.add(.5,1,.5);
			tpOffset = loc.clone().subtract(0,1.2,0);
			playerLockOffset = 1.5;
			peekVec = new Vector(0,0,0);
			break;
		case NORTH:
			vec = new Vector(0,0,-1);
			rotation[0] = 180;
			rotation[1] = (float) Math.toRadians(90);
			rotation[2] = 0;
			this.loc = block.getLocation().add(.5,.9,.5);
			dZ = .05;
			particleLoc.add(.5,.5,1);
			tpOffset = loc.clone().subtract(0,1.1,0);
			playerLockOffset = 1.15;
			peekVec = vec.clone().multiply(-0.1);
			break;
		case EAST:
			vec = new Vector(1,0,0);
			rotation[0] = -90;
			rotation[1] = (float) Math.toRadians(90);
			rotation[2] = 0;
			this.loc = block.getLocation().add(.5,.9,.5);
			dX = .05;
			particleLoc.add(0,.5,.5);
			tpOffset = loc.clone().subtract(0,1.1,0);
			playerLockOffset = 1.15;
			peekVec = vec.clone().multiply(-0.1);
			break;
		case SOUTH:
			vec = new Vector(0,0,1);
			rotation[0] = 0;
			rotation[1] = (float) Math.toRadians(90);
			rotation[2] = 0;
			this.loc = block.getLocation().add(.5,.9,.5);
			dZ = .05;
			particleLoc.add(.5,.5,0);
			tpOffset = loc.clone().subtract(0,1.1,0);
			playerLockOffset = 1.15;
			peekVec = vec.clone().multiply(-0.1);
			break;
		case WEST:
			vec = new Vector(-1,0,0);
			rotation[0] = 90;
			rotation[1] = (float) Math.toRadians(90);
			rotation[2] = 0;
			this.loc = block.getLocation().add(.5,.9,.5);
			dX = .05;
			particleLoc.add(1,.5,.5);
			tpOffset = loc.clone().subtract(0,1.1,0);
			playerLockOffset = 1.15;
			peekVec = vec.clone().multiply(-0.1);
			break;
		}
		loc.setYaw(rotation[0]);
	}
}
