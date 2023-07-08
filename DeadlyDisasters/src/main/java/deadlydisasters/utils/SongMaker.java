package deadlydisasters.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.mojang.datafixers.util.Pair;

import deadlydisasters.general.Main;

public class SongMaker {
	
	private Queue<Player> players = new ArrayDeque<>();
	private boolean loop;
	private List<Pair<List<Pair<Sound, Float>>,Integer>> notes = new ArrayList<>();
	private RepeatingTask task;
	private int note;
	private float volume = 1f;
	
	public SongMaker(Song song) {
		notes = song.notes;
	}
	public void playSong() {
		if (task == null) {
			int[] delay = {notes.get(0).getSecond()};
			task = new RepeatingTask(Main.getInstance(), 0, 1) {
				@Override
				public void run() {
					if (delay[0]-- <= 0) {
						List<Pair<Sound, Float>> sounds = notes.get(note).getFirst();
						for (Player p : players)
							for (Pair<Sound, Float> sound : sounds)
								p.playSound(p, sound.getFirst(), volume, sound.getSecond());
						note++;
						if (note < notes.size()) {
							delay[0] = notes.get(note).getSecond();
							return;
						}
						cancel();
						task = null;
						if (loop) {
							note = 0;
							playSong();
						}
						return;
					}
				}
			};
		}
	}
	public void stopSong() {
		if (task != null)
			task.cancel();
	}
	public void addPlayer(Player player) {
		players.add(player);
	}
	public void removePlayer(Player player) {
		players.remove(player);
	}
	public void addPlayers(Collection<Player> col) {
		players.addAll(col);
	}
	public void removeAll() {
		players.clear();
	}
 	public boolean isLoop() {
		return loop;
	}
	public void setLoop(boolean loop) {
		this.loop = loop;
	}
	public List<Pair<List<Pair<Sound, Float>>, Integer>> getNotes() {
		return notes;
	}
	public void setNotes(List<Pair<List<Pair<Sound, Float>>, Integer>> notes) {
		this.notes = notes;
	}
	public int getNote() {
		return note;
	}
	public void setNote(int note) {
		this.note = note;
	}
	public float getVolume() {
		return volume;
	}
	public void setVolume(float volume) {
		this.volume = volume;
	}
}
