package bot.aperture.listeners;

import bot.aperture.BungeeMain;
import bot.aperture.utils.LoggerBungee;
import bot.aperture.utils.StringUtils;
import net.md_5.bungee.api.chat.TextComponent;
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
            String url = pl.getConfig().getString("discord.oauth_url");
            this.pl.getWebapp().getCodes().put(code, puid);

            LoggerBungee.debug(code);

            TextComponent start = new TextComponent(StringUtils.color("&cNot Authorized.\nPlease go to the following website:\n"));

            TextComponent web = new TextComponent(StringUtils.color("&6" + url + "\n"));

            TextComponent codeString = new TextComponent(StringUtils.color("&cEnter Code: &6" + code));

            start.addExtra(web);
            start.addExtra(codeString);

            e.setCancelReason(start);

            e.setCancelled(true);
        }
    }

}
