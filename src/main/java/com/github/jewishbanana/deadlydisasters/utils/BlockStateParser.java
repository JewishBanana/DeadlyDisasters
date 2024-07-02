package com.github.jewishbanana.deadlydisasters.utils;

import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;

public class BlockStateParser {
	
	public static String serialize(BlockState state) {
		String parse = "";
		if (state instanceof Banner) {
			Banner banner = (Banner) state;
			parse += "BANNER?"+banner.getBaseColor().toString()+'?';
			StringBuilder builder = new StringBuilder();
			for (Pattern pat : banner.getPatterns())
				builder.append(pat.getColor().toString()+'%'+pat.getPattern().toString()+'/');
			parse += builder.toString();
			return parse;
		}
		return null;
	}
	public static void deserializeToBlock(String string, Block block) {
		
	}
}
