package deadlydisasters.general;

import deadlydisasters.utils.Utils;

public enum DifficultyLevel {
	EASY(Utils.chat("&a&lEASY"), 20, 180, new int[]{40, 30, 20, 10, 0, 0}),
	NORMAL(Utils.chat("&e&lNORMAL"), 10, 120, new int[]{30, 25, 20, 15, 9, 1}),
	HARD(Utils.chat("&c&lHARD"), 5, 90, new int[]{25, 23, 20, 18, 14, 5}),
	EXTREME(Utils.chat("&4&lEXTREME"), 0, 30, new int[]{0, 0, 29, 26, 25, 20}),
	CUSTOM(Utils.chat("&f&lCUSTOM"));
	
	private String label;
	
	private int[] table;
	
	private int offset;
	private int timer;
	
	private static final DifficultyLevel[] copyOfValues = values();
	 
	private DifficultyLevel(String label, int offset, int timer, int[] table) {
        this.label = label;
        this.offset = offset;
        this.timer = timer;
        this.table = table;
    }
	DifficultyLevel(String label) {
        this.label = label;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
    	this.label = label;
    }
    public int getOffset() {
    	return offset;
    }
    public int getTimer() {
    	return timer;
    }
    public int[] getTable() {
    	return table;
    }
    public static DifficultyLevel forName(String name) {
		for (DifficultyLevel value : copyOfValues)
			if (value.name().equals(name))
				return value;
		return null;
	}
    public static void reloadNames() {
    	EASY.setLabel(Utils.chat("&a&l"+Languages.langFile.getString("internal.easy")));
    	NORMAL.setLabel(Utils.chat("&e&l"+Languages.langFile.getString("internal.normal")));
    	HARD.setLabel(Utils.chat("&c&l"+Languages.langFile.getString("internal.hard")));
    	EXTREME.setLabel(Utils.chat("&4&l"+Languages.langFile.getString("internal.extreme")));
    	CUSTOM.setLabel(Utils.chat("&f&l"+Languages.langFile.getString("internal.custom")));
    }
}
