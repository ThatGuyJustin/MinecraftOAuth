package bot.aperture;

import bot.aperture.listeners.BungeeListener;
import bot.aperture.oauth.WebApp;
import bot.aperture.utils.LoggerSpigot;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;


public class BungeeMain extends Plugin{

    private static WebApp webapp;
    private Configuration config;

    @Override
    public void onEnable(){

        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.config.getBoolean("web.enabled")) {
            webapp = new WebApp(this);
            try {
                webapp.start();
            } catch (Exception e) {
                LoggerSpigot.error("[Webserver] " + "Cannot start: " + e.getMessage(), true);
            }
        } else {
            LoggerSpigot.log("[Webserver] Not Starting due to config value", true);
        }

        ProxyServer.getInstance().getPluginManager().registerListener(this, new BungeeListener(this));
//        ProxyServer.getInstance().getPluginManager().registerCommand(this, new MessageCommand(this, "msg", "cavern.msg", "message", "m", "whisper", "tell"));

    }

    @Override
    public void onDisable(){

    }

    public void reloadConfig(){
        try {
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Configuration getConfig() {
        return config;
    }

    public static String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder(input.length());
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

    public File saveResource(
            Plugin plugin, String resourceName, String destinationName, boolean replaceIfDestExists)
    {
        File folder = plugin.getDataFolder();

        if (!folder.exists() && !folder.mkdir())
        {
            return null;
        }

        File destinationFile = new File(folder, destinationName);

        try
        {
            if (!destinationFile.exists() || replaceIfDestExists)
            {
                if (destinationFile.createNewFile())
                {
                    try (InputStream in = plugin.getResourceAsStream(resourceName);
                         OutputStream out = new FileOutputStream(destinationFile))
                    {
                        ByteStreams.copy(in, out);
                    }
                }
                else
                {
                    return null;
                }
            }

            return destinationFile;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}