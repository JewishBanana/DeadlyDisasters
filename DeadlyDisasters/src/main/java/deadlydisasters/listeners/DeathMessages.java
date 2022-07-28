package deadlydisasters.listeners;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;

import deadlydisasters.disasters.AcidStorm;
import deadlydisasters.disasters.Blizzard;
import deadlydisasters.disasters.CaveIn;
import deadlydisasters.disasters.Earthquake;
import deadlydisasters.disasters.EndStorm;
import deadlydisasters.disasters.ExtremeWinds;
import deadlydisasters.disasters.Geyser;
import deadlydisasters.disasters.Hurricane;
import deadlydisasters.disasters.MeteorShower;
import deadlydisasters.disasters.Purge;
import deadlydisasters.disasters.SandStorm;
import deadlydisasters.disasters.Sinkhole;
import deadlydisasters.disasters.SoulStorm;
import deadlydisasters.disasters.Tsunami;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;

public class DeathMessages implements Listener {
	
	public static Set<Sinkhole> sinkholes = new HashSet<>();
	public static Set<Geyser> geysers = new HashSet<>();
	public static Set<CaveIn> caveins = new HashSet<>();
	public static Set<Earthquake> earthquakes = new HashSet<>();
	public static Set<AcidStorm> acidstorms = new HashSet<>();
	public static Set<ExtremeWinds> extremewinds = new HashSet<>();
	public static Set<Blizzard> blizzards = new HashSet<>();
	public static Set<SoulStorm> soulstorms = new HashSet<>();
	public static Set<SandStorm> sandstorms = new HashSet<>();
	public static Set<Tsunami> tsunamis = new HashSet<>();
	public static Set<MeteorShower> meteorshowers = new HashSet<>();
	public static Set<EndStorm> endstorms = new HashSet<>();
	public static Set<Hurricane> hurricanes = new HashSet<>();
	public static Set<Purge> purges = new HashSet<>();
	
	private Main plugin;
	
