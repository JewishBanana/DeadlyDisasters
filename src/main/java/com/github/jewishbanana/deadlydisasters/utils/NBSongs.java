package com.github.jewishbanana.deadlydisasters.utils;

import com.github.jewishbanana.deadlydisasters.Main;

public class NBSongs {
	
	public static com.xxmicloxx.NoteBlockAPI.model.Song HALLOWEEN_BOSS;
	
	public static void init(Main plugin) {
		HALLOWEEN_BOSS = com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder.parse(plugin.getResource("files/songs/HalloweenTheme.nbs"));
	}
}
