package com.github.jewishbanana.deadlydisasters.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import com.github.jewishbanana.deadlydisasters.disasters.Disaster;

/**
 * RegenerationDataUtil
 *
 * - Saves plugin regeneration data to an SQLite DB at: <plugin>/data/regenData.db
 * - Loads DB off-thread, hydrates Bukkit objects in small ticked batches on main thread,
 *   then atomically putAll(...) into BlockRegenHandler static maps and recreates Disasters.
 *
 * Note: tile extras are encoded/decoded reflectively and ignored if unsupported on the server version.
 */
public final class RegenerationDataUtil {
	private RegenerationDataUtil() {}

	private static final String JDBC_CLASS = "org.sqlite.JDBC";

	// adjustable batch sizes
	private static int DAMAGED_BATCH = 10000;
	private static int PLACED_BATCH = 10000;
	private static int MOVE_BATCH = 10000;
	private static int ORIGIN_BATCH = 10000;
	private static int PHYS_BATCH = 10000;
	private static final int DISASTER_BATCH = 50;
	
	public static void reload() {
		final int throttle = DataUtils.getMainConfigInt("regeneration.load_regen_data_throttle");
		DAMAGED_BATCH = throttle;
		PLACED_BATCH = throttle;
		MOVE_BATCH = throttle;
		ORIGIN_BATCH = throttle;
		PHYS_BATCH = throttle;
	}