	public DeathMessages(Main plugin) {
		this.plugin = plugin;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		World world = p.getWorld();
		if (!world.getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES) || p.getLastDamageCause() == null)
			return;
		DamageCause cause = p.getLastDamageCause().getCause();
		if (cause == null || e.getDeathMessage() == null)
			return;
		if (p.hasMetadata("dd-plague")) {
			p.removeMetadata("dd-plague", plugin);
			if (p.hasMetadata("dd-plaguedeath")) {
				e.setDeathMessage(e.getEntity().getName()+" "+Languages.langFile.getString("deaths.plague"));
				p.removeMetadata("dd-plaguedeath", plugin);
				return;
			}
			if (cause == DamageCause.WITHER) {
				e.setDeathMessage(e.getEntity().getName()+" "+Languages.langFile.getString("deaths.plague"));
				return;
			}
		}
		if (p.hasMetadata("dd-lostsouldeath")) {
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.soulDeath"));
			p.removeMetadata("dd-lostsouldeath", plugin);
			LivingEntity villager = (LivingEntity) p.getWorld().spawnEntity(p.getLocation(), EntityType.VILLAGER);
			villager.setCustomName(p.getName()+"'"+Languages.langFile.getString("deaths.corpse"));
			return;
		}
		if (p.hasMetadata("dd-unstablerift")) {
			p.removeMetadata("dd-unstablerift", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.rift"));
			return;
		}
		if (p.hasMetadata("dd-supernova")) {
			p.removeMetadata("dd-supernova", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.supernova"));
			return;
		}
		if (p.hasMetadata("dd-meteorcrush")) {
			p.removeMetadata("dd-meteorcrush", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.meteor"));
			return;
		}
		if (p.hasMetadata("dd-caveincrush")) {
			p.removeMetadata("dd-caveincrush", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.cavein"));
			return;
		}
		if (p.hasMetadata("dd-endwormfangs")) {
			p.removeMetadata("dd-endwormfangs", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.worm"));
			return;
		}
		if (p.hasMetadata("dd-purgedeath")) {
			p.removeMetadata("dd-purgedeath", plugin);
			if (Purge.targetedPlayers.contains(p))
				Purge.targetedPlayers.remove(p);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.purge"));
			return;
		}
		if (p.hasMetadata("dd-leechdeath")) {
			p.removeMetadata("dd-leechdeath", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.genericMob")+' '+Languages.langFile.getString("entities.shadowLeech"));
			return;
		}
		if (p.hasMetadata("dd-bloodleechdeath")) {
			p.removeMetadata("dd-bloodleechdeath", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.genericMob")+' '+Languages.langFile.getString("entities.bloodyLeech"));
			return;
		}
		if (cause == DamageCause.FALL || cause == DamageCause.LAVA) {
			for (Sinkhole obj : sinkholes) {
				int x = obj.getX(), y = obj.getY(), z = obj.getZ(), radius = (int) obj.getRadius();
				if (p.getLocation().getBlockX() >= x-radius && p.getLocation().getBlockX() <= x+radius && p.getLocation().getBlockZ() >= z-radius && p.getLocation().getBlockZ() <= z+radius && p.getLocation().getBlockY() <= y) {
					e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.sinkhole"));
					return;
				}
			}
			for (Earthquake obj : earthquakes) {
				int x = obj.getX(), y = obj.getY(), z = obj.getZ(), radius = obj.getRadius();
				Location loc = p.getLocation();
				if (loc.getBlockX() >= x-radius && loc.getBlockX() <= x+radius && loc.getBlockZ() >= z-radius && loc.getBlockZ() <= z+radius && loc.getBlockY() < y) {
					e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.earthquake"));
					return;
				}
			}
		}
		if (cause == DamageCause.CUSTOM || cause == DamageCause.LAVA) {
			for (Geyser obj : geysers) {
				int x = obj.getX(),z = obj.getZ(), y = obj.getY(), mem = obj.getMemory();
				if (p.getLocation().getBlockX() >= x-2 && p.getLocation().getBlockX() <= x+1 && p.getLocation().getBlockZ() >= z-2 && p.getLocation().getBlockZ() <= z+1 && p.getLocation().getBlockY() >= mem
						&& p.getLocation().getBlockY() <= y) {
					if (world.getEnvironment() == Environment.NORMAL) e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.waterGeyser"));
					else e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.lavaGeyser"));
					return;
				}
			}
		}
		if (cause == DamageCause.CUSTOM) {
			if (world.getEnvironment() == Environment.NORMAL && acidstorms.size() != 0 && p.getLocation().getBlock().getTemperature() > 0.15 && p.getLocation().getBlock().getTemperature() <= 0.95) {
				if (p.getWorld().getHighestBlockAt(p.getLocation()).getY() <= p.getLocation().getBlockY()+1) {
					for (AcidStorm storm : acidstorms) {
						if (e.getEntity().getWorld().equals(storm.getWorld())) {
							if (p.getGameMode() == GameMode.ADVENTURE || p.getGameMode() == GameMode.SURVIVAL) e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.acidstorm"));
							return;
						}
					}
				}
			}
			if (p.getWorld().getEnvironment() == Environment.NORMAL && !tsunamis.isEmpty() && p.getLocation().getBlockY() <= 75) {
				for (Tsunami tsu : tsunamis)
					if (p.getLocation().distance(tsu.getLocation()) <= tsu.getCurrent()+5) {
						if (p.getGameMode() == GameMode.ADVENTURE || p.getGameMode() == GameMode.SURVIVAL) e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.tsunami"));
						return;
					}
			}
		}
		if (cause == DamageCause.WITHER) {
			if (p.getWorld().getEnvironment() == Environment.NORMAL && blizzards.size() != 0 && p.getLocation().getBlock().getTemperature() < 0.15) {
				for (Blizzard storm : blizzards) {
					if (e.getEntity().getWorld().equals(storm.getWorld())) {
						e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.blizzard"));
						Location locS = new Location(p.getWorld(), p.getLocation().getBlockX()+0.5, p.getLocation().getBlockY(), p.getLocation().getBlockZ()+0.5, p.getLocation().getYaw(), p.getLocation().getPitch());
						Skeleton skel = (Skeleton) p.getWorld().spawnEntity(locS, EntityType.SKELETON);
						skel.setInvulnerable(true);
						skel.setSilent(true);
						skel.setAI(false);
						skel.getEquipment().setItemInMainHand(null);
						if (p.getLocation().getBlock().isPassable()) p.getLocation().getBlock().setType(Material.ICE);
						if (p.getLocation().add(0,1,0).getBlock().isPassable()) p.getLocation().add(0,1,0).getBlock().setType(Material.ICE);
						return;
					}
				}
			}
		}
		if (p.hasMetadata("dd-sandstormdeath") || cause == DamageCause.CUSTOM) {
			p.removeMetadata("dd-sandstormdeath", plugin);
			if (p.getWorld().getEnvironment() == Environment.NORMAL && sandstorms.size() != 0) {
				if (SandStorm.sandStormBiomes.contains(p.getLocation().getBlock().getBiome()))
					for (SandStorm storm : sandstorms)
						if (e.getEntity().getWorld().equals(storm.getWorld())) {
							e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.sandstorm"));
							return;
						}
			}
		}
	}
}
