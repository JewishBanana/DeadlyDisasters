package com.github.jewishbanana.deadlydisasters.utils;

import java.util.UUID;

import org.bukkit.entity.Player;

public class SongPlayer {
	
	private com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer songPlayer;
	
	public SongPlayer(com.xxmicloxx.NoteBlockAPI.model.Song song) {
		songPlayer = new com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer(song);
	}
	public void addPlayer(Player player) {
		songPlayer.addPlayer(player.getUniqueId());
	}
	public void removePlayer(Player player) {
		songPlayer.removePlayer(player.getUniqueId());
	}
	public void clearPlayers() {
		for (UUID uuid : songPlayer.getPlayerUUIDs())
			songPlayer.removePlayer(uuid);
	}
	public void setPlaying(boolean value) {
		songPlayer.setPlaying(value);
	}
	@SuppressWarnings("deprecation")
	public void setLooping(boolean value) {
		songPlayer.setLoop(value);
	}
	public void setVolume(byte volume) {
		songPlayer.setVolume(volume);
	}
}
