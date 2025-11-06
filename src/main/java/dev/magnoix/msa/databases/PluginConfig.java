package dev.magnoix.msa.databases;

import dev.magnoix.msa.messages.Msg;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PluginConfig {
    private final File file;
    private FileConfiguration config;

    /**
     * Loads a YML config. If the plugin does not have a config file, the default be copied from the resources.
     * @param dataFolder The of the plugin's data folder.
     * @param fileName   The name of the config file. (i.e., config.yml)
     */
    public PluginConfig(File dataFolder, String fileName) {
        this.file = new File(dataFolder, fileName);

        // Copy default from resources if missing
        if (!file.exists()) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (in == null) {
                    throw new IllegalArgumentException("Default config " + fileName + " not found in resources!");
                }
                file.getParentFile().mkdirs();
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                Msg.log("Failed to copy default config file: " + e.getMessage());
            }
        }

        // Load config
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /* - Lists - */
    public List<String> getStringList(String sectionTitle) {
        return config.getStringList(sectionTitle);
    }
    public void setValues(String sectionTitle, List<String> values) {
        config.set(sectionTitle, values);
        save();
    }
    public void addValue(String sectionTitle, String value) {
        List<String> values = getStringList(sectionTitle);
        if (!values.contains(value)) {
            values.add(value);
            setValues(sectionTitle, values);
        }
    }
    public void removeValue(String sectionTitle, String value) {
        List<String> values = getStringList(sectionTitle);
        if (values.contains(value)) {
            values.remove(value);
        }
    }

    /* - Trees - */
    public Map<String, Object> getTree(String sectionTitle) {
        ConfigurationSection section = config.getConfigurationSection(sectionTitle);
        if (section == null) return Collections.emptyMap();
        return section.getValues(false);
    }
    public void setTree(String sectionTitle, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            config.set(sectionTitle + "." + entry.getKey(), entry.getValue());
        }
        save();
    }
    public void setTreeValue(String path, Object value) {
        config.set(path, value);
        save();
    }
    public Object getTreeValue(String path) {
        return config.get(path);
    }

    /* - Save / Reload - */
    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            Msg.log("Failed to save config file: " + e.getMessage());
        }
    }
    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
    public FileConfiguration getConfig() {
        return config;
    }
}
