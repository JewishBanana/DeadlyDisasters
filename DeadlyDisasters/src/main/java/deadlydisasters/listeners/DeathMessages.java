package deadlydisasters.listeners;

import java.util.HashSet;
import java.util.Set;

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
import deadlydisasters.disasters.Disaster;
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
import deadlydisasters.disasters.Supernova;
import deadlydisasters.disasters.Tornado;
import deadlydisasters.disasters.Tsunami;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Metrics;

public class DeathMessages implements Listener {
	
	public static Set<Sinkhole> sinkholes = new HashSet<>();
	public static Set<Geyser> geysers = new HashSet<>();
	public static Set<CaveIn> caveins = new HashSet<>();
	public static Set<Tornado> tornados = new HashSet<>();
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
	public static Set<Supernova> supernovas = new HashSet<>();
	
	private Main plugin;
	
	public DeathMessages(Main plugin) {
		this.plugin = plugin;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		World world = p.getWorld();
		if (Purge.targetedPlayers.contains(p.getUniqueId()))
			Purge.targetedPlayers.remove(p.getUniqueId());
		if (!world.getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES) || p.getLastDamageCause() == null)
			return;
		DamageCause cause = p.getLastDamageCause().getCause();
		if (cause == null || e.getDeathMessage() == null)
			return;
		if (cause != DamageCause.VOID && cause != DamageCause.SUICIDE) {
			for (SoulStorm obj : soulstorms)
				if (obj.getWorld().equals(world)) {
					Metrics.incrementValue(Metrics.disasterKillMap, Disaster.SOULSTORM.getMetricsLabel());
					break;
				}
			for (EndStorm obj : endstorms)
				if (obj.getWorld().equals(world)) {
					Metrics.incrementValue(Metrics.disasterKillMap, Disaster.ENDSTORM.getMetricsLabel());
					break;
				}
		}
		if (p.hasMetadata("dd-plague")) {
			p.removeMetadata("dd-plague", plugin);
			if (p.hasMetadata("dd-plaguedeath")) {
				e.setDeathMessage(e.getEntity().getName()+" "+Languages.langFile.getString("deaths.plague"));
				p.removeMetadata("dd-plaguedeath", plugin);
				Metrics.incrementValue(Metrics.disasterKillMap, Disaster.PLAGUE.getMetricsLabel());
				return;
			}
			if (cause == DamageCause.WITHER) {
				e.setDeathMessage(e.getEntity().getName()+" "+Languages.langFile.getString("deaths.plague"));
				Metrics.incrementValue(Metrics.disasterKillMap, Disaster.PLAGUE.getMetricsLabel());
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
			Metrics.incrementValue(Metrics.disasterKillMap, Disaster.SUPERNOVA.getMetricsLabel());
			return;
		}
		if (p.hasMetadata("dd-meteorcrush")) {
			p.removeMetadata("dd-meteorcrush", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.meteor"));
			Metrics.incrementValue(Metrics.disasterKillMap, Disaster.METEORSHOWERS.getMetricsLabel());
			return;
		}
		if (p.hasMetadata("dd-caveincrush")) {
			p.removeMetadata("dd-caveincrush", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.cavein"));
			Metrics.incrementValue(Metrics.disasterKillMap, Disaster.CAVEIN.getMetricsLabel());
			return;
		}
		if (p.hasMetadata("dd-endwormfangs")) {
			p.removeMetadata("dd-endwormfangs", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.worm"));
			return;
		}
		if (p.hasMetadata("dd-purgedeath")) {
			p.removeMetadata("dd-purgedeath", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.purge"));
			Metrics.incrementValue(Metrics.disasterKillMap, Disaster.PURGE.getMetricsLabel());
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
		if (p.hasMetadata("dd-sandstormdeath")) {
			p.removeMetadata("dd-sandstormdeath", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.sandstorm"));
			Metrics.incrementValue(Metrics.disasterKillMap, Disaster.SANDSTORM.getMetricsLabel());
			return;
		}
		if (p.hasMetadata("dd-acidstormdeath")) {
			p.removeMetadata("dd-acidstormdeath", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.acidstorm"));
			Metrics.incrementValue(Metrics.disasterKillMap, Disaster.ACIDSTORM.getMetricsLabel());
			return;
		}
		if (p.hasMetadata("dd-blizzarddeath")) {
			p.removeMetadata("dd-blizzarddeath", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.blizzard"));
			Metrics.incrementValue(Metrics.disasterKillMap, Disaster.BLIZZARD.getMetricsLabel());
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
		if (p.hasMetadata("dd-geyserdeath")) {
			p.removeMetadata("dd-geyserdeath", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.waterGeyser"));
			Metrics.incrementValue(Metrics.disasterKillMap, Disaster.GEYSER.getMetricsLabel());
			return;
		}
		if (p.hasMetadata("dd-tsunamideath")) {
			p.removeMetadata("dd-tsunamideath", plugin);
			e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.tsunami"));
			Metrics.incrementValue(Metrics.disasterKillMap, Disaster.TSUNAMI.getMetricsLabel());
			return;
		}
		if (cause == DamageCause.FALL || cause == DamageCause.LAVA) {
			for (Sinkhole obj : sinkholes) {
				int x = obj.getX(), y = obj.getY(), z = obj.getZ(), radius = (int) obj.getRadius();
				if (obj.getLocation().getWorld().equals(world) && p.getLocation().getBlockX() >= x-radius && p.getLocation().getBlockX() <= x+radius && p.getLocation().getBlockZ() >= z-radius && p.getLocation().getBlockZ() <= z+radius && p.getLocation().getBlockY() <= y) {
					e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.sinkhole"));
					Metrics.incrementValue(Metrics.disasterKillMap, Disaster.SINKHOLE.getMetricsLabel());
					return;
				}
			}
			for (Earthquake obj : earthquakes) {
				int x = obj.getX(), y = obj.getY(), z = obj.getZ(), radius = obj.getRadius();
				Location loc = p.getLocation();
				if (obj.getLocation().getWorld().equals(world) && loc.getBlockX() >= x-radius && loc.getBlockX() <= x+radius && loc.getBlockZ() >= z-radius && loc.getBlockZ() <= z+radius && loc.getBlockY() < y) {
					e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.earthquake"));
					Metrics.incrementValue(Metrics.disasterKillMap, Disaster.EARTHQUAKE.getMetricsLabel());
					return;
				}
			}
		}
		if (cause == DamageCause.FALL) {
			for (Tornado obj : tornados)
				if (obj.isEntityInvolved(p.getUniqueId())) {
					e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.tornado"));
					Metrics.incrementValue(Metrics.disasterKillMap, Disaster.TORNADO.getMetricsLabel());
					return;
				}
			for (ExtremeWinds obj : extremewinds)
				if (obj.getWorld().equals(world) && obj.isEntityInvolved(p.getUniqueId())) {
					e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.extremewinds"));
					Metrics.incrementValue(Metrics.disasterKillMap, Disaster.EXTREMEWINDS.getMetricsLabel());
					return;
				}
		}
		if (cause == DamageCause.LAVA && world.getEnvironment() == Environment.NETHER) {
			for (Geyser obj : geysers) {
				int x = obj.getX(),z = obj.getZ(), y = obj.getY(), mem = obj.getMemory();
				if (obj.getLocation().getWorld().equals(world) && p.getLocation().getBlockX() >= x-2 && p.getLocation().getBlockX() <= x+1 && p.getLocation().getBlockZ() >= z-2 && p.getLocation().getBlockZ() <= z+1 && p.getLocation().getBlockY() >= mem
						&& p.getLocation().getBlockY() <= y) {
					e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.lavaGeyser"));
					Metrics.incrementValue(Metrics.disasterKillMap, Disaster.GEYSER.getMetricsLabel());
					return;
				}
			}
		}
		if (cause == DamageCause.FALL || cause == DamageCause.LIGHTNING) {
			for (Hurricane obj : hurricanes)
				if (p.getLocation().distance(obj.getLocation()) <= obj.getSize()) {
					e.setDeathMessage(p.getName()+" "+Languages.langFile.getString("deaths.hurricane"));
					Metrics.incrementValue(Metrics.disasterKillMap, Disaster.HURRICANE.getMetricsLabel());
					return;
				}
		}
	}
}
