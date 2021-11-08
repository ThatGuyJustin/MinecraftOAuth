package bot.aperture.utils;

import bot.aperture.SpigotMain;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class CustomConfigSpigot {
    private File file;
    private FileConfiguration config;
    private String fileName;

    public CustomConfigSpigot(SpigotMain plugin, String fileName) {

        this.fileName = fileName;

        file = new File(plugin.getDataFolder(), fileName + ".yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(fileName + ".yml", false);
        }

        try {
            config = new YamlConfiguration();
            config.load(file);
        } catch (Exception e) {
            LoggerSpigot.error("&4Could not load &c" + fileName + ".yml&4. Reason: &c" + e.getLocalizedMessage(), true);
        }
    }

    public void save() {
        try {
            config.save(file);
            reload();
        } catch(Exception e) {
            LoggerSpigot.error("&4Could not save &c" + this.fileName + ".yml&4. Reason: &c" + e.getLocalizedMessage(), true);
        }
    }

    public void reload() {
        try {
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            LoggerSpigot.error("&4Could not reload &c" + this.fileName + ".yml&4. Reason: &c" + e.getLocalizedMessage(), true);
        }
    }

    public FileConfiguration get() { return config; }
    public FileConfiguration getConfig() { return config; }
}
