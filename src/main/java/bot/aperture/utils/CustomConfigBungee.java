package bot.aperture.utils;

import bot.aperture.BungeeMain;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;

public class CustomConfigBungee {
    private File file;
    private Configuration config;
    private String fileName;

    public CustomConfigBungee(BungeeMain plugin, String fileName) {

        this.fileName = fileName;

        file = new File(plugin.getDataFolder(), fileName + ".yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(plugin, fileName + ".yml", fileName + ".yml", false);
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (Exception e) {
            LoggerBungee.error("&4Could not load &c" + fileName + ".yml&4. Reason: &c" + e.getLocalizedMessage(), true);
        }
    }

    public void save() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
            reload();
        } catch(Exception e) {
            LoggerBungee.error("&4Could not save &c" + this.fileName + ".yml&4. Reason: &c" + e.getLocalizedMessage(), true);
        }
    }

    public void reload() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (Exception e) {
            LoggerBungee.error("&4Could not reload &c" + this.fileName + ".yml&4. Reason: &c" + e.getLocalizedMessage(), true);
        }
    }

    public Configuration get() { return config; }
    public Configuration getConfig() { return config; }
}