	public static void saveAll(
			JavaPlugin plugin,
			Map<Block, BlockState> damagedBlocks,
			Map<Block, Material> placedBlocks,
			Map<Block, Disaster> damageTracker,
			Map<Block, Block> blockToBlock,
			Map<Block, Block> originBlocks,
			Map<Block, List<BlockState>> physicBlocks,
			List<Disaster> disasters
			) {
		Objects.requireNonNull(plugin, "plugin");
		Path dbPath = plugin.getDataFolder().toPath().resolve("data");
		try {
			Files.createDirectories(dbPath);
		} catch (IOException e) {
			Utils.sendConsoleMessage("&cERROR could not create data directory. Regeneration data cannot be save until this is fixed!\n&eLog: "+e.getMessage());
			return;
		}

		try (Connection conn = open(plugin)) {
			conn.setAutoCommit(false);
			ensureSchema(conn);

			// clear previous content (transactional)
			try (Statement st = conn.createStatement()) {
				st.executeUpdate("DELETE FROM damaged_blocks");
				st.executeUpdate("DELETE FROM placed_blocks");
				st.executeUpdate("DELETE FROM block_to_block");
				st.executeUpdate("DELETE FROM origin_blocks");
				st.executeUpdate("DELETE FROM physic_blocks");
				st.executeUpdate("DELETE FROM disaster_ordered_blocks");
				st.executeUpdate("DELETE FROM disasters");
				st.executeUpdate("DELETE FROM damage_tracker");
			}

			// 1) disasters -> get generated ids
			Map<Disaster, Integer> disasterIds = new IdentityHashMap<>();
			try (PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO disasters(type, level, world_uuid, x, y, z, created) VALUES(?,?,?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS)) {
				for (Disaster d : disasters) {
					Location loc = d.getLocation();
					if (loc == null || loc.getWorld() == null) continue;
					ps.setString(1, d.getClass().getName());
					ps.setInt(2, d.getLevel());
					ps.setString(3, loc.getWorld().getUID().toString());
					ps.setInt(4, loc.getBlockX());
					ps.setInt(5, loc.getBlockY());
					ps.setInt(6, loc.getBlockZ());
					ps.setLong(7, Instant.now().toEpochMilli());
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys()) {
						if (rs.next()) disasterIds.put(d, rs.getInt(1));
					}
				}
			}

			// 2) ordered blocks
			try (PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO disaster_ordered_blocks(disaster_id, seq, world_uuid, x, y, z) VALUES(?,?,?,?,?,?)")) {
				for (Disaster d : disasters) {
					Integer id = disasterIds.get(d);
					if (id == null) continue;
					int seq = 0;
					for (Block b : d.getModifiedBlocks()) {
						ps.setInt(1, id);
						ps.setInt(2, seq++);
						ps.setString(3, b.getWorld().getUID().toString());
						ps.setInt(4, b.getX());
						ps.setInt(5, b.getY());
						ps.setInt(6, b.getZ());
						ps.addBatch();
					}
				}
				ps.executeBatch();
			}

			// 3) damaged_blocks
			try (PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO damaged_blocks(world_uuid,x,y,z,blockdata,extras) VALUES(?,?,?,?,?,?)")) {
				for (Map.Entry<Block, BlockState> e : damagedBlocks.entrySet()) {
					Block b = e.getKey();
					BlockState s = e.getValue();
					ps.setString(1, b.getWorld().getUID().toString());
					ps.setInt(2, b.getX());
					ps.setInt(3, b.getY());
					ps.setInt(4, b.getZ());
					ps.setString(5, s.getBlockData().getAsString());
					ps.setBytes(6, TileExtrasCodec.encodeSafely(s));
					ps.addBatch();
				}
				ps.executeBatch();
			}

			// 4) placed blocks
			try (PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO placed_blocks(world_uuid,x,y,z,material) VALUES(?,?,?,?,?)")) {
				for (Map.Entry<Block, Material> e : placedBlocks.entrySet()) {
					Block b = e.getKey();
					Material s = e.getValue();
					ps.setString(1, b.getWorld().getUID().toString());
					ps.setInt(2, b.getX());
					ps.setInt(3, b.getY());
					ps.setInt(4, b.getZ());
					ps.setString(5, s.toString());
					ps.addBatch();
				}
				ps.executeBatch();
			}

			// 5) block_to_block
			try (PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO block_to_block(tw,tx,ty,tz,fw,fx,fy,fz) VALUES(?,?,?,?,?,?,?,?)")) {
				for (Map.Entry<Block, Block> e : blockToBlock.entrySet()) {
					Block to = e.getKey();
					Block from = e.getValue();
					ps.setString(1, to.getWorld().getUID().toString());
					ps.setInt(2, to.getX()); ps.setInt(3, to.getY()); ps.setInt(4, to.getZ());
					ps.setString(5, from.getWorld().getUID().toString());
					ps.setInt(6, from.getX()); ps.setInt(7, from.getY()); ps.setInt(8, from.getZ());
					ps.addBatch();
				}
				ps.executeBatch();
			}
			
			// 6) origin blocks
			try (PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO origin_blocks(tw,tx,ty,tz,fw,fx,fy,fz) VALUES(?,?,?,?,?,?,?,?)")) {
				for (Map.Entry<Block, Block> e : originBlocks.entrySet()) {
					Block to = e.getKey();
					Block from = e.getValue();
					ps.setString(1, to.getWorld().getUID().toString());
					ps.setInt(2, to.getX()); ps.setInt(3, to.getY()); ps.setInt(4, to.getZ());
					ps.setString(5, from.getWorld().getUID().toString());
					ps.setInt(6, from.getX()); ps.setInt(7, from.getY()); ps.setInt(8, from.getZ());
					ps.addBatch();
				}
				ps.executeBatch();
			}

			// 7) physic_blocks
			try (PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO physic_blocks(owner_w,owner_x,owner_y,owner_z, own_w, own_x, own_y, own_z, idx, blockdata, extras) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
				for (Map.Entry<Block, List<BlockState>> e : physicBlocks.entrySet()) {
					Block owner = e.getKey();
					int idx = 0;
					for (BlockState s : e.getValue()) {
						try {
							Block current = s.getBlock();
							ps.setString(1, owner.getWorld().getUID().toString());
							ps.setInt(2, owner.getX());
							ps.setInt(3, owner.getY());
							ps.setInt(4, owner.getZ());
							ps.setString(5, current.getWorld().getUID().toString());
							ps.setInt(6, current.getX());
							ps.setInt(7, current.getY());
							ps.setInt(8, current.getZ());
							ps.setInt(9, idx++);
							ps.setString(10, s.getBlockData().getAsString());
							ps.setBytes(11, TileExtrasCodec.encodeSafely(s));
							ps.addBatch();
						} catch (IllegalStateException exception) {
							Utils.sendExceptionLog(exception);
						}
					}
				}
				ps.executeBatch();
			}

			// 8) damage_tracker
			try (PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO damage_tracker(world_uuid,x,y,z,disaster_id) VALUES(?,?,?,?,?)")) {
				for (Map.Entry<Block, Disaster> e : damageTracker.entrySet()) {
					Block b = e.getKey();
					Disaster d = e.getValue();
					Integer id = disasterIds.get(d);
					if (id == null) continue;
					ps.setString(1, b.getWorld().getUID().toString());
					ps.setInt(2, b.getX());
					ps.setInt(3, b.getY());
					ps.setInt(4, b.getZ());
					ps.setInt(5, id);
					ps.addBatch();
				}
				ps.executeBatch();
			}

			conn.commit();
		} catch (Exception ex) {
			Utils.sendConsoleMessage("&cERROR failed to save regeneration data!\n&eLog: "+ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * Load all data asynchronously and hydrate on the main thread in small ticked batches.
	 * When hydration completes, atomically putAll(...) into BlockRegenHandler's static maps, recreate disasters,
	 * call regenerateBlocks() on each recreated disaster, and complete the returned future.
	 */
	public static CompletableFuture<Void> loadAllAsync(
			JavaPlugin plugin,
			Map<Block, BlockState> damagedBlocksTarget,
			Map<Block, Material> placedBlocksTarget,
			Map<Block, Disaster> damageTrackerTarget,
			Map<Block, Block> blockToBlockTarget,
			Map<Block, Block> originBlocksTarget,
			Map<Block, List<BlockState>> physicBlocksTarget
			) {
		Objects.requireNonNull(plugin, "plugin");
		CompletableFuture<Void> future = new CompletableFuture<>();

		// DB read off main thread
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Path dbFile = plugin.getDataFolder().toPath().resolve("data").resolve("regenData.db");
			if (!Files.exists(dbFile)) {
				// nothing to load
				Bukkit.getScheduler().runTask(plugin, () -> future.complete(null));
				return;
			}

			// temporary raw row containers populated from DB (thread-local)
			HashMap<TempDamaged, Void> rawDamaged = new HashMap<>();
			HashMap<TempPlaced, Void> rawPlaced = new HashMap<>();
			List<TempMove> rawMoves = new ArrayList<>();
			List<TempOrigin> rawOrigin = new ArrayList<>();
			HashMap<TempPhysOwner, List<TempPhys>> rawPhys = new HashMap<>();
			HashMap<Integer, TempDisaster> rawDisasters = new HashMap<>();
			List<TempOrdered> rawOrdered = new ArrayList<>();
			List<TempTrack> rawTracks = new ArrayList<>();

			try (Connection conn = open(plugin)) {
				ensureSchema(conn);

				try (PreparedStatement ps = conn.prepareStatement("SELECT world_uuid,x,y,z,blockdata,extras FROM damaged_blocks");
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						UUID w = UUID.fromString(rs.getString(1));
						int x = rs.getInt(2), y = rs.getInt(3), z = rs.getInt(4);
						String bd = rs.getString(5);
						byte[] extras = rs.getBytes(6);
						rawDamaged.put(new TempDamaged(w,x,y,z,bd,extras), null);
					}
				}
				
				try (PreparedStatement ps = conn.prepareStatement("SELECT world_uuid,x,y,z,material FROM placed_blocks");
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						UUID w = UUID.fromString(rs.getString(1));
						int x = rs.getInt(2), y = rs.getInt(3), z = rs.getInt(4);
						String material = rs.getString(5);
						rawPlaced.put(new TempPlaced(w,x,y,z,material), null);
					}
				}

				try (PreparedStatement ps = conn.prepareStatement("SELECT tw,tx,ty,tz,fw,fx,fy,fz FROM block_to_block");
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						UUID tw = UUID.fromString(rs.getString(1));
						int tx = rs.getInt(2), ty = rs.getInt(3), tz = rs.getInt(4);
						UUID fw = UUID.fromString(rs.getString(5));
						int fx = rs.getInt(6), fy = rs.getInt(7), fz = rs.getInt(8);
						rawMoves.add(new TempMove(tw,tx,ty,tz,fw,fx,fy,fz));
					}
				}
				
				try (PreparedStatement ps = conn.prepareStatement("SELECT tw,tx,ty,tz,fw,fx,fy,fz FROM origin_blocks");
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						UUID tw = UUID.fromString(rs.getString(1));
						int tx = rs.getInt(2), ty = rs.getInt(3), tz = rs.getInt(4);
						UUID fw = UUID.fromString(rs.getString(5));
						int fx = rs.getInt(6), fy = rs.getInt(7), fz = rs.getInt(8);
						rawOrigin.add(new TempOrigin(tw,tx,ty,tz,fw,fx,fy,fz));
					}
				}

