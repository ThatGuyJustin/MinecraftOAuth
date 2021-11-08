package bot.aperture.listeners;

import bot.aperture.BungeeMain;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class BungeeListener implements Listener {

    private BungeeMain pl;

    public BungeeListener(BungeeMain pl) { this.pl = pl; }

    @EventHandler
    public void onPlayerConnect(PreLoginEvent e){
        UUID puid = e.getConnection().getUniqueId();
    }

}
