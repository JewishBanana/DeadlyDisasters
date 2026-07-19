package com.github.jewishbanana.deadlydisasters.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Preconditions;

public class ConfigUpdater {

    //Used for separating keys in the keyBuilder inside parseComments method
    private static final char SEPARATOR = '.';

    public static void update(Plugin plugin, String resourceName, File toUpdate, List<String> ignoredSections) throws IOException {
        Preconditions.checkArgument(toUpdate.exists(), "The toUpdate file doesn't exist!");

        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource(resourceName), StandardCharsets.UTF_8));
        normalizeSectionTypeChanges(defaultConfig, toUpdate);
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(FileUtils.openInputStream(toUpdate), StandardCharsets.UTF_8));
        Map<String, String> comments = parseComments(plugin, resourceName, defaultConfig);
        Map<String, String> ignoredSectionsValues = parseIgnoredSections(toUpdate, comments, ignoredSections == null ? Collections.emptyList() : ignoredSections);
        Map<String, Object> preservedKeyValues = new LinkedHashMap<>();
        for (String s : currentConfig.getKeys(true))
        	if (!defaultConfig.contains(s))
        		preservedKeyValues.put(s, currentConfig.get(s));
        
        // will write updated config file "contents" to a string
        StringWriter writer = new StringWriter();
        write(defaultConfig, currentConfig, new BufferedWriter(writer), comments, ignoredSectionsValues);
        String value = writer.toString(); // config contents

        Path toUpdatePath = toUpdate.toPath();
        if (!value.equals(new String(Files.readAllBytes(toUpdatePath), StandardCharsets.UTF_8))) { // if updated contents are not the same as current file contents, update
            Files.write(toUpdatePath, value.getBytes(StandardCharsets.UTF_8));
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(toUpdate);
//        FileConfiguration yaml = plugin.getConfig();
        for (Entry<String, Object> entry : preservedKeyValues.entrySet())
        	yaml.set(entry.getKey(), entry.getValue());
        yaml.save(toUpdate);
    }

    private static void write(FileConfiguration defaultConfig, FileConfiguration currentConfig, BufferedWriter writer, Map<String, String> comments, Map<String, String> ignoredSectionsValues) throws IOException {
        //Used for converting objects to yaml, then cleared
        FileConfiguration parserConfig = new YamlConfiguration();

       for (String fullKey : defaultConfig.getKeys(true)) {
            String indents = ConfigUpdater.getIndents(fullKey, SEPARATOR);

           if (!ignoredSectionsValues.isEmpty()) {
               if (writeIgnoredSectionValueIfExists(ignoredSectionsValues, writer, fullKey))
                   continue;
           }
           writeCommentIfExists(comments, writer, fullKey, indents);
           Object defaultValue = defaultConfig.get(fullKey);
           Object currentValue = currentConfig.get(fullKey);

           if (currentValue == null)
               currentValue = defaultValue;
           else if ((defaultValue instanceof ConfigurationSection) != (currentValue instanceof ConfigurationSection))
               currentValue = defaultValue;

           String[] splitFullKey = fullKey.split("[" + SEPARATOR + "]");
           String trailingKey = splitFullKey[splitFullKey.length - 1];

           if (currentValue instanceof ConfigurationSection) {
               writeConfigurationSection(writer, indents, trailingKey, (ConfigurationSection) currentValue);
               continue;
           }
           writeYamlValue(parserConfig, writer, indents, trailingKey, currentValue);
       }

        String danglingComments = comments.get(null);

        if (danglingComments != null)
            writer.write(danglingComments);
        
        writer.close();
    }

    /**
     * Repairs a scalar value at a path that is now a section in the bundled defaults. Older updater versions could leave
     * the scalar in place and append the new child keys beneath it, producing invalid YAML before the next startup.
     */
    private static void normalizeSectionTypeChanges(FileConfiguration defaultConfig, File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<PathEntry> path = new ArrayList<>();
        boolean changed = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-"))
                continue;

            int colon = trimmed.indexOf(':');
            if (colon <= 0)
                continue;
            int indent = line.length() - line.stripLeading().length();
            while (!path.isEmpty() && path.get(path.size() - 1).indent >= indent)
                path.remove(path.size() - 1);

            String key = trimmed.substring(0, colon).trim();
            StringBuilder fullPath = new StringBuilder();
            for (PathEntry entry : path)
                fullPath.append(entry.key).append(SEPARATOR);
            fullPath.append(key);

            String value = trimmed.substring(colon + 1).trim();
            if (!value.isEmpty() && defaultConfig.isConfigurationSection(fullPath.toString())) {
                lines.set(i, line.substring(0, line.indexOf(':') + 1));
                changed = true;
            }
            path.add(new PathEntry(indent, key));
        }

        if (changed)
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    private static final class PathEntry {
        private final int indent;
        private final String key;

        private PathEntry(int indent, String key) {
            this.indent = indent;
            this.key = key;
        }
    }

    //Returns a map of key comment pairs. If a key doesn't have any comments it won't be included in the map.
    private static Map<String, String> parseComments(Plugin plugin, String resourceName, FileConfiguration defaultConfig) throws IOException {
        //keys are in order
        List<String> keys = new ArrayList<>(defaultConfig.getKeys(true));
        BufferedReader reader = new BufferedReader(new InputStreamReader(plugin.getResource(resourceName)));
        Map<String, String> comments = new LinkedHashMap<>();
        StringBuilder commentBuilder = new StringBuilder();
        KeyBuilder keyBuilder = new ConfigUpdater().new KeyBuilder(defaultConfig, SEPARATOR);
        String currentValidKey = null;

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            //Only getting comments for keys. A list/array element comment(s) not supported
            if (trimmedLine.startsWith("-")) continue;

            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {//Is blank line or is comment
                commentBuilder.append(trimmedLine).append("\n");
            } else {//is a valid yaml key
                //This part verifies if it is the first non-nested key in the YAML file and then stores the result as the next non-nested value.
                if (!line.startsWith(" ")) {
                    keyBuilder.clear();//add clear method instead of create new instance.
                    currentValidKey = trimmedLine;
                }

                keyBuilder.parseLine(trimmedLine, true);
                String key = keyBuilder.toString();

                //If there is a comment associated with the key it is added to comments map and the commentBuilder is reset
                if (commentBuilder.length() > 0) {
                    comments.put(key, commentBuilder.toString());
                    commentBuilder.setLength(0);
                }

                int nextKeyIndex = keys.indexOf(keyBuilder.toString()) + 1;
                if (nextKeyIndex < keys.size()) {

                    String nextKey = keys.get(nextKeyIndex);
                    while (!keyBuilder.isEmpty() && !nextKey.startsWith(keyBuilder.toString())) {
                        keyBuilder.removeLastKey();
                    }
                    //If all keys are cleared in a loop, then the first key from the nested keys in the YAML file is assigned to this keyBuilder instance.
                    //If the file contains multiple non-nested keys, the next first non-nested key will be used.
                    if (keyBuilder.isEmpty()) {
                        keyBuilder.parseLine(currentValidKey, false);
                    }
                }
            }
        }
        reader.close();

        if (commentBuilder.length() > 0)
            comments.put(null, commentBuilder.toString());

        return comments;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
	private static Map<String, String> parseIgnoredSections(File toUpdate, Map<String, String> comments, List<String> ignoredSections) throws IOException {
        Map<String, String> ignoredSectionValues = new LinkedHashMap<>(ignoredSections.size());

        DumperOptions options = new DumperOptions();
        options.setLineBreak(DumperOptions.LineBreak.UNIX);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new YamlConstructor(), new YamlRepresenter(), options);

        Map<Object, Object> root = (Map<Object, Object>) yaml.load(new FileReader(toUpdate));
        ignoredSections.forEach(section -> {
            String[] split = section.split("[" + SEPARATOR + "]");
            String key = split[split.length - 1];
            Map<Object, Object> map = getSection(section, root);
//            if (map == null)
//            	return;
            if (map == null) {
                ignoredSectionValues.put(section, "");
            	return;
            }
            Object mapKey = getKeyAsObject(key, map);
            if (!map.containsKey(mapKey)) {
                ignoredSectionValues.put(section, "");
                return;
            }

            StringBuilder keyBuilder = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                if (i != split.length - 1) {
                    if (keyBuilder.length() > 0)
                        keyBuilder.append(SEPARATOR);

                    keyBuilder.append(split[i]);
                }
            }

            ignoredSectionValues.put(section, buildIgnored(key, map, comments, keyBuilder, new StringBuilder(), yaml));
        });

        return ignoredSectionValues;
    }

    @SuppressWarnings("unchecked")
	private static Map<Object, Object> getSection(String fullKey, Map<Object, Object> root) {
        String[] keys = fullKey.split("[" + SEPARATOR + "]", 2);
        String key = keys[0];
        Object value = root.get(getKeyAsObject(key, root));

        if (keys.length == 1) {
            if (value instanceof Map || value != null)
                return root;
	   /*     if (value == null) {
                Map<Object, Object>  map= new HashMap<>();
                map.put(key,"{}");
                System.out.println("key " + key);
                return  map;
            }*/
//            throw new IllegalArgumentException("Invalid ignored section: " + fullKey);
            return null;
        }
        if (!(value instanceof Map))
        	return null;
//            throw new IllegalArgumentException("Invalid ignored ConfigurationSection specified!");

        return getSection(keys[1], (Map<Object, Object>) value);
    }

    @SuppressWarnings("unchecked")
	private static String buildIgnored(String fullKey, Map<Object, Object> ymlMap, Map<String, String> comments, StringBuilder keyBuilder, StringBuilder ignoredBuilder, Yaml yaml) {
        //0 will be the next key, 1 will be the remaining keys
        String[] keys = fullKey.split("[" + SEPARATOR + "]", 2);
        String key = keys[0];
        Object originalKey = getKeyAsObject(key, ymlMap);

        if (keyBuilder.length() > 0)
            keyBuilder.append(".");

        keyBuilder.append(key);

        if (!ymlMap.containsKey(originalKey)) {
            if (keys.length == 1)
                throw new IllegalArgumentException("Invalid ignored section: " + keyBuilder);

            throw new IllegalArgumentException("Invalid ignored section: " + keyBuilder + "." + keys[1]);
        }

        String comment = comments.get(keyBuilder.toString());
        String indents = ConfigUpdater.getIndents(keyBuilder.toString(), SEPARATOR);

        if (comment != null)
            ignoredBuilder.append(addIndentation(comment, indents)).append("\n");

        ignoredBuilder.append(addIndentation(key, indents)).append(":");
        Object obj = ymlMap.get(originalKey);

        if (obj instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) obj;

            if (map.isEmpty()) {
                ignoredBuilder.append(" {}\n");
            } else {
                ignoredBuilder.append("\n");
            }

            StringBuilder preLoopKey = new StringBuilder(keyBuilder);

            for (Object o : map.keySet()) {
                buildIgnored(o.toString(), map, comments, keyBuilder, ignoredBuilder, yaml);
                keyBuilder = new StringBuilder(preLoopKey);
            }
        } else {
            writeIgnoredValue(yaml, obj, ignoredBuilder, indents);
        }

        return ignoredBuilder.toString();
    }

    private static void writeIgnoredValue(Yaml yaml, Object toWrite, StringBuilder ignoredBuilder, String indents) {
        String yml = yaml.dump(toWrite);
        if (toWrite instanceof Collection) {
            ignoredBuilder.append("\n").append(addIndentation(yml, indents)).append("\n");
        } else {
            ignoredBuilder.append(" ").append(yml);
        }
    }

    private static String addIndentation(String s, String indents) {
        StringBuilder builder = new StringBuilder();
        String[] split = s.split("\n");

        for (String value : split) {
            if (builder.length() > 0)
                builder.append("\n");

            builder.append(indents).append(value);
        }

        return builder.toString();
    }

    private static void writeCommentIfExists(Map<String, String> comments, BufferedWriter writer, String fullKey, String indents) throws IOException {
        String comment = comments.get(fullKey);

        //Comments always end with new line (\n)
        if (comment != null)
            //Replaces all '\n' with '\n' + indents except for the last one
            writer.write(indents + comment.substring(0, comment.length() - 1).replace("\n", "\n" + indents) + "\n");
    }

    //Will try to get the correct key by using the sectionContext
    private static Object getKeyAsObject(String key, Map<Object, Object> sectionContext) {
        if (sectionContext.containsKey(key))
            return key;

        try {
            Float keyFloat = Float.parseFloat(key);

            if (sectionContext.containsKey(keyFloat))
                return keyFloat;
        } catch (NumberFormatException ignored) {}

        try {
            Double keyDouble = Double.parseDouble(key);

            if (sectionContext.containsKey(keyDouble))
                return keyDouble;
        } catch (NumberFormatException ignored) {}

        try {
            Integer keyInteger = Integer.parseInt(key);

            if (sectionContext.containsKey(keyInteger))
                return keyInteger;
        } catch (NumberFormatException ignored) {}

        try {
            Long longKey = Long.parseLong(key);

            if (sectionContext.containsKey(longKey))
                return longKey;
        } catch (NumberFormatException ignored) {}

        return null;
    }

	/**
	 * Writes the current value with the provided trailing key to the provided writer.
	 *
	 * @param parserConfig   The parser configuration to use for writing the YAML value.
	 * @param bufferedWriter The writer to write the value to.
	 * @param indents        The string representation of the indentation.
	 * @param trailingKey    The trailing key for the YAML value.
	 * @param currentValue   The current value to write as YAML.
	 * @throws IOException If an I/O error occurs while writing the YAML value.
	 */
	private static void writeYamlValue(final FileConfiguration parserConfig, final BufferedWriter bufferedWriter, final String indents, final String trailingKey, final Object currentValue) throws IOException {
		parserConfig.set(trailingKey, currentValue);
		String yaml = parserConfig.saveToString();
		yaml = yaml.substring(0, yaml.length() - 1).replace("\n", "\n" + indents);
		final String toWrite = indents + yaml + "\n";
		parserConfig.set(trailingKey, null);
		bufferedWriter.write(toWrite);
	}

    /**
     * Writes the value associated with the ignored section to the provided writer,
     * if it exists in the ignoredSectionsValues map.
     *
     * @param ignoredSectionsValues The map containing the ignored section-value mappings.
     * @param bufferedWriter        The writer to write the value to.
     * @param fullKey               The full key to search for in the ignoredSectionsValues map.
     * @throws IOException If an I/O error occurs while writing the value.
     */
    private static boolean writeIgnoredSectionValueIfExists(final Map<String, String> ignoredSectionsValues, final BufferedWriter bufferedWriter, final String fullKey) throws IOException {
        String ignored = ignoredSectionsValues.get(fullKey);
        if (ignored != null) {
            bufferedWriter.write(ignored);
            return true;
        }
        for (final Map.Entry<String, String> entry : ignoredSectionsValues.entrySet()) {
            if (ConfigUpdater.isSubKeyOf(entry.getKey(), fullKey, SEPARATOR)) {
                return true;
            }
        }
        return false;
    }

	/**
	 * Writes a configuration section with the provided trailing key and the current value to the provided writer.
	 *
	 * @param bufferedWriter The writer to write the configuration section to.
	 * @param indents        The string representation of the indentation level.
	 * @param trailingKey    The trailing key for the configuration section.
	 * @param configurationSection   The current value of the configuration section.
	 * @throws IOException If an I/O error occurs while writing the configuration section.
	 */
	private static void writeConfigurationSection(final BufferedWriter bufferedWriter, final String indents, final String trailingKey, final ConfigurationSection configurationSection) throws IOException {
		bufferedWriter.write(indents + trailingKey + ":");
		if (!(configurationSection).getKeys(false).isEmpty()) {
			bufferedWriter.write("\n");
		} else {
			bufferedWriter.write(" {}\n");
		}
	}
	public static boolean isSubKeyOf(final String parentKey, final String subKey, final char separator) {
		if (parentKey.isEmpty())
			return false;

		return subKey.startsWith(parentKey)
				&& subKey.substring(parentKey.length()).startsWith(String.valueOf(separator));
	}

	public static String getIndents(final String key, final char separator) {
		final String[] splitKey = key.split("[" + separator + "]");
		final StringBuilder builder = new StringBuilder();

		for (int i = 1; i < splitKey.length; i++) {
			builder.append("  ");
		}
		return builder.toString();
	}
	public class KeyBuilder implements Cloneable {

	    private final FileConfiguration config;
	    private final char separator;
	    private final StringBuilder builder;

	    public KeyBuilder(FileConfiguration config, char separator) {
	        this.config = config;
	        this.separator = separator;
	        this.builder = new StringBuilder();
	    }

	    private KeyBuilder(KeyBuilder keyBuilder) {
	        this.config = keyBuilder.config;
	        this.separator = keyBuilder.separator;
	        this.builder = new StringBuilder(keyBuilder.toString());
	    }

	    public void parseLine(String line, boolean checkIfExists) {
	        line = line.trim();

	        String[] currentSplitLine = line.split(":");

	        if (currentSplitLine.length > 2)
	            currentSplitLine = line.split(": ");

	        String key = currentSplitLine[0].replace("'", "").replace("\"", "");

	        if (checkIfExists) {
	            //Checks keyBuilder path against config to see if the path is valid.
	            //If the path doesn't exist in the config it keeps removing last key in keyBuilder.
	            while (builder.length() > 0 && !config.contains(builder.toString() + separator + key)) {
	                removeLastKey();
	            }
	        }

	        //Add the separator if there is already a key inside keyBuilder
	        //If currentSplitLine[0] is 'key2' and keyBuilder contains 'key1' the result will be 'key1.' if '.' is the separator
	        if (builder.length() > 0)
	            builder.append(separator);

	        //Appends the current key to keyBuilder
	        //If keyBuilder is 'key1.' and currentSplitLine[0] is 'key2' the resulting keyBuilder will be 'key1.key2' if separator is '.'
	        builder.append(key);
	    }

	    public String getLastKey() {
	        if (builder.length() == 0)
	            return "";

	        return builder.toString().split("[" + separator + "]")[0];
	    }

	    public boolean isEmpty() {
	        return builder.length() == 0;
	    }
	    public void clear() {
	        builder.setLength(0);
	    }
	    //Checks to see if the full key path represented by this instance is a sub-key of the key parameter
	    public boolean isSubKeyOf(String parentKey) {
	        return ConfigUpdater.isSubKeyOf(parentKey, builder.toString(), separator);
	    }

	    //Checks to see if subKey is a sub-key of the key path this instance represents
	    public boolean isSubKey(String subKey) {
	        return ConfigUpdater.isSubKeyOf(builder.toString(), subKey, separator);
	    }

	    public boolean isConfigSection() {
	        String key = builder.toString();
	        return config.isConfigurationSection(key);
	    }

	    public boolean isConfigSectionWithKeys() {
	        String key = builder.toString();
	        return config.isConfigurationSection(key) && !config.getConfigurationSection(key).getKeys(false).isEmpty();
	    }

	    //Input: 'key1.key2' Result: 'key1'
	    public void removeLastKey() {
	        if (builder.length() == 0)
	            return;

	        String keyString = builder.toString();
	        //Must be enclosed in brackets in case a regex special character is the separator
	        String[] split = keyString.split("[" + separator + "]");
	        //Makes sure begin index isn't < 0 (error). Occurs when there is only one key in the path
	        int minIndex = Math.max(0, builder.length() - split[split.length - 1].length() - 1);
	        builder.replace(minIndex, builder.length(), "");
	    }

	    @Override
	    public String toString() {
	        return builder.toString();
	    }

	    @Override
	    protected KeyBuilder clone() {
	        return new KeyBuilder(this);
	    }
	}
}
