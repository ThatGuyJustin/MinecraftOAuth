package bot.aperture.listeners;

import bot.aperture.BungeeMain;
import bot.aperture.utils.LoggerBungee;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class BungeeListener implements Listener {

    private BungeeMain pl;

    public BungeeListener(BungeeMain pl) { this.pl = pl; }

    @EventHandler
    public void onPlayerConnect(LoginEvent e){
        UUID puid = e.getConnection().getUniqueId();

        if(pl.getAuthConfig().getConfig().getSection("users").getKeys().contains(puid.toString())){
            // Check for guilds here if enabled
        }else{
            String code = BungeeMain.generateCode();
            this.pl.getWebapp().getCodes().put(code, puid);

            LoggerBungee.debug(code);

            e.setCancelReason("Not Authorized, please go to " + this.pl.getConfig().getString("web.host") + " to be whitelisted.");
            e.setCancelled(true);
        }
    }

}
