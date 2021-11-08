package bot.aperture;

import bot.aperture.utils.LoggerSpigot;
import org.bukkit.plugin.java.JavaPlugin;
import bot.aperture.oauth.WebApp;

public class SpigotMain extends JavaPlugin {
    private static WebApp webapp;

    @Override
    public void onEnable(){

        if (true) {
            webapp = new WebApp(this);
            try {
                webapp.start();
            } catch (Exception e) {
                LoggerSpigot.error("[Webserver] " + "Cannot start: " + e.getMessage(), true);
            }
        } else {
            LoggerSpigot.log("[Webserver] Not Starting due to config value", true);
        }

        LoggerSpigot.info("Plugin Enabled!", true);
    }

    @Override
    public void onDisable(){
        if (webapp != null) {
            webapp.stop();
        }

        LoggerSpigot.info("Plugin Disabled!", true);
    }
}
