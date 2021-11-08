package bot.aperture.listeners;

import bot.aperture.SpigotMain;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class SpigotListener implements Listener {

    private SpigotMain pl;

    public SpigotListener(SpigotMain pl){
        this.pl = pl;
    }

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent e){
        UUID puid = e.getUniqueId();
    }

}
