package deadlydisasters.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Sound;

import com.mojang.datafixers.util.Pair;

public class Song {
	
	/* list (
			list (
				list ( pair (note, pitch) )
			, ticksToNext )
		)
	*/

	@SuppressWarnings("unchecked")
	public static Song CHRISTMAS_THEME = new Song(Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(0)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(12))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(17))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(12))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(17))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(17))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(18))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(17))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(12))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(12))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(12))), christmasTickRate(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(12))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), christmasTickRate(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(17))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(20))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), christmasTickRate(2)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(12))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), christmasTickRate(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), christmasTickRate(40)));
	
	@SuppressWarnings("unchecked")
	public static Song EASTER_THEME = new Song(Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(9))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(11))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(11))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(15))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(5))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_BELL, gfh(10))), t(4)),
			Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(15))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(9))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(15))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(18))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(17))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(15))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(15))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(15))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(18))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(17))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(15))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5)), Pair.of(Sound.BLOCK_NOTE_BLOCK_BIT, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), t(4)),
			Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(9))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(15))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(13))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(11))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(1))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(5))), t(8)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(6))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(8))), t(4)), Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(10))), t(4)),
			Pair.of(List.of(Pair.of(Sound.BLOCK_NOTE_BLOCK_HARP, gfh(3))), t(8))
			);
	
	public List<Pair<List<Pair<Sound, Float>>,Integer>> notes = new ArrayList<>();
	
	public Song(@SuppressWarnings("unchecked") Pair<List<Pair<Sound, Float>>,Integer>... list) {
		for (Pair<List<Pair<Sound, Float>>,Integer> i : list)
			notes.add(i);
	}
	public static float gfh(int hits) {
		switch (hits) {
		default:
		case 0:
			return 0.5f;
		case 1:
			return 0.529732f;
		case 2:
			return 0.561231f;
		case 3:
			return 0.594604f;
		case 4:
			return 0.629961f;
		case 5:
			return 0.667420f;
		case 6:
			return 0.707107f;
		case 7:
			return 0.749154f;
		case 8:
			return 0.793701f;
		case 9:
			return 0.840896f;
		case 10:
			return 0.890899f;
		case 11:
			return 0.943874f;
		case 12:
			return 1.0f;
		case 13:
			return 1.059463f;
		case 14:
			return 1.122462f;
		case 15:
			return 1.189207f;
		case 16:
			return 1.259921f;
		case 17:
			return 1.334840f;
		case 18:
			return 1.414214f;
		case 19:
			return 1.498307f;
		case 20:
			return 1.587401f;
		case 21:
			return 1.681793f;
		case 22:
			return 1.781797f;
		case 23:
			return 1.887749f;
		case 24:
			return 2.0f;
			
		}
	}
	public static int christmasTickRate(int ticks) {
		return ticks;
	}
	public static int t(int ticks) {
		return (int) ((20.0 / 27.0) * ticks);
	}
}
