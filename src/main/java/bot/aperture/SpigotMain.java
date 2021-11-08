package bot.aperture;

import bot.aperture.utils.CustomConfigSpigot;
import bot.aperture.utils.LoggerSpigot;
import org.bukkit.plugin.java.JavaPlugin;
import bot.aperture.oauth.WebApp;

public class SpigotMain extends JavaPlugin {
    public static CustomConfigSpigot AuthConfig;
    private static WebApp webapp;

    @Override
    public void onEnable(){

        this.saveDefaultConfig();

        AuthConfig = new CustomConfigSpigot(this, "auth");

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

    public CustomConfigSpigot getAuthConfig(){
        return AuthConfig;
    }
}