				try (PreparedStatement ps = conn.prepareStatement(
						"SELECT owner_w,owner_x,owner_y,owner_z, own_w, own_x, own_y, own_z, idx, blockdata, extras FROM physic_blocks ORDER BY owner_w,owner_x,owner_y,owner_z, own_w, own_x, own_y, own_z, idx");
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						UUID ow = UUID.fromString(rs.getString(1));
						int ox = rs.getInt(2), oy = rs.getInt(3), oz = rs.getInt(4);
						UUID ownWorld = UUID.fromString(rs.getString(5));
						int ownX = rs.getInt(6), ownY = rs.getInt(7), ownZ = rs.getInt(8);
						int idx = rs.getInt(9);
						String bd = rs.getString(10);
						byte[] ex = rs.getBytes(11);
						TempPhysOwner key = new TempPhysOwner(ow,ox,oy,oz);
						rawPhys.computeIfAbsent(key, k -> new ArrayList<>()).add(new TempPhys(ownWorld, ownX, ownY, ownZ, idx, bd, ex));
					}
				}

				try (PreparedStatement ps = conn.prepareStatement("SELECT id,type,level,world_uuid,x,y,z FROM disasters ORDER BY id");
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						int id = rs.getInt(1);
						String type = rs.getString(2);
						int level = rs.getInt(3);
						UUID w = UUID.fromString(rs.getString(4));
						int x = rs.getInt(5), y = rs.getInt(6), z = rs.getInt(7);
						rawDisasters.put(id, new TempDisaster(id, type, level, w, x, y, z));
					}
				}

				try (PreparedStatement ps = conn.prepareStatement(
						"SELECT disaster_id, seq, world_uuid, x, y, z FROM disaster_ordered_blocks ORDER BY disaster_id, seq");
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						rawOrdered.add(new TempOrdered(rs.getInt(1), rs.getInt(2),
								UUID.fromString(rs.getString(3)), rs.getInt(4), rs.getInt(5), rs.getInt(6)));
					}
				}

				try (PreparedStatement ps = conn.prepareStatement("SELECT world_uuid,x,y,z,disaster_id FROM damage_tracker");
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						rawTracks.add(new TempTrack(UUID.fromString(rs.getString(1)), rs.getInt(2),
								rs.getInt(3), rs.getInt(4), rs.getInt(5)));
					}
				}

			} catch (Exception ex) {
				Utils.sendConsoleMessage("&cERROR database failed to load. Saved regeneration data will not regenerate!\n&eLog: "+ex.getMessage());
				future.completeExceptionally(ex);
				return;
			}

			// Hydration on main thread in small batches
			new BukkitRunnable() {
				private final Iterator<TempDamaged> itDam = rawDamaged.keySet().iterator();
				private final Iterator<TempPlaced> itPlaced = rawPlaced.keySet().iterator();
				private final Iterator<TempMove> itMove = rawMoves.iterator();
				private final Iterator<TempOrigin> itOrigin = rawOrigin.iterator();
				private final Iterator<Map.Entry<TempPhysOwner, List<TempPhys>>> itPhys = rawPhys.entrySet().iterator();
				private final Iterator<Map.Entry<Integer, TempDisaster>> itDis = rawDisasters.entrySet().iterator();
				private final Iterator<TempOrdered> itOrd = rawOrdered.iterator();
				private final Iterator<TempTrack> itTrack = rawTracks.iterator();

				// temp in-memory structures
				private final HashMap<Block, BlockState> tmpDamaged = new HashMap<>();
				private final HashMap<Block, Material> tmpPlaced = new HashMap<>();
				private final HashMap<Block, Disaster> tmpDamageTracker = new HashMap<>();
				private final HashMap<Block, Block> tmpBlockToBlock = new HashMap<>();
				private final HashMap<Block, Block> tmpBlockOrigin = new HashMap<>();
				private final HashMap<Block, List<BlockState>> tmpPhysic = new HashMap<>();
				private final HashMap<Integer, Disaster> idToDisaster = new HashMap<>();
				private final ArrayList<Disaster> recreatedDisasters = new ArrayList<>();

				// stage sequencing
				private int stage = 0; // 0=damaged,1=moves,2=phys,3=disasters+ordered+track,4=handover

				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					try {
						if (stage == 0) {
							int i = 0;
							while (i++ < DAMAGED_BATCH && itDam.hasNext()) {
								TempDamaged td = itDam.next();
								World w = worldByUUID(td.world);
								if (w == null) continue;
								Block b = w.getBlockAt(td.x, td.y, td.z);
								BlockState proxy = TileExtrasCodec.proxyStateFor(b.getLocation(), td.blockData, td.extras);
								tmpDamaged.put(b, proxy);
							}
							if (!itDam.hasNext()) stage = 1;
							return;
						}

						if (stage == 1) {
							int i = 0;
							while (i++ < PLACED_BATCH && itPlaced.hasNext()) {
								TempPlaced tp = itPlaced.next();
								World w = worldByUUID(tp.world);
								if (w == null) continue;
								Block b = w.getBlockAt(tp.x, tp.y, tp.z);
								Material m = Material.valueOf(tp.material);
								tmpPlaced.put(b, m);
							}
							if (!itPlaced.hasNext()) stage = 2;
							return;
						}
						
						if (stage == 2) {
							int i = 0;
							while (i++ < MOVE_BATCH && itMove.hasNext()) {
								TempMove mv = itMove.next();
								World tw = worldByUUID(mv.toWorld);
								World fw = worldByUUID(mv.fromWorld);
								if (tw == null || fw == null) continue;
								Block to = tw.getBlockAt(mv.tx, mv.ty, mv.tz);
								Block from = fw.getBlockAt(mv.fx, mv.fy, mv.fz);
								tmpBlockToBlock.put(to, from);
							}
							if (!itMove.hasNext()) stage = 3;
							return;
						}
						
						if (stage == 3) {
							int i = 0;
							while (i++ < ORIGIN_BATCH && itOrigin.hasNext()) {
								TempOrigin mv = itOrigin.next();
								World tw = worldByUUID(mv.toWorld);
								World fw = worldByUUID(mv.fromWorld);
								if (tw == null || fw == null) continue;
								Block to = tw.getBlockAt(mv.tx, mv.ty, mv.tz);
								Block from = fw.getBlockAt(mv.fx, mv.fy, mv.fz);
								tmpBlockOrigin.put(to, from);
							}
							if (!itOrigin.hasNext()) stage = 4;
							return;
						}

						if (stage == 4) {
							int i = 0;
							while (i++ < PHYS_BATCH && itPhys.hasNext()) {
								Map.Entry<TempPhysOwner, List<TempPhys>> en = itPhys.next();
								TempPhysOwner owner = en.getKey();
								World w = worldByUUID(owner.world);
								if (w == null) continue;
								Block own = w.getBlockAt(owner.x, owner.y, owner.z);
								List<BlockState> set = tmpPhysic.computeIfAbsent(own, k -> new ArrayList<>());
								for (TempPhys tp : en.getValue()) {
									Block current = worldByUUID(tp.world).getBlockAt(tp.x, tp.y, tp.z);
									BlockState proxy = TileExtrasCodec.proxyStateFor(current.getLocation(), tp.blockData, tp.extras);
									set.add(proxy);
								}
							}
							if (!itPhys.hasNext()) stage = 5;
							return;
						}

						if (stage == 5) {
							// recreate disasters in small batches
							int i = 0;
							while (i++ < DISASTER_BATCH && itDis.hasNext()) {
								Map.Entry<Integer, TempDisaster> en = itDis.next();
								TempDisaster td = en.getValue();
								World w = worldByUUID(td.world);
								if (w == null) continue;
								Location loc = new Location(w, td.x + 0.5, td.y, td.z + 0.5);
								try {
									Class<? extends Disaster> cls = (Class<? extends Disaster>) Class.forName(td.type);
									Disaster d = cls.getConstructor(Location.class, org.bukkit.entity.Player.class, int.class)
											.newInstance(loc, null, td.level);
									idToDisaster.put(td.id, d);
									recreatedDisasters.add(d);
								} catch (Throwable t) {
									Utils.sendConsoleMessage("&cERROR could not initalize disaster &d'"+td.type+"' &cthis disaster will not be regenerated!\n&eLog: "+t.getMessage());
								}
							}
							if (!itDis.hasNext()) {
								// now ordered blocks
								while (itOrd.hasNext()) {
									TempOrdered ord = itOrd.next();
									Disaster d = idToDisaster.get(ord.disasterId);
									if (d == null) continue;
									World w = worldByUUID(ord.world);
									if (w == null) continue;
									Block b = w.getBlockAt(ord.x, ord.y, ord.z);
									d.getModifiedBlocks().add(b);
								}
								// now damage_tracker
								while (itTrack.hasNext()) {
									TempTrack tr = itTrack.next();
									World w = worldByUUID(tr.world);
									if (w == null) continue;
									Block b = w.getBlockAt(tr.x, tr.y, tr.z);
									Disaster d = idToDisaster.get(tr.disasterId);
									if (d != null) tmpDamageTracker.put(b, d);
								}
								stage = 6;
							}
							return;
						}

						if (stage == 6) {
							// final atomic handover on main thread
							try {
								damagedBlocksTarget.putAll(tmpDamaged);
								placedBlocksTarget.putAll(tmpPlaced);
								blockToBlockTarget.putAll(tmpBlockToBlock);
								originBlocksTarget.putAll(tmpBlockOrigin);
								physicBlocksTarget.putAll(tmpPhysic);
								damageTrackerTarget.putAll(tmpDamageTracker);

								// kick off regeneration for each recreated disaster
								for (Disaster d : recreatedDisasters) {
									try {
										Disaster.regeneratingDisasters.put(d, d.new RegeneratingTask(d, new ArrayList<>(d.getModifiedBlocks()), 0));
										d.getModifiedBlocks().clear();
									} catch (Throwable t) {
										Utils.sendConsoleMessage("&cERROR failed to restart regeneration of disaster &d'"+d.getDisplayName()+"'&c!\n&eLog: "+t.getMessage());
									}
								}
								plugin.getLogger().info("Loaded all regen data!");
							} catch (Throwable t) {
								Utils.sendConsoleMessage("&cERROR failed to finalize regeneration data!\n&eLog: "+t.getMessage());
								future.completeExceptionally(t);
								cancel();
								return;
							}
							future.complete(null);
							cancel();
						}
					} catch (Throwable t) {
						Utils.sendConsoleMessage("&cERROR failed hydration tick while loading regeneration data!\n&eLog: "+t.getMessage());
						t.printStackTrace();
						future.completeExceptionally(t);
						cancel();
					}
				}
			}.runTaskTimer(plugin, 1L, 1L);
		});

		return future;
	}

	// ---- Tile extras codec (reflective and safe) ----

	private static final class TileExtrasCodec {
		private TileExtrasCodec() {}

		static byte[] encodeSafely(BlockState state) {
			try {
				return encode(state);
			} catch (Throwable t) {
				return null;
			}
		}

		/** Encode tile extras reflectively; returns null if none or on error. */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		static byte[] encode(BlockState state) throws IOException {
			if (state == null || !(state instanceof TileState))
				return null;

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bos);

			out.writeInt(0); // placeholder for count
			int count = 0;

			// container inventories
			if (isInstance("org.bukkit.inventory.InventoryHolder", state)) {
				try {
					org.bukkit.inventory.InventoryHolder holder = (org.bukkit.inventory.InventoryHolder) state;
					org.bukkit.inventory.Inventory inv = holder.getInventory();
					if (inv != null) {
						byte[] data = serializeWithBukkitObject(inv.getContents());
						writeTag(out, "container", data);
						count++;
					}
				} catch (Throwable ignored) {}
			}

			// sign (modern and legacy)
			if (isInstance("org.bukkit.block.Sign", state)) {
				try {
					Class<?> signClass = Class.forName("org.bukkit.block.Sign");
					try {
						Method getSide = signClass.getMethod("getSide", Class.forName("org.bukkit.block.sign.Side"));
						Class<?> sideCls = Class.forName("org.bukkit.block.sign.Side");
						Class<?> signSideCls = Class.forName("org.bukkit.block.sign.SignSide");
						Object front = getSide.invoke(state, Enum.valueOf((Class<Enum>) sideCls.asSubclass(Enum.class), "FRONT"));
						Object back = getSide.invoke(state, Enum.valueOf((Class<Enum>) sideCls.asSubclass(Enum.class), "BACK"));
						try (ByteArrayOutputStream s = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(s)) {
							writeStringArray(ds, readSideLines(front, signSideCls));
							writeStringArray(ds, readSideLines(back, signSideCls));
							writeTag(out, "sign2", s.toByteArray());
							count++;
						}
					} catch (Throwable modern) {
						try {
							Method getLine = signClass.getMethod("getLine", int.class);
							String[] lines = new String[4];
							for (int i = 0; i < 4; i++) lines[i] = (String) getLine.invoke(state, i);
							try (ByteArrayOutputStream s = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(s)) {
								writeStringArray(ds, lines);
								writeTag(out, "sign", s.toByteArray());
								count++;
							}
						} catch (Throwable ignored) {}
					}
				} catch (Throwable ignored) {}
			}

			// skull owner
			if (isInstance("org.bukkit.block.Skull", state)) {
				try {
					Method getOwning = state.getClass().getMethod("getOwningPlayer");
					Object op = getOwning.invoke(state);
					if (op != null) {
						Method uid = op.getClass().getMethod("getUniqueId");
						UUID id = (UUID) uid.invoke(op);
						try (ByteArrayOutputStream s = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(s)) {
							ds.writeBoolean(true);
							ds.writeLong(id.getMostSignificantBits());
							ds.writeLong(id.getLeastSignificantBits());
							writeTag(out, "skull", s.toByteArray());
							count++;
						}
					}
				} catch (Throwable ignored) {}
			}

			// jukebox record
			if (isInstance("org.bukkit.block.Jukebox", state)) {
				try {
					Method m = state.getClass().getMethod("getRecord");
					Object rec = m.invoke(state);
					if (rec instanceof ItemStack is && is.getType() != Material.AIR) {
						byte[] b = serializeWithBukkitObject(is);
						writeTag(out, "jukebox", b);
						count++;
					}
				} catch (Throwable ignored) {}
			}

			// lectern page
			if (isInstance("org.bukkit.block.Lectern", state)) {
				try {
					Method m = state.getClass().getMethod("getPage");
					Object p = m.invoke(state);
					if (p instanceof Integer page) {
						try (ByteArrayOutputStream s = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(s)) {
							ds.writeInt(page);
							writeTag(out, "lectern", s.toByteArray());
							count++;
						}
					}
				} catch (Throwable ignored) {}
			}

			// banner (base and patterns)
			if (isInstance("org.bukkit.block.Banner", state)) {
				try {
					Method getBase = state.getClass().getMethod("getBaseColor");
					Object base = getBase.invoke(state);
					Method getPatterns = state.getClass().getMethod("getPatterns");
					Object patterns = getPatterns.invoke(state);
					byte[] b = serializeWithBukkitObject(new Object[]{base, patterns});
					writeTag(out, "banner", b);
					count++;
				} catch (Throwable ignored) {}
			}

			// furnace family
			if (isInstance("org.bukkit.block.Furnace", state)) {
				try {
					Method gb = state.getClass().getMethod("getBurnTime");
					Method gc = state.getClass().getMethod("getCookTime");
					Method gt = state.getClass().getMethod("getCookTimeTotal");
					int burn = (int) gb.invoke(state);
					int cook = (int) gc.invoke(state);
					int tot = (int) gt.invoke(state);
					try (ByteArrayOutputStream s = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(s)) {
						ds.writeShort(burn); ds.writeShort(cook); ds.writeShort(tot);
						writeTag(out, "furnace", s.toByteArray());
						count++;
					}
				} catch (Throwable ignored) {}
			}

			// brewing stand
			if (isInstance("org.bukkit.block.BrewingStand", state)) {
				try {
					Method gm = state.getClass().getMethod("getFuelLevel");
					int fuel = (int) gm.invoke(state);
					try (ByteArrayOutputStream s = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(s)) {
						ds.writeInt(fuel);
						writeTag(out, "brew", s.toByteArray());
						count++;
					}
				} catch (Throwable ignored) {}
			}

			// creature spawner
			if (isInstance("org.bukkit.block.CreatureSpawner", state)) {
				try {
					Method getType = state.getClass().getMethod("getSpawnedType");
					Object type = getType.invoke(state);
					Method getDelay = state.getClass().getMethod("getDelay");
					int delay = (int) getDelay.invoke(state);
					try (ByteArrayOutputStream s = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(s)) {
						oos.writeObject(type);
						oos.writeInt(delay);
						writeTag(out, "spawner", s.toByteArray());
						count++;
					}
				} catch (Throwable ignored) {}
			}

			// command block
			if (isInstance("org.bukkit.block.CommandBlock", state)) {
				try {
					Method gn = state.getClass().getMethod("getName");
					Method gc = state.getClass().getMethod("getCommand");
					String name = (String) gn.invoke(state);
					String cmd = (String) gc.invoke(state);
					try (ByteArrayOutputStream s = new ByteArrayOutputStream(); DataOutputStream ds = new DataOutputStream(s)) {
						writeUTF(ds, name);
						writeUTF(ds, cmd);
						writeTag(out, "cmd", s.toByteArray());
						count++;
					}
				} catch (Throwable ignored) {}
			}

			// end gateway
			if (isInstance("org.bukkit.block.EndGateway", state)) {
				try {
					Method me = state.getClass().getMethod("isExactTeleport");
					Method ma = state.getClass().getMethod("getAge");
					Method gx = state.getClass().getMethod("getExitLocation");
					boolean exact = (boolean) me.invoke(state);
					long age = (long) ma.invoke(state);
					Location exit = (Location) gx.invoke(state);
					try (ByteArrayOutputStream s = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(s)) {
						oos.writeBoolean(exact);
						oos.writeLong(age);
						oos.writeObject(exit);
						writeTag(out, "endgate", s.toByteArray());
						count++;
					}
				} catch (Throwable ignored) {}
			}

			// write tag count
			out.flush();
			byte[] arr = bos.toByteArray();
			arr[0] = (byte) ((count >>> 24) & 0xFF);
			arr[1] = (byte) ((count >>> 16) & 0xFF);
			arr[2] = (byte) ((count >>> 8) & 0xFF);
			arr[3] = (byte) (count & 0xFF);
			return arr;
		}
		
		private static final Map<BlockState, ItemStack[]> blockInventories = new HashMap<>();
		
		static BlockState proxyStateFor(Location loc, String blockDataString, byte[] extras) {
			Objects.requireNonNull(loc, "loc");
			BlockData data = Bukkit.createBlockData(blockDataString);

			return (BlockState) java.lang.reflect.Proxy.newProxyInstance(
					BlockState.class.getClassLoader(),
					new Class[]{BlockState.class},
					(proxy, method, args) -> {
						String name = method.getName();
						if ("update".equals(name)) {
							boolean force = args != null && args.length > 0 && args[0] instanceof Boolean && (Boolean) args[0];
							boolean physics = args != null && args.length > 1 && args[1] instanceof Boolean && (Boolean) args[1];
							Block b = loc.getBlock();
							b.setBlockData(data, physics);
							if (extras != null && extras.length > 0) {
								BlockState typed = b.getState();
								try { decodeAndApply(typed, extras); } catch (Throwable t) {
									Utils.sendConsoleMessage("&cERROR could not properly restore regeneration data on a tile state!\n&eLog: "+t.getMessage());
								}
								boolean val = typed.update(force, physics);
								ItemStack[] items = blockInventories.remove(typed);
								if (items != null)
									((org.bukkit.inventory.InventoryHolder) typed).getInventory().setContents(items);
								return val;
							} else {
								return true;
							}
						}
						BlockState snap = loc.getBlock().getState();
						snap.setBlockData(data);
						return method.invoke(snap, args);
					}
					);
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static void decodeAndApply(BlockState state, byte[] bytes) throws Exception {
			if (bytes == null || bytes.length == 0) return;
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
			int tags = in.readInt();
			for (int i = 0; i < tags; i++) {
				String tag = readUTF(in);
				int len = in.readInt();
				byte[] payload = len > 0 ? in.readNBytes(len) : new byte[0];
				switch (tag) {
				case "container":
					if (isInstance("org.bukkit.inventory.InventoryHolder", state)) {
						Object o = deserializeWithBukkitObject(payload);
						if (o instanceof ItemStack[] items) {
							org.bukkit.inventory.Inventory inv = ((org.bukkit.inventory.InventoryHolder) state).getInventory();
							if (inv != null)
								blockInventories.put(state, items);
						}
					}
					break;
				case "sign":
					if (isInstance("org.bukkit.block.Sign", state)) {
						try (DataInputStream s = new DataInputStream(new ByteArrayInputStream(payload))) {
							String[] lines = readStringArray(s);
							Method setLine = state.getClass().getMethod("setLine", int.class, String.class);
							for (int li = 0; li < Math.min(4, lines.length); li++) setLine.invoke(state, li, lines[li]);
						} catch (Throwable ignored) {}
					}
					break;
				case "sign2":
					if (isInstance("org.bukkit.block.Sign", state)) {
						try (DataInputStream s = new DataInputStream(new ByteArrayInputStream(payload))) {
							String[] front = readStringArray(s);
							String[] back = readStringArray(s);
							try {
								Class<?> sideCls = Class.forName("org.bukkit.block.sign.Side");
								Class<?> signSideCls = Class.forName("org.bukkit.block.sign.SignSide");
								Method getSide = state.getClass().getMethod("getSide", sideCls);
								Object FRONT = Enum.valueOf((Class<Enum>) sideCls.asSubclass(Enum.class), "FRONT");
								Object BACK = Enum.valueOf((Class<Enum>) sideCls.asSubclass(Enum.class), "BACK");
								Object fs = getSide.invoke(state, FRONT);
								Object bs = getSide.invoke(state, BACK);
								Method setLine = signSideCls.getMethod("setLine", int.class, String.class);
								for (int li = 0; li < Math.min(4, front.length); li++) setLine.invoke(fs, li, front[li]);
								for (int li = 0; li < Math.min(4, back.length); li++) setLine.invoke(bs, li, back[li]);
							} catch (Throwable ignored) {}
						} catch (Throwable ignored) {}
					}
					break;
				case "skull":
					if (isInstance("org.bukkit.block.Skull", state)) {
						try (DataInputStream s = new DataInputStream(new ByteArrayInputStream(payload))) {
							boolean has = s.readBoolean();
							if (has) {
								UUID id = new UUID(s.readLong(), s.readLong());
								OfflinePlayer op = Bukkit.getOfflinePlayer(id);
								try { state.getClass().getMethod("setOwningPlayer", OfflinePlayer.class).invoke(state, op); } catch (Throwable ignored) {}
							}
						} catch (Throwable ignored) {}
					}
					break;
				case "jukebox":
					if (isInstance("org.bukkit.block.Jukebox", state)) {
						Object o = deserializeWithBukkitObject(payload);
						if (o instanceof ItemStack disc) {
							try { state.getClass().getMethod("setRecord", ItemStack.class).invoke(state, disc); } catch (Throwable ignored) {}
						}
					}
					break;
				case "lectern":
					if (isInstance("org.bukkit.block.Lectern", state)) {
						try (DataInputStream s = new DataInputStream(new ByteArrayInputStream(payload))) {
							int page = s.readInt();
							try { state.getClass().getMethod("setPage", int.class).invoke(state, page); } catch (Throwable ignored) {}
						} catch (Throwable ignored) {}
					}
					break;
				case "banner":
					if (isInstance("org.bukkit.block.Banner", state)) {
						try {
							Object[] arr = (Object[]) deserializeWithBukkitObject(payload);
							Object base = arr[0];
							Object patterns = arr[1];
							try { state.getClass().getMethod("setBaseColor", base.getClass()).invoke(state, base); } catch (Throwable ignored) {}
							try { state.getClass().getMethod("setPatterns", java.util.List.class).invoke(state, patterns); } catch (Throwable ignored) {}
						} catch (Throwable ignored) {}
					}
					break;
				case "furnace":
					if (isInstance("org.bukkit.block.Furnace", state)) {
						try (DataInputStream s = new DataInputStream(new ByteArrayInputStream(payload))) {
							int burn = s.readUnsignedShort();
							int cook = s.readUnsignedShort();
							int tot = s.readUnsignedShort();
							try { state.getClass().getMethod("setBurnTime", int.class).invoke(state, burn); } catch (Throwable ignored) {}
							try { state.getClass().getMethod("setCookTime", int.class).invoke(state, cook); } catch (Throwable ignored) {}
							try { state.getClass().getMethod("setCookTimeTotal", int.class).invoke(state, tot); } catch (Throwable ignored) {}
						} catch (Throwable ignored) {}
					}
					break;
				case "brew":
					if (isInstance("org.bukkit.block.BrewingStand", state)) {
						try (DataInputStream s = new DataInputStream(new ByteArrayInputStream(payload))) {
							int fuel = s.readInt();
							try { state.getClass().getMethod("setFuelLevel", int.class).invoke(state, fuel); } catch (Throwable ignored) {}
						} catch (Throwable ignored) {}
					}
					break;
				case "spawner":
					if (isInstance("org.bukkit.block.CreatureSpawner", state)) {
						try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(payload))) {
							Object type = ois.readObject();
							int delay = ois.readInt();
							try { state.getClass().getMethod("setSpawnedType", type.getClass()).invoke(state, type); } catch (Throwable ignored) {}
							try { state.getClass().getMethod("setDelay", int.class).invoke(state, delay); } catch (Throwable ignored) {}
						} catch (Throwable ignored) {}
					}
					break;
				case "cmd":
					if (isInstance("org.bukkit.block.CommandBlock", state)) {
						try (DataInputStream s = new DataInputStream(new ByteArrayInputStream(payload))) {
							String name = readUTF(s);
							String cmd = readUTF(s);
							try { state.getClass().getMethod("setName", String.class).invoke(state, name); } catch (Throwable ignored) {}
							try { state.getClass().getMethod("setCommand", String.class).invoke(state, cmd); } catch (Throwable ignored) {}
						} catch (Throwable ignored) {}
					}
					break;
				case "endgate":
					if (isInstance("org.bukkit.block.EndGateway", state)) {
						try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(payload))) {
							boolean exact = ois.readBoolean();
							long age = ois.readLong();
							Location exit = (Location) ois.readObject();
							try { state.getClass().getMethod("setExactTeleport", boolean.class).invoke(state, exact); } catch (Throwable ignored) {}
							try { state.getClass().getMethod("setAge", long.class).invoke(state, age); } catch (Throwable ignored) {}
							try { state.getClass().getMethod("setExitLocation", Location.class).invoke(state, exit); } catch (Throwable ignored) {}
						} catch (Throwable ignored) {}
					}
					break;
				default:
					// unknown, skip
					break;
				}
			}
		}

		/* helper utilities */

		private static boolean isInstance(String className, Object o) {
			try {
				Class<?> c = Class.forName(className);
				return c.isInstance(o);
			} catch (Throwable t) { return false; }
		}

		private static void writeTag(DataOutputStream out, String tag, byte[] payload) throws IOException {
			writeUTF(out, tag);
			out.writeInt(payload == null ? 0 : payload.length);
			if (payload != null) out.write(payload);
		}

		private static byte[] serializeWithBukkitObject(Object obj) throws IOException {
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); BukkitObjectOutputStream boos = new BukkitObjectOutputStream(bos)) {
				boos.writeObject(obj);
				boos.flush();
				return bos.toByteArray();
			}
		}

		private static Object deserializeWithBukkitObject(byte[] bytes) throws IOException, ClassNotFoundException {
			try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); BukkitObjectInputStream bois = new BukkitObjectInputStream(bis)) {
				return bois.readObject();
			}
		}

		private static void writeStringArray(DataOutputStream out, String[] arr) throws IOException {
			out.writeInt(arr.length);
			for (String s : arr) writeUTF(out, s);
		}

		private static String[] readSideLines(Object sideState, Class<?> signSideCls) {
			try {
				Method getLines = signSideCls.getMethod("getLines");
				Object lines = getLines.invoke(sideState);
				if (lines instanceof String[] arr) return arr;
			} catch (Throwable ignored) {}
			try {
				Method getLine = sideState.getClass().getMethod("getLine", int.class);
				String[] lines = new String[4];
				for (int i = 0; i < 4; i++) lines[i] = (String) getLine.invoke(sideState, i);
				return lines;
			} catch (Throwable ignored) {}
			return new String[0];
		}

		private static void writeUTF(DataOutput out, String s) throws IOException {
			if (s == null) out.writeBoolean(false);
			else { out.writeBoolean(true); out.writeUTF(s); }
		}

		private static String readUTF(DataInput in) throws IOException {
			return in.readBoolean() ? in.readUTF() : null;
		}

		private static String[] readStringArray(DataInputStream in) throws IOException {
			int n = in.readInt();
			String[] a = new String[n];
			for (int i = 0; i < n; i++) a[i] = readUTF(in);
			return a;
		}
	}

	// ---- SQLite utilities and schema ----

	private static Connection open(JavaPlugin plugin) throws Exception {
		Files.createDirectories(plugin.getDataFolder().toPath().resolve("data"));
		Class.forName(JDBC_CLASS);
		String url = "jdbc:sqlite:" + plugin.getDataFolder().toPath().resolve("data").resolve("regenData.db").toAbsolutePath().toString();
		return DriverManager.getConnection(url);
	}

	private static void ensureSchema(Connection c) throws SQLException {
		try (Statement st = c.createStatement()) {
			st.executeUpdate(
					"CREATE TABLE IF NOT EXISTS damaged_blocks(" +
							"world_uuid TEXT NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL," +
					"blockdata TEXT NOT NULL, extras BLOB, PRIMARY KEY(world_uuid,x,y,z))");
			st.executeUpdate(
					"CREATE TABLE IF NOT EXISTS placed_blocks(" +
							"world_uuid TEXT NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL," +
					"material TEXT NOT NULL, PRIMARY KEY(world_uuid,x,y,z))");
			st.executeUpdate(
					"CREATE TABLE IF NOT EXISTS block_to_block(" +
							"tw TEXT NOT NULL, tx INT NOT NULL, ty INT NOT NULL, tz INT NOT NULL," +
							"fw TEXT NOT NULL, fx INT NOT NULL, fy INT NOT NULL, fz INT NOT NULL," +
					"PRIMARY KEY(tw,tx,ty,tz))");
			st.executeUpdate(
					"CREATE TABLE IF NOT EXISTS origin_blocks(" +
							"tw TEXT NOT NULL, tx INT NOT NULL, ty INT NOT NULL, tz INT NOT NULL," +
							"fw TEXT NOT NULL, fx INT NOT NULL, fy INT NOT NULL, fz INT NOT NULL," +
					"PRIMARY KEY(tw,tx,ty,tz))");
			st.executeUpdate(
					"CREATE TABLE IF NOT EXISTS physic_blocks(" +
							"owner_w TEXT NOT NULL, owner_x INT NOT NULL, owner_y INT NOT NULL, owner_z INT NOT NULL," +
							"own_w TEXT NOT NULL, own_x INT NOT NULL, own_y INT NOT NULL, own_z INT NOT NULL," +
					"idx INT NOT NULL, blockdata TEXT NOT NULL, extras BLOB, PRIMARY KEY(owner_w,owner_x,owner_y,owner_z,own_w,own_x,own_y,own_z,idx))");
			st.executeUpdate(
					"CREATE TABLE IF NOT EXISTS disasters(" +
							"id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, level INT NOT NULL," +
					"world_uuid TEXT NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL, created BIGINT NOT NULL)");
			st.executeUpdate(
					"CREATE TABLE IF NOT EXISTS disaster_ordered_blocks(" +
							"disaster_id INT NOT NULL, seq INT NOT NULL, world_uuid TEXT NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL," +
					"PRIMARY KEY(disaster_id, seq))");
			st.executeUpdate(
					"CREATE TABLE IF NOT EXISTS damage_tracker(" +
							"world_uuid TEXT NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL, disaster_id INT NOT NULL," +
					"PRIMARY KEY(world_uuid,x,y,z))");
		}
	}

	// ---- temp DTOs (no records for better compatibility) ----

	private static final class TempDamaged {
		final UUID world; final int x, y, z;
		final String blockData; final byte[] extras;
		TempDamaged(UUID world, int x, int y, int z, String blockData, byte[] extras) {
			this.world = world; this.x = x; this.y = y; this.z = z; this.blockData = blockData; this.extras = extras;
		}
	}
	
	private static final class TempPlaced {
		final UUID world; final int x, y, z;
		final String material;
		TempPlaced(UUID world, int x, int y, int z, String material) {
			this.world = world; this.x = x; this.y = y; this.z = z; this.material = material;;
		}
	}

	private static final class TempMove {
		final UUID toWorld; final int tx, ty, tz;
		final UUID fromWorld; final int fx, fy, fz;
		TempMove(UUID toWorld, int tx, int ty, int tz, UUID fromWorld, int fx, int fy, int fz) {
			this.toWorld = toWorld; this.tx = tx; this.ty = ty; this.tz = tz;
			this.fromWorld = fromWorld; this.fx = fx; this.fy = fy; this.fz = fz;
		}
	}
	
	private static final class TempOrigin {
		final UUID toWorld; final int tx, ty, tz;
		final UUID fromWorld; final int fx, fy, fz;
		TempOrigin(UUID toWorld, int tx, int ty, int tz, UUID fromWorld, int fx, int fy, int fz) {
			this.toWorld = toWorld; this.tx = tx; this.ty = ty; this.tz = tz;
			this.fromWorld = fromWorld; this.fx = fx; this.fy = fy; this.fz = fz;
		}
	}

	private static final class TempPhysOwner {
		final UUID world; final int x, y, z;
		TempPhysOwner(UUID world, int x, int y, int z) { this.world = world; this.x = x; this.y = y; this.z = z; }
		@Override public int hashCode() { return Objects.hash(world, x, y, z); }
		@Override public boolean equals(Object o) {
			if (this == o) return true; if (!(o instanceof TempPhysOwner)) return false;
			TempPhysOwner t = (TempPhysOwner) o; return x==t.x && y==t.y && z==t.z && Objects.equals(world, t.world);
		}
	}

	private static final class TempPhys {
		final UUID world; final int x, y, z; final String blockData; final byte[] extras;
		TempPhys(UUID world,int x,int y,int z,int idx,String blockData,byte[] extras) { this.world=world;this.x=x;this.y=y;this.z=z;this.blockData=blockData;this.extras=extras; }
	}

	private static final class TempDisaster {
		final int id; final String type; final int level; final UUID world; final int x, y, z;
		TempDisaster(int id,String type,int level,UUID world,int x,int y,int z){this.id=id;this.type=type;this.level=level;this.world=world;this.x=x;this.y=y;this.z=z;}
	}

	private static final class TempOrdered {
		final int disasterId; final UUID world; final int x, y, z;
		TempOrdered(int disasterId,int seq,UUID world,int x,int y,int z){this.disasterId=disasterId;this.world=world;this.x=x;this.y=y;this.z=z;}
	}

	private static final class TempTrack {
		final UUID world; final int x, y, z, disasterId;
		TempTrack(UUID world,int x,int y,int z,int disasterId){this.world=world;this.x=x;this.y=y;this.z=z;this.disasterId=disasterId;}
	}

	private static World worldByUUID(UUID id) {
		for (World w : Bukkit.getWorlds()) if (w.getUID().equals(id)) return w;
		return null;
	}
}
